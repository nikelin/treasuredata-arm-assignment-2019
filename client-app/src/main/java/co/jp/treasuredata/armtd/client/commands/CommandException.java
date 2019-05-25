package co.jp.treasuredata.armtd.client.commands;

public class CommandException extends Throwable {

    public CommandException() {
        super();
    }

    public CommandException(String message) {
        super(message);
    }

    public CommandException(String message, Throwable cause) {
        super(message, cause);
    }
}
