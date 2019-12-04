package moe.tyty.fileuploader.Protocol;

import java.nio.ByteBuffer;
import java.util.Random;

public class Session {
    public enum MessageType {SESSION, THREAD}
    public enum HelloStatus {OK, BAD_PASSWORD, BAD_VERSION}
    public enum NegotiationStatus {OK, OPEN_FAIL}
    Random random_source = new Random();

    public static class MsgGuess {
        Session.MessageType type;
        byte[] message;
    }

    public static class ClientHelloResult {
        HelloStatus status;
        byte[] session;
    }

    public static class FileNegotiationInfo {
        int pieceSize;
        long fileLength;
        String filePath;
    }

    byte[] session(int length) {
        ByteBuffer buffer = ByteBuffer.allocate(length);
        for (int i = 0; i < length; ++i) {
            buffer.put(i, (byte) random_source.nextInt(256));
        }
        return buffer.array();
    }
}
