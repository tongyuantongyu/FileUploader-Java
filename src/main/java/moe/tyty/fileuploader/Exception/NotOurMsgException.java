package moe.tyty.fileuploader.Exception;

public class NotOurMsgException extends RuntimeException {
    public NotOurMsgException() {
        super();
    }
    public NotOurMsgException(String message) {
        super(message);
    }
}
