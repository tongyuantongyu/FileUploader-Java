package moe.tyty.fileuploader.File.test;

import moe.tyty.fileuploader.File.Writer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class WriterTest {
    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Test
    public void write_full_piece() throws Exception {
        String TestPath = tmpFolder.getRoot().getAbsolutePath();
        Writer writer = new Writer(TestPath + "/test_fullpiece", 32);

        // first write
        byte[] data = "0123456789ABCDEF".getBytes();
        ByteBuffer buffer = ByteBuffer.wrap(data);
        assertTrue(writer.write(buffer, 0));

        // second write
        data = "0123456789ABCDEF".getBytes();
        buffer = ByteBuffer.wrap(data);
        assertTrue(writer.write(buffer, 16));

        writer.close();

        byte[] array = Files.readAllBytes(Paths.get(TestPath + "/test_fullpiece"));
        assertArrayEquals("0123456789ABCDEF0123456789ABCDEF".getBytes(), array);
    }

    @Test
    public void write_short_piece() throws Exception {
        String TestPath = tmpFolder.getRoot().getAbsolutePath();
        Writer writer = new Writer(TestPath + "/test_shortpiece", 31);

        // first write
        byte[] data = "0123456789ABCDEF".getBytes();
        ByteBuffer buffer = ByteBuffer.wrap(data);
        assertTrue(writer.write(buffer, 0));

        // second write
        data = "0123456789ABCDE".getBytes();
        buffer = ByteBuffer.wrap(data);
        assertTrue(writer.write(buffer, 16));

        writer.close();

        byte[] array = Files.readAllBytes(Paths.get(TestPath + "/test_shortpiece"));
        assertArrayEquals("0123456789ABCDEF0123456789ABCDE".getBytes(), array);
    }

    @Test(expected = moe.tyty.fileuploader.Exception.FileOpenException.class)
    public void write_too_big() {
        String TestPath = tmpFolder.getRoot().getAbsolutePath();
        Writer writer = new Writer(TestPath + "/TOO_BIG_FILE", Long.MAX_VALUE);
    }
}
