package moe.tyty.fileuploader.Protocol;

import moe.tyty.fileuploader.Cipher.Encrypter;
import moe.tyty.fileuploader.File.Reader;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import static com.ea.async.Async.await;
import static java.util.concurrent.CompletableFuture.completedFuture;

public class Constructor {
    static final byte[] MAGIC_HEADER = {84, 89};
    static final byte[] MAGIC_HEADER_TRANSFER = {89, 84};
    static final byte[] VERSION = {1, 1, 1, 1};

    final Encrypter enc;

    public Constructor(String password) {
        enc = new Encrypter(password);
    }

    public static <T> CompletionHandler<T, Void> regToFuture(CompletableFuture<T> completableFuture) {
        return new CompletionHandler<T, Void>() {
            @Override
            public void completed(T result, Void attachment) {
                completableFuture.complete(result);
            }

            @Override
            public void failed(Throwable exc, Void attachment) {
                completableFuture.completeExceptionally(exc);
            }
        };
    }

    public static <T extends Number> byte[] fixedLength(T number, int length) {
        return String.format("%0" + length + "x", number.longValue()).getBytes();
    }

    public CompletableFuture<Boolean> writeMsg(AsynchronousSocketChannel channel, byte[] message, Session.MessageType type) {
        try {
            CompletableFuture<Integer> headFuture = new CompletableFuture<>();
            switch (type) {
                case SESSION:
                    channel.write(ByteBuffer.wrap(MAGIC_HEADER), null, regToFuture(headFuture));
                    break;
                case THREAD:
                    channel.write(ByteBuffer.wrap(MAGIC_HEADER_TRANSFER), null, regToFuture(headFuture));
                    break;
                case UNKNOWN:
                    return completedFuture(false);
            }
            Integer headLength = await(headFuture);

            CompletableFuture<Integer> lengthFuture = new CompletableFuture<>();
            channel.write(ByteBuffer.wrap(String.format("%08x", message.length).getBytes()), null, regToFuture(lengthFuture));
            Integer lengthLength = await(lengthFuture);

            CompletableFuture<Integer> msgFuture = new CompletableFuture<>();
            channel.write(ByteBuffer.wrap(message), null, regToFuture(msgFuture));
            Integer msgLength = await(msgFuture);

            return completedFuture(headLength.equals(2) && lengthLength.equals(8) && msgLength.equals(message.length));
        } catch (CompletionException e) {
            // for debug
            e.printStackTrace();
            return completedFuture(false);
        }
    }

    public static byte[] getWrittenByte(ByteBuffer buffer) {
        byte[] data = new byte[buffer.position()];
        buffer.position(0);
        buffer.get(data);
        return data;
    }

    public byte[] serverHello() {
        ByteBuffer buffer = ByteBuffer.allocate(6);
        buffer.put(MAGIC_HEADER);
        buffer.put(VERSION);
        return enc.encrypt(buffer.array());
    }

    public byte[] clientHello(Session.HelloStatus status, byte[] session) {
        ByteBuffer buffer = ByteBuffer.allocate(64);
        switch (status) {
            case OK:
                buffer.put((byte) 0);
                buffer.put(enc.encrypt(session));
                break;
            case BAD_PASSWORD:
                buffer.put((byte) 1);
                break;
            case BAD_VERSION:
                buffer.put((byte) 2);
                buffer.put(enc.encrypt(VERSION));
        }
        return getWrittenByte(buffer);
    }

    public byte[] fileNegotiation(byte[] session, Session.FileNegotiationInfo info) {
        ByteBuffer buffer= ByteBuffer.allocate(312);
        buffer.put(session);
        buffer.put(fixedLength(info.pieceSize, 8));
        buffer.put(fixedLength(info.fileLength, 16));
        buffer.put(info.filePath.getBytes());
        return enc.encrypt(getWrittenByte(buffer));
    }

    public byte[] fileNegotiationReply(byte[] session, Session.NegotiationStatus status) {
        ByteBuffer buffer = ByteBuffer.allocate(33);
        buffer.put(session);
        switch (status) {
            case OK:
                buffer.put((byte) 0);
                break;
            case OPEN_FAIL:
                buffer.put((byte) 1);
                break;
        }
        return enc.encrypt(buffer.array());
    }

    public byte[] fileTransferInit(byte[] session) {
        ByteBuffer buffer = ByteBuffer.allocate(32);
        buffer.put(session);
        return enc.encrypt(buffer.array());
    }

    public byte[] fileTransfer(byte[] session, Reader.ReadData data) throws ExecutionException, InterruptedException {
        int size = data.size.get();
        ByteBuffer buffer = ByteBuffer.allocate(48 + size);
        buffer.put(session);
        buffer.put(fixedLength(data.order, 8));
        buffer.put(fixedLength(size, 8));
        data.data.position(0);
        buffer.put(data.data);
        return enc.encrypt(buffer.array());
    }
}
