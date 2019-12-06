package moe.tyty.fileuploader.Exception;

public class PeerSideException extends RuntimeException {
    public PeerSideException() {
        super();
    }
    public PeerSideException(String message) {
        super(message);
    }
    public PeerSideException(String message, Throwable cause) {
        super(message, cause);
    }
}
