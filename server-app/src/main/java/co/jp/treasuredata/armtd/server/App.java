package co.jp.treasuredata.armtd.server;

import co.jp.treasuredata.armtd.api.protocol.handler.PacketHandler;
import co.jp.treasuredata.armtd.api.protocol.handler.spi.PacketHandlerProvider;
import co.jp.treasuredata.armtd.server.io.Server;
import co.jp.treasuredata.armtd.server.io.spi.ServerProvider;
import co.jp.treasuredata.armtd.server.server.commands.DiscoveryRoutingRouteHandler;
import co.jp.treasuredata.armtd.server.server.commands.discovery.DiscovererException;
import co.jp.treasuredata.armtd.server.server.commands.discovery.PropertiesBasedHandlersDiscoverer;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Optional;
import java.util.Properties;
import java.util.ServiceLoader;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {
  private static Logger logger = LoggerFactory.getLogger(App.class);

  public static void main(String[] args) {
    ServerConfig serverConfig = new ServerConfig();
    final CmdLineParser parser = new CmdLineParser(serverConfig);

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

    InputStream settingsStream = App.class.getResourceAsStream("application.properties");
    if (settingsStream == null) {
      logger.error(
          "ERROR: Application properties is not present on the classpath (classpath:application.properties)");
      exit(-1);
      return;
    }

    final Properties properties = new Properties();
    try {
      properties.load(settingsStream);
    } catch (IOException e) {
      logger.error(
          "ERROR: Unable to load application properties file (classpath:application.properties)");
      exit(-1);
      return;
    }

    logger.info("ARM TD - Network File Server");

    final Optional<PacketHandlerProvider> packetHandlerOpt =
        resolveServiceProvider(PacketHandlerProvider.class);
    if (!packetHandlerOpt.isPresent()) {
      logger.error("ERROR: Unable to resolve an instance of PacketHandlerProvider via SPI");
      exit(-1);
      return;
    }

    final PacketHandler packetHandler = packetHandlerOpt.get().provide();

    final Optional<ServerProvider> serverProviderOpt = resolveServiceProvider(ServerProvider.class);
    if (!serverProviderOpt.isPresent()) {
      logger.error("ERROR: Unable to resolve an instance of ServerProvider via SPI");
      exit(-1);
      return;
    }

    final Server server;
    try {
      server =
          serverProviderOpt
              .get()
              .provide(
                  packetHandler,
                  new DiscoveryRoutingRouteHandler(
                      new PropertiesBasedHandlersDiscoverer(serverConfig, properties)
                          .resolveHandlers()));
    } catch (DiscovererException | IOException e) {
      logger.error("ERROR: Server initialisation failed", e);
      exit(-1);
      return;
    }

    Thread serverThread =
        new Thread(
            () -> {
              try {
                server.start(serverConfig.getHost(), serverConfig.getPort());
              } catch (IOException e) {
                logger.error("ERROR: Failed to start server on port " + serverConfig.getPort(), e);
              } catch (InterruptedException e) {
                logger.error("ERROR: Server thread has been interrupted", e);
              }
            });

    serverThread.start();

    try {
      server.awaitTermination();
    } catch (InterruptedException ignored) {
    }

    exit(0);
  }

  private static <T> Optional<T> resolveServiceProvider(Class<T> serviceClass) {
    ServiceLoader<T> loader = ServiceLoader.load(serviceClass);
    Iterator<T> providers = loader.iterator();
    if (!providers.hasNext()) return Optional.empty();

    return Optional.of(providers.next());
  }

  private static void exit(int status) {
    logger.error("Server stopped.");
    System.exit(status);
  }
}
