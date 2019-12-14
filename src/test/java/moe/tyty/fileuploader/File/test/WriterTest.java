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
    public final TemporaryFolder tmpFolder = new TemporaryFolder();

    @Test
    public void write_full_piece() throws Exception {
        String TestPath = tmpFolder.getRoot().getAbsolutePath();
        Writer writer = new Writer(TestPath + "/test_fullpiece", 32, 16);

        // first write
        Writer.WriteData data = new Writer.WriteData();
        data.OK = true;
        data.data = ByteBuffer.wrap("0123456789ABCDEF".getBytes());
        data.order = 0;
        assertTrue(writer.write(data));

        // second write
        data.data = ByteBuffer.wrap("0123456789ABCDEF".getBytes());
        data.order = 1;
        assertTrue(writer.write(data));

        writer.close();

        // new writer implementation require some more time to ensure writing finished. 500ms should be far more than enough.
        Thread.sleep(500);

        byte[] array = Files.readAllBytes(Paths.get(TestPath + "/test_fullpiece"));
        assertArrayEquals("0123456789ABCDEF0123456789ABCDEF".getBytes(), array);
    }

    @Test
    public void write_short_piece() throws Exception {
        String TestPath = tmpFolder.getRoot().getAbsolutePath();
        Writer writer = new Writer(TestPath + "/test_shortpiece", 31, 16);

        // first write
        Writer.WriteData data = new Writer.WriteData();
        data.OK = true;
        data.data = ByteBuffer.wrap("0123456789ABCDEF".getBytes());
        data.order = 0;
        assertTrue(writer.write(data));

        // second write
        data.data = ByteBuffer.wrap("0123456789ABCDE".getBytes());
        data.order = 1;
        assertTrue(writer.write(data));

        writer.close();

        // see L37
        Thread.sleep(500);

        byte[] array = Files.readAllBytes(Paths.get(TestPath + "/test_shortpiece"));
        assertArrayEquals("0123456789ABCDEF0123456789ABCDE".getBytes(), array);
    }

    @Test(expected = moe.tyty.fileuploader.Exception.FileOpenException.class)
    public void write_too_big() {
        String TestPath = tmpFolder.getRoot().getAbsolutePath();
        //noinspection unused
        Writer writer = new Writer(TestPath + "/TOO_BIG_FILE", Long.MAX_VALUE, 16);
    }
}
