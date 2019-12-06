package moe.tyty.fileuploader.Protocol;

import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class Session {
    public enum MessageType {SESSION, THREAD}
    public enum HelloStatus {OK, BAD_PASSWORD, BAD_VERSION}
    public enum NegotiationStatus {OK, OPEN_FAIL}
    Random random_source = new Random();

    public static class MsgGuess {
        public Session.MessageType type;
        public CompletableFuture<byte[]> message;
    }

    public static class ClientHelloResult {
        public HelloStatus status;
        public byte[] session;
    }

    public static class FileNegotiationInfo {
        public int pieceSize;
        public long fileLength;
        public String filePath;
    }

    byte[] session(int length) {
        ByteBuffer buffer = ByteBuffer.allocate(length);
        for (int i = 0; i < length; ++i) {
            buffer.put(i, (byte) random_source.nextInt(256));
        }
        return buffer.array();
    }
}
