package moe.tyty.fileuploader.Protocol;

import moe.tyty.fileuploader.Cipher.Encrypter;
import moe.tyty.fileuploader.File.Reader;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class Constructor {
    static byte[] MAGIC_HEADER = {84, 89};
    static byte[] MAGIC_HEADER_TRANSFER = {89, 84};
    static byte[] VERSION = {1, 1, 1, 1};
    static byte[] PADDING_FE = {
            (byte) 0xfe, (byte) 0xfe, (byte) 0xfe, (byte) 0xfe,
            (byte) 0xfe, (byte) 0xfe, (byte) 0xfe, (byte) 0xfe,
            (byte) 0xfe, (byte) 0xfe, (byte) 0xfe, (byte) 0xfe,
            (byte) 0xfe, (byte) 0xfe, (byte) 0xfe, (byte) 0xfe
    };
    static byte[] PADDING_FF = {
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff
    };

    Encrypter enc;

    public Constructor(String password) {
        enc = new Encrypter(password);
    }


    public static <T extends Number> byte[] fixedLength(T number, int length) {
        return String.format("%0" + length +"x", number.longValue()).getBytes();
    }

    public boolean writeMsg(AsynchronousSocketChannel channel, byte[] message, Session.MessageType type) {
        Future<Integer> head = channel.write(ByteBuffer.wrap(MAGIC_HEADER));
        Future<Integer> length = channel.write(ByteBuffer.wrap(String.format("%08x", message.length).getBytes()));
        Future<Integer> msg = channel.write(ByteBuffer.wrap(message));
        try {
            return head.get() == 2 && length.get() == 8 && msg.get() == message.length;
        } catch (InterruptedException | ExecutionException e) {
            return false;
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
        return buffer.array();
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
        ByteBuffer buffer = ByteBuffer.allocate(48);
        buffer.put(PADDING_FE);
        buffer.put(session);
        return enc.encrypt(buffer.array());
    }

    public byte[] fileTransfer(byte[] session, Reader.ReadData data) throws ExecutionException, InterruptedException {
        int size = data.size.get();
        ByteBuffer buffer = ByteBuffer.allocate(64 + size);
        buffer.put(PADDING_FF);
        buffer.put(session);
        buffer.put(fixedLength(data.order, 8));
        buffer.put(fixedLength(size, 8));
        data.data.position(0);
        buffer.put(data.data);
        return enc.encrypt(buffer.array());
    }
}
