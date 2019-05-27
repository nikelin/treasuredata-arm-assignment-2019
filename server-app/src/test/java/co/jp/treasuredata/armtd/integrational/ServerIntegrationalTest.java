package co.jp.treasuredata.armtd.integrational;

import static org.junit.Assert.*;

import co.jp.treasuredata.armtd.api.protocol.Packet;
import co.jp.treasuredata.armtd.api.protocol.Request;
import co.jp.treasuredata.armtd.api.protocol.handler.TDPacketHandler;
import co.jp.treasuredata.armtd.server.ServerConfig;
import co.jp.treasuredata.armtd.server.io.ResponseAction;
import co.jp.treasuredata.armtd.server.io.Server;
import co.jp.treasuredata.armtd.server.io.impl.NonBlockingServer;
import co.jp.treasuredata.armtd.server.server.commands.RouteHandler;
import co.jp.treasuredata.armtd.server.server.commands.actions.TextResponseAction;
import co.jp.treasuredata.armtd.server.server.commands.impl.GetRouteHandler;
import co.jp.treasuredata.armtd.utils.SocketUtils;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.Test;

public class ServerIntegrationalTest {

  private final ExecutorService service = Executors.newCachedThreadPool();
  private final TDPacketHandler packetHandler = new TDPacketHandler();

  @Test
  public void testSingleConnectionBasicRequest() throws Throwable {
    final int serverPort = SocketUtils.nextFreePort();
    String checkPayload = "single connection request";
    CountDownLatch latch = new CountDownLatch(1000);
    NonBlockingServer server =
        new NonBlockingServer(new DummyRouteHandler(checkPayload, latch), packetHandler);

    service.execute(
        () -> {
          try {
            server.start("0.0.0.0", serverPort);
          } catch (Throwable e) {
            fail();
          }
        });

    server.awaitStart();

    SocketChannel socket = SocketChannel.open();
    socket.connect(new InetSocketAddress(serverPort));

    List<String> results = new ArrayList<>();
    for (int i = 0; i < 1000; i++) {
      results.addAll(
          executeConnection(
                  i,
                  1,
                  serverPort,
                  Collections.singletonList(128),
                  (idx) -> new Packet(idx, (checkPayload + " " + idx).getBytes()),
                  readSingleBuffer)
              .stream()
              .map((v) -> new String(v.getData()))
              .collect(Collectors.toList()));
    }

    assertEquals(1000, results.size());

    for (int i = 1; i < 1001; i++) {
      assertTrue(
          "results contain a correct value for #" + i, results.contains(checkPayload + " " + i));
    }

    server.stop();
  }

  @Test
  public void testSingleConnectionGetRoute() throws Throwable {
    File testDirectory = new File("./test-directory-" + Math.random());
    testDirectory.mkdir();
    testDirectory.deleteOnExit();

    File testFile = new File(testDirectory, "test-file-" + Math.random());
    testFile.createNewFile();
    testFile.deleteOnExit();

    String fileTestData = "test data" + Math.random();

    BufferedWriter writer =
        new BufferedWriter(new OutputStreamWriter(new FileOutputStream(testFile)));
    writer.write(fileTestData);
    writer.close();

    final int serverPort = SocketUtils.nextFreePort();
    String checkPayload = "get " + testFile.getName();

    ServerConfig config = new ServerConfig(testDirectory.getAbsolutePath(), "0.0.0.0", serverPort);

    NonBlockingServer server = new NonBlockingServer(new GetRouteHandler(config), packetHandler);

    service.execute(
        () -> {
          try {
            server.start("0.0.0.0", serverPort);
          } catch (Throwable e) {
            fail();
          }
        });

    server.awaitStart();

    SocketChannel socket = SocketChannel.open();
    socket.connect(new InetSocketAddress(serverPort));

    List<String> results = new ArrayList<>();
    for (int i = 0; i < 1000; i++) {
      results.addAll(
          executeConnection(
                  i,
                  1,
                  serverPort,
                  Arrays.asList(10, 36),
                  (idx) -> new Packet(idx, checkPayload.getBytes()),
                  readTwoBuffers)
              .stream()
              .map((v) -> new String(v.getData()))
              .collect(Collectors.toList()));
    }

    assertEquals(2000, results.size());

    assertEquals("not enough oks", 1000, Collections.frequency(results, "ok"));
    assertEquals("not enough payloads", 1000, Collections.frequency(results, fileTestData));

    server.stop();
  }

  @Test
  public void testMultipleConnectionBasicRequest() throws Throwable {
    CountDownLatch latch = new CountDownLatch(1000);

    final int serverPort = SocketUtils.nextFreePort();
    String checkPayload = "single connection request";

    NonBlockingServer server =
        new NonBlockingServer(new DummyRouteHandler(checkPayload, latch), packetHandler);

    service.execute(
        () -> {
          try {
            server.start("0.0.0.0", serverPort);
          } catch (Throwable e) {
            fail();
          }
        });

    server.awaitStart();

    final List<String> results = new ArrayList<>();
    final AtomicInteger connectionId = new AtomicInteger();
    for (int connection = 1; connection < 101; connection++) {
      Future<List<String>> future =
          service.submit(
              () -> {
                try {
                  return executeConnection(
                          connectionId.getAndIncrement(),
                          100,
                          serverPort,
                          Collections.singletonList(128),
                          (i) -> new Packet(i, (checkPayload + " " + i).getBytes()),
                          readSingleBuffer)
                      .stream()
                      .map((v) -> new String(v.getData()))
                      .collect(Collectors.toList());
                } catch (Throwable e) {
                  fail(e.getMessage());
                  return Collections.emptyList();
                }
              });

      results.addAll(future.get());
    }

    latch.await();

    results.sort(Comparator.naturalOrder());

    for (int i = 1; i < connectionId.get(); i++) {
      assertTrue(results.contains(checkPayload + " " + i));
    }

    server.stop();
  }

  private List<Packet> executeConnection(
      int connectionId,
      int requestsCount,
      int serverPort,
      List<Integer> bufferSizes,
      Function<Integer, Packet> packetBuilder,
      BiFunction<SocketChannel, List<Integer>, List<Packet>> readPackets)
      throws IOException {
    SocketChannel socket = SocketChannel.open();
    socket.configureBlocking(true);
    socket.connect(new InetSocketAddress(serverPort));
    socket.finishConnect();

    ArrayList<Packet> responsePackets = new ArrayList<>();
    for (int j = 1; j < requestsCount + 1; j++) {
      Packet packet = packetBuilder.apply(connectionId * j + j);

      ByteBuffer buffer = ByteBuffer.allocate(packet.byteSize());
      packetHandler.serialise(buffer, packet);

      buffer.flip();
      while (buffer.hasRemaining()) {
        socket.write(buffer);
      }

      List<Packet> packets = readPackets.apply(socket, bufferSizes);

      responsePackets.addAll(packets);
    }

    return responsePackets;
  }

  public class DummyRouteHandler implements RouteHandler {
    private final String staticMessage;
    private final CountDownLatch latch;

    DummyRouteHandler(String staticMessage, CountDownLatch latch) {
      this.staticMessage = staticMessage;
      this.latch = latch;
    }

    @Override
    public List<ResponseAction> handleRequest(Server server, Request request) {
      return Collections.singletonList(
          new TextResponseAction(staticMessage + " " + request.getToken()) {
            @Override
            public CompletableFuture<List<Packet>> execute(Request request) {
              return super.execute(request)
                  .thenApply(
                      (v) -> {
                        latch.countDown();
                        return v;
                      });
            }
          });
    }
  }

  private final BiFunction<SocketChannel, List<Integer>, List<Packet>> readTwoBuffers =
      (socket, bufferSizes) -> {
        try {
          ByteBuffer buffer = ByteBuffer.allocate(bufferSizes.get(0));
          socket.read(buffer);
          buffer.flip();

          ByteBuffer secondBuffer = ByteBuffer.allocate(bufferSizes.get(1));
          socket.read(secondBuffer);
          secondBuffer.flip();

          int length = buffer.getInt();
          int token = buffer.getInt();
          byte[] data = new byte[length];
          buffer.get(data);
          buffer.clear();

          int dataLength = secondBuffer.getInt();
          int dataToken = secondBuffer.getInt();
          byte[] bodyData = new byte[dataLength];
          secondBuffer.get(bodyData);
          secondBuffer.clear();

          return Arrays.asList(new Packet(token, data), new Packet(dataToken, bodyData));
        } catch (Throwable e) {
          throw new RuntimeException(e);
        }
      };

  private final BiFunction<SocketChannel, List<Integer>, List<Packet>> readSingleBuffer =
      (socket, bufferSizes) -> {
        try {
          ByteBuffer buffer = ByteBuffer.allocate(bufferSizes.get(0));
          socket.read(buffer);
          buffer.flip();

          int length = buffer.getInt();
          int token = buffer.getInt();
          byte[] data = new byte[length];
          buffer.get(data);

          return Collections.singletonList(new Packet(token, data));
        } catch (Throwable e) {
          throw new RuntimeException(e);
        }
      };
}
