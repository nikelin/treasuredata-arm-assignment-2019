package co.jp.treasuredata.armtd.client;

import co.jp.treasuredata.armtd.api.protocol.handler.PacketHandler;
import co.jp.treasuredata.armtd.api.protocol.handler.spi.PacketHandlerProvider;
import co.jp.treasuredata.armtd.client.api.TDServerConnector;
import co.jp.treasuredata.armtd.client.api.impl.AsyncTDServerConnector;
import co.jp.treasuredata.armtd.client.commands.CommandHandler;
import co.jp.treasuredata.armtd.client.commands.CommandException;
import co.jp.treasuredata.armtd.client.commands.loader.spi.CommandsLoaderProvider;
import org.kohsuke.args4j.*;

import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

public final class App {

    public static void main(String[] args) {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        ClientConfig config = new ClientConfig();
        final CmdLineParser parser = new CmdLineParser(config);

        if (args.length == 0) {
            parser.printUsage(System.out);
            exit(-1);
            return;
        }

        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            e.printStackTrace(System.err);
            exit(-1);
            return;
        }

        Iterator<PacketHandlerProvider> packetHandlerIterator = ServiceLoader.load(PacketHandlerProvider.class).iterator();
        if (!packetHandlerIterator.hasNext()) {
            System.out.println("[ERROR] No packet loaders provided!");
            exit(-1);
            return;
        }

        final TDServerConnector connector;
        try {
            connector = new AsyncTDServerConnector(packetHandlerIterator.next().provide(),
                    config.getServerHost(), config.getServerPort());
        } catch (Throwable e) {
            System.out.println("[ERROR] Failed to instantiate an instance of TD File Server connector.");
            e.printStackTrace();
            exit(-1);
            return;
        }

        try {
            connector.connect();
        } catch (Throwable e) {
            System.out.println("[ERROR] Failed to establish connection to the TD File Server endpoint.");
            exit(-1);
            return;
        }

        Iterator<CommandsLoaderProvider> providedLoaders = ServiceLoader.load(CommandsLoaderProvider.class).iterator();
        if (!providedLoaders.hasNext()) {
            System.out.println("[ERROR] No command loaders provided!");
            exit(-1);
            return;
        }

        try {
            handleInput(new ExecutionContext(config, connector, in, System.out), providedLoaders.next().provide().load());
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("I/O operation failed");
            exit(-1);
            return;
        }

        exit(0);
    }

    private static void handleInput(ExecutionContext context, List<CommandHandler> commandHandlers) throws IOException {
        boolean isStopped = false;
        while(!isStopped) {
            context.out().println("== Pick one of the available command handlers:");
            for (CommandHandler handler : commandHandlers) {
                context.out().println("* type '" + handler.getKey() + "' to execute [" + handler.toString() + "]");
            }

            context.out().println("[: q or quit to stop]");
            context.out().println("[: qc to disconnect]");

            final String line = context.in().readLine();
            if (line == null) {
                continue;
            }

            if (line.equals("qc")) {
                try {
                    context.getConnector().disconnect();
                } catch (Throwable e) {
                    context.out().println("[ERROR] Failed to close connection");
                }
                continue;
            }

            if (line.equals("q") || line.equals("quit")) {
                isStopped = true;
                continue;
            }

            Optional<CommandHandler> command = commandHandlers.stream().filter((v) -> v.getKey().contains(line)).findFirst();
            if (!command.isPresent()) {
                context.out().println("[ERROR] Unrecognised command");
                continue;
            }

            try {
                context.out().println("Command execution has started");
                command.get().execute(context);
            } catch (CommandException e) {
                e.printStackTrace(context.out());
                context.out().println("[ERROR] CommandHandler execution failed: " + e.getMessage());
            }
        }
    }

    private static void exit(int status) {
        System.out.println("Exiting.");
        System.exit(status);
    }

}
