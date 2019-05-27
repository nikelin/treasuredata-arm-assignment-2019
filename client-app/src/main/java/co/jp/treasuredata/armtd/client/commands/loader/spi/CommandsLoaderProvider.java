package co.jp.treasuredata.armtd.client.commands.loader.spi;

import co.jp.treasuredata.armtd.client.commands.loader.CommandsLoader;

public interface CommandsLoaderProvider {

  CommandsLoader provide();
}
