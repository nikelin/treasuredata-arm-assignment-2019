package co.jp.treasuredata.armtd.client.commands;

import co.jp.treasuredata.armtd.client.ExecutionContext;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class IndexCommandHandler implements CommandHandler {

  @Override
  public String getKey() {
    return "index";
  }

  @Override
  public void execute(ExecutionContext context) throws CommandException {
    try {
      List<String> files = context.getConnector().listFiles().get(3, TimeUnit.SECONDS);
      context.out().println(files.size() + " entries resolved:");
      for (int i = 0; i < files.size(); i++) {
        context.out().println("[" + (i + 1) + "] " + files.get(i));
      }
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      throw new CommandException("Execution failed", e);
    }
  }
}
