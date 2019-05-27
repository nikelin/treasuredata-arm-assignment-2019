package co.jp.treasuredata.armtd.server.io.impl;

import co.jp.treasuredata.armtd.api.protocol.Packet;
import co.jp.treasuredata.armtd.api.protocol.Request;
import co.jp.treasuredata.armtd.api.protocol.handler.PacketHandler;
import co.jp.treasuredata.armtd.api.protocol.io.FramesBuilderRunnable;
import co.jp.treasuredata.armtd.api.protocol.io.impl.StandardPacketsBuilder;
import co.jp.treasuredata.armtd.server.io.ResponseAction;
import co.jp.treasuredata.armtd.server.io.Server;
import co.jp.treasuredata.armtd.server.server.commands.HandlerException;
import co.jp.treasuredata.armtd.server.server.commands.RouteHandler;
import co.jp.treasuredata.armtd.server.server.commands.actions.ErrorResponseAction;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NonBlockingServer implements Server {
  private static final Logger logger = LoggerFactory.getLogger(NonBlockingServer.class);

  private final AtomicBoolean isTerminated = new AtomicBoolean();
  private final Object terminated = new Object();
  private final Object started = new Object();

  private final RouteHandler routeHandler;
  private final PacketHandler packetHandler;

  private final ExecutorService service = Executors.newCachedThreadPool();

  private final Set<SocketChannel> activeConnections = Collections.synchronizedSet(new HashSet<>());
  private final BlockingQueue<Pair<SocketChannel, ByteBuffer>> readBuffers =
      new LinkedBlockingQueue<>();
  private final BlockingQueue<Pair<SocketChannel, Packet>> inPacketsQueue =
      new LinkedBlockingQueue<>();
  private final BlockingQueue<Pair<SocketChannel, List<Packet>>> outPacketsQueue =
      new LinkedBlockingQueue<>();

  private final Duration writeTimeout = Duration.of(2, ChronoUnit.SECONDS);

  public NonBlockingServer(RouteHandler routeHandler, PacketHandler packetHandler) {
    this.routeHandler = routeHandler;
    this.packetHandler = packetHandler;
  }

  @Override
  public void start(String host, int port) throws IOException {
    Selector selector = Selector.open();

    ServerSocketChannel serverChannel = ServerSocketChannel.open();
    serverChannel.bind(
        host != null ? new InetSocketAddress(host, port) : new InetSocketAddress(port));
    serverChannel.configureBlocking(false);

    serverChannel.register(selector, serverChannel.validOps(), null);

    synchronized (this.started) {
      this.started.notifyAll();
    }

    for (int builderIdx = 0; builderIdx < 3; builderIdx++) {
      service.execute(
          new FramesBuilderRunnable(
              builderIdx,
              new StandardPacketsBuilder(2048),
              readBuffers,
              inPacketsQueue,
              isTerminated));
    }

    for (int processorIdx = 0; processorIdx < 3; processorIdx++) {
      service.execute(new PacketsProcessorRunnable(processorIdx));
    }

    for (int processorIdx = 0; processorIdx < 1; processorIdx++) {
      service.execute(new PacketsWriterRunnable());
    }

    while (!this.isTerminated.get()) {
      selector.select();

      Set<SelectionKey> selectedKeys = selector.selectedKeys();
      Iterator<SelectionKey> iterator = selectedKeys.iterator();
      while (iterator.hasNext()) {
        SelectionKey selectedKey = iterator.next();

        iterator.remove();

        if (!selectedKey.isValid()) {
          try {
            selectedKey.channel().close();
          } catch (IOException e) {
            logger.error("Can't close connection: " + e.getMessage(), e);
            continue;
          }
        }

        if (selectedKey.isAcceptable()) {
          SocketChannel clientSocket = serverChannel.accept();
          clientSocket.configureBlocking(false);
          clientSocket.register(selector, SelectionKey.OP_READ);
          activeConnections.add(clientSocket);
        } else if (selectedKey.isReadable()) {
          try {
            readChannel(
                selectedKey,
                (v) -> {
                  selectedKey.cancel();
                  closeChannel((SocketChannel) selectedKey.channel());
                  return null;
                });
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
    }

    serverChannel.close();
    selector.close();
  }

  protected void broadcast(Packet packet) {
    for (SocketChannel connection : activeConnections) {
      this.outPacketsQueue.add(Pair.of(connection, Collections.singletonList(packet)));
    }
  }

  private boolean isActiveConnection(SocketChannel channel) {
    return this.activeConnections.contains(channel);
  }

  private void readChannel(SelectionKey key, Function<Void, Void> onClose) throws IOException {
    SocketChannel channel = (SocketChannel) key.channel();

    ByteBuffer buffer = ByteBuffer.allocate(512);
    int bytesRead = channel.read(buffer);
    if (bytesRead == -1) {
      onClose.apply(null);
    } else {
      buffer.flip();

      ByteBuffer inputBuffer = ByteBuffer.allocate(bytesRead);
      inputBuffer.put(buffer.array(), 0, bytesRead);
      inputBuffer.flip();
      buffer.clear();
      readBuffers.add(Pair.of(channel, inputBuffer));
    }

    buffer.clear();
  }

  private void closeChannel(SocketChannel channel) {
    this.activeConnections.remove(channel);
    this.inPacketsQueue.removeIf((pair) -> pair.getLeft() == channel);
    this.outPacketsQueue.removeIf((pair) -> pair.getLeft() == channel);
    this.readBuffers.removeIf((pair) -> pair.getLeft() == channel);
  }

  @Override
  public void stop() {
    this.broadcast(Packet.broadcast("[server status] Server has stopped.".getBytes()));

    Future<?> delayedTermination =
        this.service.submit(
            new Runnable() {
              @Override
              public void run() {
                try {
                  Thread.sleep(2000);
                } catch (Throwable ignored) {
                  return;
                }

                NonBlockingServer.this.isTerminated.set(true);
                synchronized (NonBlockingServer.this.terminated) {
                  NonBlockingServer.this.terminated.notifyAll();
                }
              }
            });

    try {
      delayedTermination.get(1, TimeUnit.SECONDS);
    } catch (Throwable ignored) {
    }
  }

  @Override
  public void awaitStart() throws InterruptedException {
    synchronized (this.started) {
      this.started.wait();
    }
  }

  @Override
  public void awaitTermination() throws InterruptedException {
    synchronized (this.terminated) {
      this.terminated.wait();
    }
  }

  private class PacketsWriterRunnable implements Runnable {
    @Override
    public void run() {
      while (!NonBlockingServer.this.isTerminated.get()) {
        final Pair<SocketChannel, List<Packet>> outPair;
        try {
          outPair = outPacketsQueue.poll(1, TimeUnit.SECONDS);
        } catch (Throwable ignore) {
          continue;
        }

        if (outPair == null) continue;

        SocketChannel channel = outPair.getLeft();

        if (!isActiveConnection(channel)) continue;

        logger.info("[output] Response packets produced - " + outPair.getRight().size());
        for (int i = 0; i < outPair.getRight().size(); i++) {
          Packet packet = outPair.getRight().get(i);
          ByteBuffer buffer = ByteBuffer.allocate(outPair.getRight().get(i).byteSize());
          packetHandler.serialise(buffer, packet);
          buffer.flip();

          try {
            logger.debug(
                "[output] Sending response ("
                    + packet.byteSize()
                    + " bytes) "
                    + " - "
                    + (i + 1)
                    + " / "
                    + outPair.getRight().size());
            while (buffer.hasRemaining()) {
              channel.write(buffer);
            }
            logger.debug("[output] Response sent - " + (i + 1) + " / " + outPair.getRight().size());
          } catch (IOException e) {
            logger.error("[ERROR] I/O failed", e);
          }

          buffer.clear();
        }
      }
    }
  }

  private class PacketsProcessorRunnable implements Runnable {
    private final int idx;

    PacketsProcessorRunnable(int idx) {
      this.idx = idx;
    }

    @Override
    public void run() {
      logger.info("= Packets processor #" + idx);

      while (!NonBlockingServer.this.isTerminated.get()) {
        final Pair<SocketChannel, Packet> inputPair;
        try {
          inputPair = NonBlockingServer.this.inPacketsQueue.poll(1, TimeUnit.SECONDS);
        } catch (Throwable e) {
          continue;
        }

        if (inputPair == null || !isActiveConnection(inputPair.getLeft())) continue;

        logger.info("[input] Request received - " + inputPair);

        Optional<Request> request =
            NonBlockingServer.this.packetHandler.parse(inputPair.getRight());

        CompletableFuture<List<Packet>> future;
        if (!request.isPresent()) {
          logger.info("[input] Request processing failed - " + inputPair);
          future = ErrorResponseAction.corruptedRequest().execute(null);
        } else {
          try {
            future = CompletableFuture.supplyAsync(ArrayList::new);
            logger.info("[input] Routing request - " + inputPair);
            for (ResponseAction action :
                routeHandler.handleRequest(NonBlockingServer.this, request.get())) {
              future =
                  future.thenCompose(
                      (list) ->
                          action
                              .execute(request.get())
                              .thenApply(
                                  (v) -> {
                                    list.addAll(v);
                                    return list;
                                  }));
            }
          } catch (HandlerException e) {
            logger.error("[input] Handler failed to process the request", e);
            future = ErrorResponseAction.internalException(e.getMessage()).execute(request.get());
          }
        }

        try {
          List<Packet> outPackets = future.get(writeTimeout.toMillis(), TimeUnit.MILLISECONDS);
          if (outPackets.isEmpty()) continue;

          outPacketsQueue.add(Pair.of(inputPair.getLeft(), outPackets));
        } catch (Throwable e) {
          logger.error("[error] Request processing failed due to timeout", e);
        }
      }
    }
  }
}
