package moe.tyty.fileuploader;

import moe.tyty.fileuploader.Exception.FileOpenException;
import moe.tyty.fileuploader.Exception.NotOurMsgException;
import moe.tyty.fileuploader.File.Writer;
import moe.tyty.fileuploader.Protocol.Constructor;
import moe.tyty.fileuploader.Protocol.Reader;
import moe.tyty.fileuploader.Protocol.Session;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static com.ea.async.Async.await;
import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * class ServerSession handles all connections comes from one client.
 * It saves status of handshake and transfer to react client properly.
 *
 * @author TYTY
 */
public class ServerSession {
    static CompletableFuture<Boolean> False = completedFuture(false);

    public enum ServerSessionStatus {INIT, HANDSHAKE, READY, FINISH}

    Constructor builder;
    Reader reader;
    AsynchronousSocketChannel SSession;
    String key;
    public byte[] session;
    public ServerSessionStatus status;
    public Session.HelloStatus helloStatus;
    Writer file;
    Function<Void, Void> onFinish;
    AtomicInteger threadCount;

    /**
     * Static method to process server hello data and return ServerSession instance
     * @param SSession socket
     * @param key password
     * @param serverHelloData data received by server. provide for further verification
     * @return ServerSession object
     */
    public static ServerSession StartSession(AsynchronousSocketChannel SSession, String key, byte[] serverHelloData) {
        ServerSession newSession = new ServerSession();
        newSession.builder = new Constructor(key);
        newSession.reader = new Reader(key);
        newSession.SSession = SSession;
        newSession.key = key;

        newSession.helloStatus = newSession.reader.serverHello(serverHelloData);

        if (newSession.helloStatus != Session.HelloStatus.OK) {
            return null;
        }
        newSession.session = Session.session(32);
        newSession.status = ServerSessionStatus.INIT;
        newSession.threadCount = new AtomicInteger(0);

        return newSession;
    }

    /**
     * handshake part logic
     * verify client and confirm file info
     * @param finishFunction Function to call when session finished in any status
     * @return if session finished successfully or with wrong
     */
    public CompletableFuture<Boolean> doHandshake(Function<Void, Void> finishFunction) {

        onFinish = finishFunction;

        try {
            CompletableFuture<Boolean> clientHelloSendResult = builder.writeMsg(
                    SSession, builder.clientHello(helloStatus, session), Session.MessageType.SESSION);

            boolean clientHelloSend = await(clientHelloSendResult);

            switch (helloStatus) {
                case BAD_VERSION:
                    System.out.println("Client version conflict.");
                    SSession.close();
                    status = ServerSessionStatus.FINISH;
                    onFinish.apply(null);
                    return False;
                case BAD_PASSWORD:
                    System.out.println("Client using another key.");
                    status = ServerSessionStatus.FINISH;
                    onFinish.apply(null);
                    return False;
                case OK:
                    if (clientHelloSend) {
                        System.out.println("Handshake successful.");
                        status = ServerSessionStatus.HANDSHAKE;
                    }
                    else {
                        System.out.println("Failed sending client hello msg.");
                        status = ServerSessionStatus.FINISH;
                        onFinish.apply(null);
                        return False;
                    }
            }

            CompletableFuture<byte[]> fileNegotiationInfoFuture = reader.readMsg(SSession, Session.MessageType.SESSION);

            Session.FileNegotiationInfo fileNegotiationInfo = reader.fileNegotiation(session, await(fileNegotiationInfoFuture));

            try {
                file = new Writer(fileNegotiationInfo.filePath, fileNegotiationInfo.fileLength, fileNegotiationInfo.pieceSize);
            } catch (FileOpenException e) {
                if (e.getMessage().equals("No enough space.")) {
                    System.out.println(e.getMessage());
                } else {
                    e.printStackTrace();
                }
                CompletableFuture<Boolean> fileNegotiationResultFuture =
                        builder.writeMsg(SSession,
                                builder.fileNegotiationReply(session, Session.NegotiationStatus.OPEN_FAIL),
                                Session.MessageType.SESSION);
                if (await(fileNegotiationResultFuture)) {
                    System.out.println("Message sent. Shut down connection.");
                }
                else {
                    System.out.println("Failed sending file negotiation failed msg. Shut down connection.");
                }
                SSession.close();
                status = ServerSessionStatus.FINISH;
                onFinish.apply(null);
                return False;
            }

            System.out.printf("File opened for writing: %s\n", fileNegotiationInfo.filePath);

            CompletableFuture<Boolean> fileNegotiationResultFuture =
                    builder.writeMsg(SSession,
                            builder.fileNegotiationReply(session, Session.NegotiationStatus.OK),
                            Session.MessageType.SESSION);

            if (await(fileNegotiationResultFuture)) {
                System.out.println("File negotiation finished. Waiting for file transfer connection.");
                status = ServerSessionStatus.READY;
                return completedFuture(true);
            }
            else {
                System.out.println("Failed sending file negotiation msg. Shut down connection.");
                SSession.close();
                status = ServerSessionStatus.FINISH;
                onFinish.apply(null);
                return False;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return False;
    }

    /**
     * attach a thread socket to this channel
     * @param SThread socket
     * @return placeholder
     */
    public CompletableFuture<Void> attachThread(AsynchronousSocketChannel SThread) {
        int index = threadCount.getAndIncrement();
        try {
            System.out.printf("Thread#%d: Start transfer.\n", index);
            CompletableFuture<byte[]> fileTransferFuture = reader.readMsg(SThread, Session.MessageType.THREAD);
            Writer.WriteData data = reader.fileTransfer(session, await(fileTransferFuture));

            while (data.OK) {
                file.write(data);
                fileTransferFuture = reader.readMsg(SThread, Session.MessageType.THREAD);
                data = reader.fileTransfer(session, await(fileTransferFuture));
            }
        } catch (NotOurMsgException e) {
            // debug only
            e.printStackTrace();
            System.out.println(e.getMessage());
            threadCount.decrementAndGet();
        } catch (CompletionException e) {
            // debug only
            e.printStackTrace();
            System.out.printf("Thread#%d: Exception while receiving data.\n", index);
            threadCount.decrementAndGet();
        }
        System.out.printf("Thread#%d: Finish transfer.\n", index);
        if (threadCount.decrementAndGet() == 0) {
            System.out.println("Session finished. Saving file.");
            status = ServerSessionStatus.FINISH;
            file.finish = true;
            file.close();
            onFinish.apply(null);
        }
        return completedFuture(null);
    }
}
