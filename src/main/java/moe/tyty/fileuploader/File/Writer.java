package moe.tyty.fileuploader.File;

import moe.tyty.fileuploader.Exception.FileOpenException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * class Writer is a wrapper to java.nio.channels.AsynchronousFileChannel to provide simple simple call to write out
 * a part of file providing the part's offset and size info. These info was retrieved from the info received from the
 * client and will later reconstruct the entire file.
 *
 * @author TYTY
 */
public class Writer {
    private Vector<Future<Integer>> asyncFileWriteFutureVector;
    private AsynchronousFileChannel file;
    private int buff_s;
    private boolean working;

    /**
     * Construct method
     * @param path path of the file to be write. exist file with same name will be replaced.
     * @param size
     * size of the entire file to be write. An available disk space check will be performed to prevent exception
     * this is still not perfect as multiple files can be opened for writing simultaneously.
     * @throws FileOpenException Throws when file can't be opened for writing. Server will shutdown the connection then.
     */
    public Writer(String path, long size, int buff_s) throws FileOpenException {
        this.buff_s = buff_s;
        asyncFileWriteFutureVector = new Vector<>();
        try {
            if (Files.getFileStore(Paths.get("./")).getUsableSpace() < size) {
                throw new FileOpenException();
            }
            file = AsynchronousFileChannel.open(Paths.get(path),
                    StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            working = true;
        }
        catch (IOException e) {
            throw new FileOpenException();
        }
    }

    static public class WriteData {
        public int order;
        public ByteBuffer data;
    }

    /**
     * write a piece of file
     * @param data data of the piece. ByteBuffer carries length info so no length info is required.
     * @return whether write operation is successful.
     */
    public boolean write(WriteData data) {
        if (!working) return false;
        try {
            asyncFileWriteFutureVector.addElement(file.write(data.data, ((long) data.order) * buff_s));
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * make sure everything has been wrote to the disk.
     * @throws ExecutionException strange exception during async operation. Simply passing the exception.
     * @throws InterruptedException interrupt issued by user. Simply passing the exception.
     */
    public void close() throws ExecutionException, InterruptedException {
        if (!working) return;
        for (Future<Integer> future : asyncFileWriteFutureVector) {
            future.get();
        }
    }
}
