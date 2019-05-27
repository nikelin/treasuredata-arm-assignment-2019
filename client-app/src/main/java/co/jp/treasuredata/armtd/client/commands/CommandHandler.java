package co.jp.treasuredata.armtd.client.commands;

import co.jp.treasuredata.armtd.client.ExecutionContext;

public interface CommandHandler {

  String getKey();

  void execute(ExecutionContext context) throws CommandException;
}
