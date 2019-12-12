package moe.tyty.fileuploader;

import moe.tyty.fileuploader.Exception.NotOurMsgException;
import moe.tyty.fileuploader.Protocol.Constructor;
import moe.tyty.fileuploader.Protocol.Reader;
import moe.tyty.fileuploader.Protocol.Session;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static com.ea.async.Async.await;
import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 *  class Server is an acceptor to receive new connections and build proper handler to deal with them.
 *  And itself implements CompletionHandler interface to allow callback-style asynchronous use it as handler.
 *
 * @author TYTY
 */
public class Server implements CompletionHandler<AsynchronousSocketChannel, Void> {

    Constructor builder;
    Reader reader;

    InetAddress host;
    SocketAddress listen;
    int port;
    String key;

    static CompletableFuture<Void> Return = completedFuture(null);

    AsynchronousServerSocketChannel SAcceptor;
    Map<SessionHelper, ServerSession> sessionMap;

    /**
     * Initialize socket listener
     * @param host ip to bind and listen to
     * @param port port to bind and listen to
     * @param key password to be used
     */
    public Server(InetAddress host, int port, String key) {
        try {
            SAcceptor = AsynchronousServerSocketChannel.open();
            SAcceptor.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            listen = new InetSocketAddress(host, port);
            SAcceptor.bind(listen);
            System.out.printf("Server listening on [%s]:%d\n", host.getHostAddress(), port);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Exception while opening socket.");
            System.exit(1);
        }

        builder = new Constructor(key);
        reader = new Reader(key);
        sessionMap = new HashMap<>();

        this.host = host;
        this.port = port;
        this.key = key;
    }

    /**
     * Helper class to solve native array can't be hashed
     */
    public static final class SessionHelper {
        private final byte[] t;

        public SessionHelper(byte[] t) {
            this.t = t;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 23 * hash + Arrays.hashCode(this.t);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            return Arrays.equals(this.t, ((SessionHelper) obj).t);
        }
    }

    /**
     * Determine connection and send to corresponding place
     * @param CSocket socket connected
     * @return void wrapped with CompletableFuture to use await
     */
    public CompletableFuture<Void> dispatch(AsynchronousSocketChannel CSocket) {
        CompletableFuture<Session.MsgGuess> helloFuture = reader.readMsgGuess(CSocket);
        Session.MsgGuess guessResult = await(helloFuture);

        switch (guessResult.type) {
            case SESSION:
                System.out.println("Acceptor: New session.");
                ServerSession SSession = ServerSession.StartSession(CSocket, key, await(guessResult.message));
                if (SSession == null) {
                    try {
                        CSocket.close();
                    } catch (IOException ignored) {}
                    return Return;
                }
                sessionMap.put(new SessionHelper(SSession.session), SSession);
                SSession.doHandshake((Void) -> {
                    sessionMap.remove(new SessionHelper(SSession.session));
                    return null;
                });
                break;
            case THREAD:
                System.out.println("Acceptor: New Thread.");
                byte[] session = reader.fileTransferInit(await(guessResult.message));
                if (sessionMap.containsKey(new SessionHelper(session))) {
                    System.out.println("Acceptor: Attach to session.");
                    sessionMap.get(new SessionHelper(session)).attachThread(CSocket);
                } else {
                    System.out.println("Acceptor: Orphan thread, close.");
                    try {
                        CSocket.close();
                    } catch (IOException ignored) {}
                }
                break;
            case UNKNOWN:
                System.out.println("Acceptor: Unknown connection, close.");
                try {
                    CSocket.close();
                } catch (IOException ignored) {}
        }
        return Return;
    }

    /**
     * callback function to be used when new connection comes in
     * @param CSocket Connection socket
     * @param attachment void. Placeholder
     */
    @Override
    public void completed(AsynchronousSocketChannel CSocket, Void attachment) {
        SAcceptor.accept(null, this);
        try {
            CSocket.setOption(StandardSocketOptions.TCP_NODELAY, true);
            dispatch(CSocket);
        } catch (NotOurMsgException e) {
            // debug only
            e.printStackTrace();
            System.out.println(e.getMessage());
        } catch (CompletionException e) {
            // debug only
            e.printStackTrace();
            System.out.println("Exception while sending data.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * callback function to be used when new connection comes in with exception
     * @param exc exception
     * @param attachment void. Placeholder
     */
    @Override
    public void failed(Throwable exc, Void attachment) {
        SAcceptor.accept(null, this);
        throw new RuntimeException(exc);
    }

    /**
     * start the infinite accept loop
     */
    public void bootstrap() {
        SAcceptor.accept(null, this);
        try {
            // block main thread to prevent it from exit
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
