package co.jp.treasuredata.armtd.client.commands.loader.spi;

import co.jp.treasuredata.armtd.client.commands.loader.CommandsLoader;
import co.jp.treasuredata.armtd.client.commands.loader.PropertiesBasedCommandsLoader;

public class DefaultCommandsLoaderProvider implements CommandsLoaderProvider {

  @Override
  public CommandsLoader provide() {
    return new PropertiesBasedCommandsLoader();
  }
}
