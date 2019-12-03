package moe.tyty.fileuploader.Protocol;

import java.nio.ByteBuffer;
import java.util.Random;

public class Session {
    Random random_source = new Random();

    byte[] session(int length) {
        ByteBuffer buffer = ByteBuffer.allocate(length);
        for (int i = 0; i < length; ++i) {
            buffer.put(i, (byte) random_source.nextInt(256));
        }
        return buffer.array();
    }
}
