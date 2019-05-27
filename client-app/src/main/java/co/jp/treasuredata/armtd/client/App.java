package co.jp.treasuredata.armtd.client;

import co.jp.treasuredata.armtd.api.protocol.handler.PacketHandler;
import co.jp.treasuredata.armtd.api.protocol.handler.spi.PacketHandlerProvider;
import co.jp.treasuredata.armtd.client.api.TDServerConnector;
import co.jp.treasuredata.armtd.client.api.impl.NonBlockingTDServerConnector;
import co.jp.treasuredata.armtd.client.commands.BroadcastPacketHandler;
import co.jp.treasuredata.armtd.client.commands.CommandException;
import co.jp.treasuredata.armtd.client.commands.CommandHandler;
import co.jp.treasuredata.armtd.client.commands.handler.spi.BroadcastPacketHandlerProvider;
import co.jp.treasuredata.armtd.client.commands.loader.spi.CommandsLoaderProvider;
import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import org.kohsuke.args4j.*;

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

    Optional<PacketHandlerProvider> packetHandlerOpt =
        resolveServiceProvider(PacketHandlerProvider.class);
    if (!packetHandlerOpt.isPresent()) {
      System.err.println("[ERROR] No packet loaders provided!");
      exit(-1);
      return;
    }

    final PacketHandler packetHandler = packetHandlerOpt.get().provide();

    final BroadcastPacketHandler broadcastPacketHandler =
        resolveServiceProvider(BroadcastPacketHandlerProvider.class)
            .map(BroadcastPacketHandlerProvider::provide)
            .orElseGet(() -> null);

    final TDServerConnector connector;
    try {
      connector =
          new NonBlockingTDServerConnector(
              System.out, packetHandler, broadcastPacketHandler, config);
    } catch (Throwable e) {
      System.err.println("[ERROR] Failed to instantiate an instance of TD File Server connector.");
      e.printStackTrace();
      exit(-1);
      return;
    }

    Iterator<CommandsLoaderProvider> providedLoaders =
        ServiceLoader.load(CommandsLoaderProvider.class).iterator();
    if (!providedLoaders.hasNext()) {
      System.err.println("[ERROR] No command loaders provided!");
      exit(-1);
      return;
    }

    try {
      handleInput(
          new ExecutionContext(config, connector, in, System.out),
          providedLoaders.next().provide().load());
    } catch (IOException e) {
      System.err.println("I/O operation failed: " + e.getMessage());
      exit(-1);
      return;
    }

    exit(0);
  }

  private static <T> Optional<T> resolveServiceProvider(Class<T> serviceClass) {
    ServiceLoader<T> loader = ServiceLoader.load(serviceClass);
    Iterator<T> providers = loader.iterator();
    if (!providers.hasNext()) return Optional.empty();

    return Optional.of(providers.next());
  }

  private static void handleInput(ExecutionContext context, List<CommandHandler> commandHandlers)
      throws IOException {
    boolean isStopped = false;
    while (!isStopped) {
      context.out().println("== Pick one of the available command handlers:");

      for (CommandHandler handler : commandHandlers) {
        context
            .out()
            .println("* type '" + handler.getKey() + "' to execute [" + handler.toString() + "]");
      }

      context.out().println("[: q or quit to stop]");
      context.out().println("[: qc to disconnect]");

      final String line = context.in().readLine();
      if (line == null) {
        continue;
      }

      if (line.equals("qc")) {
        try {
          context.getConnector().close();
          context.getConnector().connect();
        } catch (Throwable e) {
          context.out().println("[ERROR] Failed to close connection");
        }
        continue;
      }

      if (line.equals("q") || line.equals("quit")) {
        isStopped = true;
        continue;
      }

      Optional<CommandHandler> command =
          commandHandlers.stream().filter((v) -> v.getKey().contains(line)).findFirst();
      if (!command.isPresent()) {
        context.out().println(">>> Unrecognised command");
        continue;
      }

      boolean retryRequested = false;
      do {
        boolean failed = false;

        try {
          if (!context.getConnector().isConnected()) {
            try {
              context.getConnector().connect();
            } catch (Throwable e) {
              if (context.getClientConfig().getVerbose()) {
                context
                    .out()
                    .println(
                        "[error] Unable to establish connection to the TD Server: "
                            + e.getMessage());
              }
            }
          }

          context.out().println(">>> Command execution started");
          command.get().execute(context);
        } catch (CommandException e) {
          context.out().println("[ERROR] CommandHandler execution failed: " + e.getMessage());
          failed = true;
        }

        if (failed) {
          context.out().println(">>> Command execution has failed, do you want to retry? [y/n]");

          String input = context.in().readLine();
          if (input == null) {
            break;
          }

          switch (input) {
            case "y":
              retryRequested = true;
              break;
            case "n":
              retryRequested = false;
              break;
            default:
              context
                  .out()
                  .println(
                      "[error] Unrecognised input, returning to the commands selection screen.");
          }
        }
      } while (retryRequested);
    }
  }

  private static void exit(int status) {
    System.out.println("Exiting.");
    System.exit(status);
  }
}
