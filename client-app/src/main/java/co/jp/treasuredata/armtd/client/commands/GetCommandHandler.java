package co.jp.treasuredata.armtd.client.commands;

import co.jp.treasuredata.armtd.client.ExecutionContext;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class GetCommandHandler implements CommandHandler {

    @Override
    public String getKey() {
        return "get";
    }

    @Override
    public void execute(ExecutionContext context) throws CommandException {
        try {
            context.out().println("Type the file name you want to fetch:");
            String fileName = context.in().readLine();
            if (fileName == null || fileName.isEmpty()) {
                context.out().println("[ERROR] File name can't be empty");
                return;
            }

            CompletableFuture<String> future = context.getConnector().fetchFile(fileName);
            final String fileContents;
            try {
                fileContents = future.get(3, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                throw new CommandException("Execution failed", e);
            }

            context.out().println(fileContents.length() + " characters received: ");
            context.out().println(fileContents);
        } catch (IOException e) {
            throw new CommandException("I/O failed", e);
        }
    }
}
