package co.jp.treasuredata.armtd.client.commands.loader;

import co.jp.treasuredata.armtd.client.commands.CommandHandler;

import java.io.IOException;
import java.util.List;

public interface CommandsLoader {

    List<CommandHandler> load() throws IOException;

}
