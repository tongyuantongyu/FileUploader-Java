package moe.tyty.fileuploader;

import moe.tyty.fileuploader.Exception.BadOptionException;
import moe.tyty.fileuploader.Exception.PeerSideException;
import moe.tyty.fileuploader.Protocol.Constructor;
import moe.tyty.fileuploader.Protocol.Reader;
import moe.tyty.fileuploader.Protocol.Session;

import static com.ea.async.Async.await;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.file.Paths;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * class Client represents a client to upload a file to the server.
 * It holds file info as well as threads.
 *
 * @author TYTY
 */
public class Client {

    public static class OptionPack {
        InetAddress host;
        int port;
        String key;
        String file;
        int thread = 1;
        int size = 65536;
        String srv_file;
    }

    public static OptionPack buildOption(String host, String port, String key, String file,
                                         String thread, String size, String srv_file, boolean IPv4, boolean IPv6) {
        OptionPack option = new OptionPack();
        if (host == null || port == null || key == null || file == null) {
            throw new BadOptionException("One or more required parameters not given.", true);
        }

        try {
            option.port = Integer.parseUnsignedInt(port);
            if (!(option.port > 0 && option.port < 65536)) {
                throw new BadOptionException("port parameter should be an int in range (0, 65536).");
            }
        } catch (NumberFormatException e) {
            throw new BadOptionException("port parameter should be an number.");
        }

        try {
            if (IPv4 && IPv6) {
                throw new BadOptionException("-4 and -6 should not be used together.");
            } else if (!IPv4 && !IPv6) {
                option.host = InetAddress.getByName(host);
            } else if (IPv4) {
                for (InetAddress address : InetAddress.getAllByName(host)) {
                    if (address instanceof Inet4Address) {
                        option.host = address;
                        break;
                    }
                }
            } else {
                for (InetAddress address : InetAddress.getAllByName(host)) {
                    if (address instanceof Inet6Address) {
                        option.host = address;
                        break;
                    }
                }
            }
            if (option.host == null) {
                throw new UnknownHostException();
            }
        } catch (UnknownHostException e) {
            throw new BadOptionException("No available ip to the given host.");
        }

        System.out.printf("Server: [%s]:%d\n", option.host.getHostAddress(), option.port);

        if (key.length() < 8) {
            System.err.println("You are using a short key which is weak to be attacked.");
        }

        option.key = key;

        File _path = Paths.get(file).toFile();
        if (!_path.isFile() || !_path.canRead()) {
            throw new BadOptionException("The file can't be opened for reading.");
        }
        System.out.printf("File: %s\n", file);
        option.file = file;

        if (thread != null) {
            try {
                option.thread = Integer.parseUnsignedInt(thread);
                if (option.thread < 1) {
                    throw new BadOptionException("thread parameter should be an unsigned int.");
                }
            } catch (NumberFormatException e) {
                throw new BadOptionException("thread parameter should be an number.");
            }
        }

        if (size != null) {
            try {
                option.size = Integer.parseUnsignedInt(size);
                if (option.size < 1) {
                    throw new BadOptionException("size parameter should be an unsigned int.");
                }
                if (option.size < 64) {
                    System.err.println("Your size parameter is too small." +
                            " We'll go on, but transfer can be inefficient.");
                }
            } catch (NumberFormatException e) {
                throw new BadOptionException("size parameter should be an number.");
            }
        }

        System.out.printf("Using thread: %d, piece size: %d\n", option.thread, option.size);

        if (srv_file == null) {
            option.srv_file = option.file;
        } else {
            option.srv_file = srv_file;
        }
        return option;
    }

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

    public Client(InetAddress host, int port, String key, String file_, int thread, int size, String srv_file) {
        file = new moe.tyty.fileuploader.File.Reader(file_, size);
        builder = new Constructor(key);
        reader = new Reader(key);
        server = new InetSocketAddress(host, port);
        this.host = host;
        this.port = port;
        this.key = key;
        this.file_ = srv_file;
        this.size = size;
        this.thread = thread;
    }

    public Client(InetAddress host, int port, String key, String file_, int thread, int size) {
        this(host, port, key, file_, thread, size, file_);
    }

    public Client(OptionPack option) {
        this(option.host, option.port, option.key, option.file, option.thread, option.size, option.srv_file);
    }

    /**
     * start a thread to transfer data
     *
     * @param index thread index used to print information
     * @return if thread finished normally or abnormally
     */
    public CompletableFuture<Boolean> runThread(int index) throws ExecutionException, InterruptedException {
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
                System.out.printf("Thread#%d: Failed in sending file transfer init data.\n", index);
                return False;
            }
//            System.out.println("Sent.");
            System.out.printf("Thread#%d: Start sending data.\n", index);
            moe.tyty.fileuploader.File.Reader.ReadData data = file.read();
            CompletableFuture<Boolean> fileTransferFuture;

            while (data.ok) {
                fileTransferFuture = builder.writeMsg(CThread, builder.fileTransfer(session, data), Session.MessageType.THREAD);
                if (!await(fileTransferFuture)) {
                    System.out.printf("Thread#%d: Failed in sending file transfer data.\n", index);
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
                System.out.printf("Thread#%d: Failed in sending file transfer finish data.\n", index);
                return False;
            }

            System.out.printf("Thread#%d: Fine. Everything done.\n", index);
            CThread.close();

            return completedFuture(true);

        } catch (IOException e) {
            throw new PeerSideException("Exception while sending data.", e);
        }
    }

    /**
     * start connect server and do handshake, and when finished, start file transfer thread.
     *
     * @return if completed normally
     */
    public CompletableFuture<Double> runSession() throws IOException, ExecutionException, InterruptedException {
        long start = System.nanoTime();
        AsynchronousSocketChannel CSession;

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
                throw new PeerSideException("Server using another key. Check your key.");
            case BAD_VERSION:
                throw new PeerSideException("Server version conflict. Check server version.");
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
            throw new PeerSideException("Failed sending data.");
        }

        System.out.println("Sent.");
        System.out.print("Receiving negotiation result...");

        CompletableFuture<byte[]> fileNegotiationReplyFuture = reader.readMsg(CSession, Session.MessageType.SESSION);

        Session.NegotiationStatus negotiationStatus = reader.fileNegotiationResult(session, await(fileNegotiationReplyFuture));

        switch (negotiationStatus) {
            case OPEN_FAIL:
                throw new PeerSideException("Server can't open the file for reading.");
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

        System.out.printf("File transfer finished in %f seconds.\n", ((double) (System.nanoTime() - start)) / 1000000000);
        return completedFuture(((double) (System.nanoTime() - start)) / 1000000000);
    }
}
