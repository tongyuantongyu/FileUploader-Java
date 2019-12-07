package moe.tyty.fileuploader;

import moe.tyty.fileuploader.Exception.NotOurMsgException;
import moe.tyty.fileuploader.Protocol.Constructor;
import moe.tyty.fileuploader.Protocol.Reader;
import moe.tyty.fileuploader.Protocol.Session;

import static com.ea.async.Async.await;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import static java.util.concurrent.CompletableFuture.completedFuture;


public class Client {

    static CompletableFuture<Boolean> False = completedFuture(false);

    moe.tyty.fileuploader.File.Reader file;
    Constructor builder;
    Reader reader;
    SocketAddress server;
    InetAddress host;
    int port;
    String key;
    String file_;
    int thread;
    int size;
    byte[] session;

    public Client(InetAddress host, int port, String key, String file_, int thread, int size) {
        file = new moe.tyty.fileuploader.File.Reader(file_, size);
        builder = new Constructor(key);
        reader = new Reader(key);
        server = new InetSocketAddress(host, port);
        this.host = host;
        this.port = port;
        this.key = key;
        this.file_ = file_;
        this.size = size;
        this.thread = thread;
    }

    public CompletableFuture<Boolean> runThread(int index) {
        AsynchronousSocketChannel CThread;

        if (!file.working) {
            System.out.printf("Thread#%d: File has reached the end before we start.\n", index);
            return completedFuture(true);
        }

        try {
//            System.out.printf("Thread#%d: Connecting server...", index);
            CThread = AsynchronousSocketChannel.open();

            CompletableFuture<Void> connectFuture = new CompletableFuture<>();
            CThread.connect(server, null, Reader.regToFuture(connectFuture));
            await(connectFuture);
//            System.out.println("Connected.");

//            System.out.printf("Thread#%d: Sending init data...", index);
            CompletableFuture<Boolean> fileTransferInitFuture = builder.writeMsg(CThread, builder.fileTransferInit(session), Session.MessageType.THREAD);
            if (!await(fileTransferInitFuture)) {
                System.out.println("Failed in sending file transfer init data.");
                System.exit(1);
                return False;
            }
//            System.out.println("Sent.");
            System.out.printf("Thread#%d: Start sending data.\n", index);
            moe.tyty.fileuploader.File.Reader.ReadData data = file.read();
            CompletableFuture<Boolean> fileTransferFuture;

            while (data.ok) {
                fileTransferFuture = builder.writeMsg(CThread, builder.fileTransfer(session, data), Session.MessageType.THREAD);
                if (!await(fileTransferFuture)) {
                    System.out.println("Failed in sending file transfer data.");
                    System.exit(1);
                    return False;
                }
                data = file.read();
            }

//            System.out.printf("Thread#%d: Finished sending data. Inform server.\n", index);
            // finish packet
            data.size = completedFuture(0);
            data.data = ByteBuffer.allocate(0);
            fileTransferFuture = builder.writeMsg(CThread, builder.fileTransfer(session, data), Session.MessageType.THREAD);
            if (!await(fileTransferFuture)) {
                System.out.println("Failed in sending file transfer finish data.");
                System.exit(1);
                return False;
            }

            System.out.printf("Thread#%d: Fine. Everything done.\n", index);
            CThread.close();

            return completedFuture(true);

        } catch (NotOurMsgException e) {
            // debug only
            e.printStackTrace();
            System.out.println(e.getMessage());
            System.exit(1);
            return False;
        } catch (CompletionException | InterruptedException | ExecutionException | IOException e) {
            // debug only
            e.printStackTrace();
            System.out.println("Exception while sending data.");
            System.exit(1);
            return False;
        }
    }

    public CompletableFuture<Boolean> runSession() {
        long start = System.nanoTime();
        AsynchronousSocketChannel CSession;
        try {

            CSession = AsynchronousSocketChannel.open();

            System.out.print("Connecting to server...");

            CompletableFuture<Void> connectFuture = new CompletableFuture<>();
            CSession.connect(server, null, Reader.regToFuture(connectFuture));
            await(connectFuture);

            System.out.println("Connected.");
            System.out.print("Sending server hello...");

            CompletableFuture<Boolean> serverHelloResult = builder.writeMsg(
                    CSession, builder.serverHello(), Session.MessageType.SESSION);
            await(serverHelloResult);

            System.out.println("Sent.");
            System.out.print("Receiving client hello...");

            CompletableFuture<byte[]> clientHelloFuture = reader.readMsg(CSession, Session.MessageType.SESSION);

            Session.ClientHelloResult helloResult = reader.clientHello(await(clientHelloFuture));

            System.out.println("Received.");
            switch (helloResult.status) {
                case BAD_PASSWORD:
                    System.out.println("Server using another key. Check your key.");
                    System.exit(1);
                    return False;
                case BAD_VERSION:
                    System.out.println("Server version conflict. Check server version.");
                    System.exit(1);
                    return False;
                case OK:
                    session = helloResult.session;
                    System.out.println("Handshake successful.");
            }

            Session.FileNegotiationInfo negotiationInfo = new Session.FileNegotiationInfo();
            negotiationInfo.fileLength = file.getSize();
            negotiationInfo.filePath = file_;
            negotiationInfo.pieceSize = size;

            System.out.print("Sending file negotiation info...");
            CompletableFuture<Boolean> negotiationInfoSendResult = builder.writeMsg(
                    CSession, builder.fileNegotiation(session, negotiationInfo), Session.MessageType.SESSION);

            if (!await(negotiationInfoSendResult)) {
                System.out.println("Failed sending data.");
                System.exit(1);
                return False;
            }

            System.out.println("Sent.");
            System.out.print("Receiving negotiation result...");

            CompletableFuture<byte[]> fileNegotiationReplyFuture = reader.readMsg(CSession, Session.MessageType.SESSION);

            Session.NegotiationStatus negotiationStatus = reader.fileNegotiationResult(session, await(fileNegotiationReplyFuture));

            switch (negotiationStatus) {
                case OPEN_FAIL:
                    System.out.println("Server can't open the file for reading.");
                    System.exit(1);
                    return False;
                case OK:
                    System.out.println("File negotiation Successful.");
            }

            System.out.println("Start spawning threads.");
            Vector<CompletableFuture<Boolean>> threads = new Vector<>(thread);

            for (int i = 0; i < thread; i++) {
                System.out.printf("Starting Thread #%d.\n", i);
                threads.add(runThread(i));
            }

            for (CompletableFuture<Boolean> i : threads) {
                await(i);
            }

            System.out.printf("File transfer finished in %f seconds.\n", ((double)(System.nanoTime() - start)) / 1000000000);
            return completedFuture(true);

        } catch (NotOurMsgException e) {
            // debug only
            e.printStackTrace();
            System.out.println(e.getMessage());
            System.exit(1);
            return False;
        } catch (CompletionException e) {
            // debug only
            e.printStackTrace();
            System.out.println("Exception while sending data.");
            System.exit(1);
            return False;
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Exception while opening socket.");
            System.exit(1);
            return False;
        }
    }
}
