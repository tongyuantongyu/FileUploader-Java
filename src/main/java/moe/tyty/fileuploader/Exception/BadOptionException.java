package moe.tyty.fileuploader.Exception;

public class BadOptionException extends RuntimeException {
    public boolean showHelp = false;
    public BadOptionException(String message, boolean showHelp) {
        super(message);
        this.showHelp = showHelp;
    }
    public BadOptionException(String message) {
        super(message);
    }
}
