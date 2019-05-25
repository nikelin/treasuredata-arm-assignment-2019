package co.jp.treasuredata.armtd.client.commands.loader;

import co.jp.treasuredata.armtd.client.commands.CommandHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class PropertiesBasedCommandsLoader implements CommandsLoader {

    @Override
    public List<CommandHandler> load() throws IOException {
        Properties properties = new Properties();
        properties.load(this.getClass().getResourceAsStream("commands.properties"));

        List<CommandHandler> handlers = new ArrayList<>();
        for (String key : properties.stringPropertyNames()) {
            if (!key.startsWith("command.")) continue;

            String value = properties.getProperty(key);

            final Class<?> commandClass;
            try {
                commandClass = this.getClass().getClassLoader().loadClass(value);
            } catch (Throwable e) {
                throw new IOException("Failed to load command handler - " + value);
            }

            try {
                handlers.add((CommandHandler) commandClass.newInstance());
            } catch (Throwable e) {
                throw new IOException("Failed to instantiate command handler - " + value);
            }
        }

        return handlers;
    }
}
