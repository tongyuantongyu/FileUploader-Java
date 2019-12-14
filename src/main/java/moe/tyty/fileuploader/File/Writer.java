package moe.tyty.fileuploader.File;

import moe.tyty.fileuploader.Exception.FileOpenException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * class Writer is a wrapper to java.nio.channels.AsynchronousFileChannel to provide simple simple call to write out
 * a part of file providing the part's offset and size info. These info was retrieved from the info received from the
 * client and will later reconstruct the entire file.
 *
 * @author TYTY
 */
public class Writer {
    private AsynchronousFileChannel file;
    private final int buff_s;
    private boolean working;
    public volatile boolean finish = false;
    public boolean closed = false;
    private final AtomicInteger writingTask = new AtomicInteger(0);


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
        try {
            Path _p = Paths.get(path).getRoot();
            if (_p == null) {
                if (Files.getFileStore(Paths.get("./")).getUsableSpace() < size) {
                    throw new FileOpenException("No enough space.");
                }
            }
            else {
                if (Files.getFileStore(_p).getUsableSpace() < size) {
                    throw new FileOpenException("No enough space.");
                }
                try {
                    Files.createDirectories(Paths.get(path).getParent());
                } catch (FileAlreadyExistsException ignore) {}
            }
            file = AsynchronousFileChannel.open(Paths.get(path),
                    StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            working = true;
        }
        catch (IOException e) {
            throw new FileOpenException(e);
        }
    }

    static public class WriteData {
        public boolean OK;
        public int order;
        public ByteBuffer data;
    }

    /**
     * write a piece of file
     * @param data data of the piece. ByteBuffer carries length info so no length info is required.
     * @return whether write operation is successful.
     */
    public boolean write(WriteData data) {
        if (!working || !data.OK) return false;
        try {
            writingTask.incrementAndGet();
            file.write(data.data, ((long) data.order) * buff_s, null, new CompletionHandler<Integer, Void>() {
                @Override
                public void completed(Integer result, Void attachment) {
                    if (finish && !closed && writingTask.decrementAndGet() == 0) {
                        try {
                            file.close();
                            System.out.println("File Closed.");
                            closed = true;
                        } catch (IOException e) {
                            e.printStackTrace();
                            System.out.println("Exception while closing file.");
                        }
                    }
                }

                @Override
                public void failed(Throwable exc, Void attachment) {
                    throw new FileOpenException(exc);
                }
            });
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * make sure everything has been wrote to the disk.
     */
    public void close() {
        if (!working) return;
        if (finish && !closed && writingTask.get() == 0) {
            try {
                file.close();
                System.out.println("File Closed.");
                closed = true;
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Exception while closing file.");
            }
        }
    }
}
