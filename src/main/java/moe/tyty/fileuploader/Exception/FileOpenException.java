package moe.tyty.fileuploader.Exception;

public class FileOpenException extends RuntimeException {
    public FileOpenException() {
        super();
    }
    public FileOpenException(String message) {
        super(message);
    }
    public FileOpenException(Throwable cause) {
        super(cause);
    }
}
