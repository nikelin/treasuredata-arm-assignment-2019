package co.jp.treasuredata.armtd.client.commands.loader.spi;

import co.jp.treasuredata.armtd.client.ExecutionContext;
import co.jp.treasuredata.armtd.client.commands.loader.CommandsLoader;

import java.io.IOException;
import java.util.List;

public interface CommandsLoaderProvider {

    CommandsLoader provide();

}
