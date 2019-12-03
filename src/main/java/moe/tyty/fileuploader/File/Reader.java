package moe.tyty.fileuploader.File;

import moe.tyty.fileuploader.Exception.FileOpenException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * class Reader is a wrapper to java.nio.channels.AsynchronousFileChannel to provide simple call to read out
 * a part of file along with the part's offset and size info. These info will later be used to construct
 * message to be sent to the server.
 *
 * @author TYTY
 */
public class Reader {
    private AsynchronousFileChannel file;
    public long size;
    private int buff_s;
    private long offset;
    private boolean working;

    /**
     * Constructor method
     * @param path path to the file to be read.
     * @param buffer_size file piece size to be read into buffer.
     * @throws FileOpenException Throws when file can't be opened for reading. Client will simply exit then.
     */
    public Reader(String path, int buffer_size) throws FileOpenException {
        buff_s = buffer_size;
        try {
            file = AsynchronousFileChannel.open(Paths.get(path), StandardOpenOption.READ);
            size = file.size();
            working = true;
        }
        catch (IOException e) {
            throw new FileOpenException();
        }
    }

    /**
     * class ReadData is simply bundles necessary info into one object for passing.
     *
     * @author TYTY
     */
    public static class ReadData {
        public boolean ok = false;
        public long offset;
        public Future<Integer> size;
        public ByteBuffer data;
    }

    /**
     * Read a piece of file.
     * @return ReadData object that contains the data read.
     * @throws ExecutionException strange exception during async operation. Simply passing the exception.
     * @throws InterruptedException interrupt issued by user. Simply passing the exception.
     */
    public ReadData read() throws ExecutionException, InterruptedException {
        ReadData result = new ReadData();
        if (!working) {
            return result;
        }
        if (offset >= size) {
            working = false;
            return result;
        }
        if (offset + buff_s > size) {
            result.data = ByteBuffer.allocate((int) (size - offset));
            result.size = file.read(result.data, offset);
            result.offset = offset;
            result.ok = true;
            offset = size;
        }
        else {
            result.data = ByteBuffer.allocate(buff_s);
            result.size = file.read(result.data, offset);
            result.offset = offset;
            result.ok = true;
            offset += buff_s;
        }
        return result;
    }

    /**
     * get size of the file.
     * @return file size.
     */
    public long getSize() {
        return size;
    }
}
