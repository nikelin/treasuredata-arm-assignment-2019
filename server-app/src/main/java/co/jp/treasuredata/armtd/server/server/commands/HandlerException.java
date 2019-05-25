package co.jp.treasuredata.armtd.server.server.commands;

public class HandlerException extends Throwable {

    public HandlerException() {
        super();
    }

    public HandlerException(String message) {
        super(message);
    }

    public HandlerException(String message, Throwable cause) {
        super(message, cause);
    }
}
