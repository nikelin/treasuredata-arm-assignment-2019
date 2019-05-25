package co.jp.treasuredata.armtd.client.commands;

import co.jp.treasuredata.armtd.client.ExecutionContext;

import java.io.BufferedReader;
import java.io.PrintWriter;

public interface CommandHandler {

    String getKey();

    void execute(ExecutionContext context) throws CommandException;

}
