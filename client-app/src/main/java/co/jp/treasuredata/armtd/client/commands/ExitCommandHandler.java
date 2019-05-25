package co.jp.treasuredata.armtd.client.commands;

import co.jp.treasuredata.armtd.client.ExecutionContext;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ExitCommandHandler implements CommandHandler {

    @Override
    public String getKey() {
        return "stop";
    }

    @Override
    public void execute(ExecutionContext context) throws CommandException {
        try {
            context.getConnector().quit().get(3, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new CommandException("Execution failed", e);
        }
    }
}
