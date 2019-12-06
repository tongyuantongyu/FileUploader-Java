package moe.tyty.fileuploader.Protocol;

import moe.tyty.fileuploader.Cipher.Decrypter;
import moe.tyty.fileuploader.Exception.NotOurMsgException;
import moe.tyty.fileuploader.File.Writer;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static java.util.concurrent.CompletableFuture.completedFuture;

import static com.ea.async.Async.await;

public class Reader {
    static byte[] MAGIC_HEADER = {84, 89};
    static byte[] MAGIC_HEADER_TRANSFER = {89, 84};
    static byte[] VERSION = {1, 1, 1, 1};

    Decrypter dec;

    public Reader(String password) {
        dec = new Decrypter(password);
    }

    public static <T> CompletionHandler<T, Void> regToFuture(CompletableFuture<T> completableFuture) {
        return new CompletionHandler<>() {
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

    static CompletableFuture<Boolean> isOurMsg(AsynchronousSocketChannel channel) {
        ByteBuffer head = ByteBuffer.allocate(2);
        CompletableFuture<Integer> headFuture = new CompletableFuture<>();
        channel.read(head, null, regToFuture(headFuture));
        try {
            return completedFuture(await(headFuture).equals(2) && Arrays.equals(head.array(), MAGIC_HEADER));
        } catch (CompletionException e) {
            throw new NotOurMsgException("Failed in reading session head.", e);
        }
    }

    static CompletableFuture<Boolean> isOurMsgTransfer(AsynchronousSocketChannel channel) {
        ByteBuffer head = ByteBuffer.allocate(2);
        CompletableFuture<Integer> headFuture = new CompletableFuture<>();
        channel.read(head, null, regToFuture(headFuture));
        try {
            return completedFuture(await(headFuture).equals(2) && Arrays.equals(head.array(), MAGIC_HEADER_TRANSFER));
        } catch (CompletionException e) {
            throw new NotOurMsgException("Failed in reading transfer head.", e);
        }
    }

    public CompletableFuture<byte[]> readVerifiedMsg(AsynchronousSocketChannel channel) {
        try {
            Integer futureLength;
            ByteBuffer raw_length = ByteBuffer.allocate(8);
            CompletableFuture<Integer> lengthFuture = new CompletableFuture<>();
            channel.read(raw_length, null, regToFuture(lengthFuture));
            futureLength = await(lengthFuture);
            if (!futureLength.equals(8)) {
                throw new NotOurMsgException(String.format(
                        "Bad data in reading message length info. Length field except length 8, got %d", futureLength));
            }
            int length = Integer.parseUnsignedInt(new String(raw_length.array()), 16);
            ByteBuffer message = ByteBuffer.allocate(length);
            CompletableFuture<Integer> dataFuture = new CompletableFuture<>();
            channel.read(message, null, regToFuture(dataFuture));
            futureLength = await(dataFuture);
            if (!futureLength.equals(length)) {
                throw new NotOurMsgException(String.format(
                        "Bad data in reading message data. Data field except length %d, got %d", length, futureLength));
            }
            return completedFuture(message.array());
        } catch (CompletionException e) {
            throw new NotOurMsgException("Failed in reading transfer head.", e);
        }
    }

    public CompletableFuture<byte[]> readMsg(AsynchronousSocketChannel channel, Session.MessageType type) {
        switch (type) {
            case SESSION:
                if (!await(isOurMsg(channel))) {
                    throw new NotOurMsgException("Received connection but not a session.");
                }
                break;
            case THREAD:
                if (!await(isOurMsgTransfer(channel))) {
                    throw new NotOurMsgException("Received connection but not a thread.");
                }
                break;
        }
        return readVerifiedMsg(channel);
    }

    public CompletableFuture<Session.MsgGuess> readMsgGuess(AsynchronousSocketChannel channel) {
        Session.MsgGuess result = new Session.MsgGuess();
        ByteBuffer head = ByteBuffer.allocate(2);
        CompletableFuture<Integer> headFuture = new CompletableFuture<>();
        channel.read(head, null, regToFuture(headFuture));
        Integer futureLength;
        try {
            futureLength = await(headFuture);
            if (!futureLength.equals(2)) {
                throw new NotOurMsgException(String.format(
                        "Bad data in reading message head. Head field except length 2, got %d", futureLength));
            }
        } catch (CompletionException e) {
            throw new NotOurMsgException("Failed in reading transfer head.", e);
        }

        byte[] head_byte = head.array();
        if (Arrays.equals(head.array(), MAGIC_HEADER)) {
            result.type = Session.MessageType.SESSION;
        }
        else if (Arrays.equals(head.array(), MAGIC_HEADER_TRANSFER)) {
            result.type = Session.MessageType.THREAD;
        }
        else {
            throw new NotOurMsgException("Bad data in reading message type. Unknown connection type.");
        }
        result.message = readVerifiedMsg(channel);
        return completedFuture(result);
    }

    public static byte[] readFromBuffer(ByteBuffer buffer, int length) {
        byte[] data = new byte[length];
        buffer.get(data);
        return data;
    }

    public Session.HelloStatus serverHello(byte[] message) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(dec.decrypt(message));
            if (!Arrays.equals(readFromBuffer(buffer, 2), MAGIC_HEADER)) {
                return Session.HelloStatus.BAD_PASSWORD;
            }
            if (!Arrays.equals(readFromBuffer(buffer, 4), VERSION)) {
                return Session.HelloStatus.BAD_VERSION;
            }
            return Session.HelloStatus.OK;
        } catch (IndexOutOfBoundsException | BufferUnderflowException e) {
            return Session.HelloStatus.BAD_PASSWORD;
        }
    }

    public static byte[] trimSession(byte[] session) {
        if (session.length == 32) {
            return session;
        }
        else {
            return Arrays.copyOf(session, 32);
        }
    }

    public static byte[] trimZero(byte[] data) {
        if (data[data.length - 1] != 0) {
            return data;
        }
        else {
            return Arrays.copyOf(data, data.length - 1);
        }
    }

    public static byte[] readLeftFromBuffer(ByteBuffer buffer) {
        return trimZero(readFromBuffer(buffer, buffer.remaining()));
    }

    public Session.ClientHelloResult clientHello(byte[] message) {
        Session.ClientHelloResult result = new Session.ClientHelloResult();
        try {
            ByteBuffer buffer = ByteBuffer.wrap(message);
            switch (buffer.get()) {
                case 0:
                    result.status = Session.HelloStatus.OK;
                    result.session = trimSession(dec.decrypt(readLeftFromBuffer(buffer)));
                    return result;
                case 1:
                    result.status = Session.HelloStatus.BAD_PASSWORD;
                    return result;
                case 2:
                    result.status = Session.HelloStatus.BAD_VERSION;
                    return result;
                default:
                    throw new NotOurMsgException("Bad data in client hello status field.");
            }
        } catch (IndexOutOfBoundsException | BufferUnderflowException e) {
            throw new NotOurMsgException("Failed in reading client hello message.");
        }
    }

    public static void verifySession(byte[] session, ByteBuffer buffer) {
        if (!Arrays.equals(readFromBuffer(buffer, 32), session)) {
            throw new NotOurMsgException("Session Conflict.");
        }
    }

    public Session.FileNegotiationInfo fileNegotiation(byte[] session, byte[] message) {
        Session.FileNegotiationInfo result = new Session.FileNegotiationInfo();
        ByteBuffer buffer = ByteBuffer.wrap(dec.decrypt(message));
        try {
            verifySession(session, buffer);
            result.pieceSize = Integer.parseUnsignedInt(new String(readFromBuffer(buffer, 8)), 16);
            result.fileLength = Long.parseUnsignedLong(new String(readFromBuffer(buffer, 8)), 16);
            result.filePath = new String(readLeftFromBuffer(buffer));
            return result;
        } catch (IndexOutOfBoundsException | BufferUnderflowException e) {
            throw new NotOurMsgException("Failed in reading file negotiation message.");
        }
    }

    public Session.NegotiationStatus fileNegotiationResult(byte[] session, byte[] message) {
        ByteBuffer buffer = ByteBuffer.wrap(dec.decrypt(message));
        try {
            verifySession(session, buffer);
            switch (buffer.get()) {
                case 0:
                    return Session.NegotiationStatus.OK;
                case 1:
                    return Session.NegotiationStatus.OPEN_FAIL;
                default:
                    throw new NotOurMsgException("Bad data in file negotiation status field.");
            }
        } catch (IndexOutOfBoundsException | BufferUnderflowException e) {
            throw new NotOurMsgException("Failed in reading file negotiation result message.");
        }
    }

    public byte[] fileTransferInit(byte[] message) {
        try {
            byte[] session = dec.decrypt(message);
            return Arrays.copyOf(session, 32);
//            return Arrays.copyOf(dec.decrypt(message), 32);
        } catch (IndexOutOfBoundsException e) {
            throw new NotOurMsgException("Failed in reading file transfer init message.");
        }
    }

    public Writer.WriteData fileTransfer(byte[] session, byte[] message) {
        Writer.WriteData data = new Writer.WriteData();
        ByteBuffer buffer = ByteBuffer.wrap(dec.decrypt(message));
        try {
            readFromBuffer(buffer, 16);
            verifySession(session, buffer);
            data.order = Integer.parseUnsignedInt(new String(readFromBuffer(buffer, 8)), 16);
            int size = Integer.parseUnsignedInt(new String(readFromBuffer(buffer, 8)), 16);
            data.data = ByteBuffer.wrap(readFromBuffer(buffer, size));
            return data;
        } catch (IndexOutOfBoundsException | BufferUnderflowException e) {
            throw new NotOurMsgException("Failed in reading file transfer message.");
        }
    }
}
