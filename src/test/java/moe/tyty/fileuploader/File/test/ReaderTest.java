package moe.tyty.fileuploader.File.test;

import moe.tyty.fileuploader.File.Reader;
import org.junit.Test;
import java.io.File;
import java.util.Objects;

import static org.junit.Assert.*;

public class ReaderTest {

    private static String getResourcePath(String ResourceFile) {
        ClassLoader classLoader = ReaderTest.class.getClassLoader();
        File file = new File(Objects.requireNonNull(classLoader.getResource(ResourceFile)).getFile());
        return file.getAbsolutePath();
    }

    @Test
    public void read_full_piece() throws Exception {
        String path = getResourcePath("test_fullpiece");
        Reader reader = new Reader(path, 16);

        // file is 32 bytes
        assertEquals(reader.getSize(), 32);

        Reader.ReadData data = reader.read();

        // first read. should read out 16 bytes data
        assertEquals(Integer.valueOf(16), data.size.get());

        // first read. verify data
        assertEquals(0, data.order);
        assertArrayEquals("0123456789ABCDEF".getBytes(), data.data.array());

        data = reader.read();

        // second read. should read out 16 bytes data
        assertEquals(Integer.valueOf(16), data.size.get());

        // second read. verify data
        assertEquals(1, data.order);
        assertArrayEquals("0123456789ABCDEF".getBytes(), data.data.array());

        // third read. Nothing left
        data = reader.read();

        // should remind data not available
        assertFalse(data.ok);
    }

    @Test
    public void read_short_piece() throws Exception {
        String path = getResourcePath("test_shortpiece");
        Reader reader = new Reader(path, 16);

        // file is 31 bytes
        assertEquals(reader.getSize(), 31);

        Reader.ReadData data = reader.read();

        // first read. should read out 16 bytes data
        assertEquals(Integer.valueOf(16), data.size.get());

        // first read. verify data
        assertEquals(0, data.order);
        assertArrayEquals("0123456789ABCDEF".getBytes(), data.data.array());

        data = reader.read();

        // second read. should read out 15 bytes data
        assertEquals(Integer.valueOf(15), data.size.get());

        // second read. verify data
        assertEquals(1, data.order);
        assertArrayEquals("0123456789ABCDE".getBytes(), data.data.array());

        // third read. Nothing left
        data = reader.read();

        // should remind data not available
        assertFalse(data.ok);
    }

    @Test(expected = moe.tyty.fileuploader.Exception.FileOpenException.class)
    public void read_non_exist() {
        //noinspection unused
        Reader reader = new Reader("ABSOLUTELY_NON_EXIST_PATH", 16);
    }
}
