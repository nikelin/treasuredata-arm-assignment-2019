package co.jp.treasuredata.armtd.api.protocol.io;

import static org.junit.Assert.*;

import co.jp.treasuredata.armtd.api.protocol.Packet;
import co.jp.treasuredata.armtd.api.protocol.handler.PacketHandler;
import co.jp.treasuredata.armtd.api.protocol.handler.TDPacketHandler;
import co.jp.treasuredata.armtd.api.protocol.io.impl.StandardPacketsBuilder;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

public class FrameBuilderRunnableTest {

  private final PacketHandler packetHandler = new TDPacketHandler();

  @Test
  public void testFramesBuilderTerminatesCorrectly()
      throws InterruptedException, ExecutionException, TimeoutException {
    BlockingQueue<Pair<SocketChannel, ByteBuffer>> readBuffers = new LinkedBlockingQueue<>();
    BlockingQueue<Pair<SocketChannel, Packet>> packetsQueue = new LinkedBlockingQueue<>();
    AtomicBoolean terminatedSignal = new AtomicBoolean();

    ExecutorService executorService = Executors.newCachedThreadPool();

    FramesBuilderRunnable framesBuilderRunnable =
        new FramesBuilderRunnable(0, null, readBuffers, packetsQueue, terminatedSignal);
    Future<?> ready = executorService.submit(framesBuilderRunnable);
    terminatedSignal.set(true);
    ready.get(1, TimeUnit.SECONDS);
  }

  @Test
  public void testQueueWithSingleBufferAndMultiplePackets() throws InterruptedException {
    BlockingQueue<Pair<SocketChannel, ByteBuffer>> readBuffers = new LinkedBlockingQueue<>();
    BlockingQueue<Pair<SocketChannel, Packet>> packetsQueue = new LinkedBlockingQueue<>();
    AtomicBoolean terminatedSignal = new AtomicBoolean();

    ExecutorService executorService = Executors.newCachedThreadPool();

    Packet packet1 = new Packet(0, "test test".getBytes());
    Packet packet2 = new Packet(1, "test test 1".getBytes());
    Packet packet3 = new Packet(2, "test test 2".getBytes());
    ByteBuffer buffer =
        ByteBuffer.allocate(packet1.byteSize() + packet2.byteSize() + packet3.byteSize());

    packetHandler.serialise(buffer, packet1);
    packetHandler.serialise(buffer, packet2);
    packetHandler.serialise(buffer, packet3);

    buffer.flip();

    readBuffers.add(Pair.of(null, buffer));

    FramesBuilderRunnable framesBuilderRunnable =
        new FramesBuilderRunnable(
            0, new StandardPacketsBuilder(1024), readBuffers, packetsQueue, terminatedSignal);
    executorService.execute(framesBuilderRunnable);

    for (int i = 0; i < 3; i++) {
      Pair<SocketChannel, Packet> result = packetsQueue.poll(1, TimeUnit.SECONDS);
      if (result == null) {
        fail("result is null");
      }

      if (result.getRight().getToken() == packet1.getToken()) {
        assertEquals(result.getRight().getLength(), packet1.getLength());
        assertEquals(result.getRight().getToken(), packet1.getToken());
        assertArrayEquals(result.getRight().getData(), packet1.getData());
      } else if (result.getRight().getToken() == packet2.getToken()) {
        assertEquals(result.getRight().getLength(), packet2.getLength());
        assertEquals(result.getRight().getToken(), packet2.getToken());
        assertArrayEquals(result.getRight().getData(), packet2.getData());
      } else if (result.getRight().getToken() == packet3.getToken()) {
        assertEquals(result.getRight().getLength(), packet3.getLength());
        assertEquals(result.getRight().getToken(), packet3.getToken());
        assertArrayEquals(result.getRight().getData(), packet3.getData());
      }
    }

    terminatedSignal.set(true);
  }

  @Test
  public void testQueueWithSplitDataBuffers() throws InterruptedException {
    BlockingQueue<Pair<SocketChannel, ByteBuffer>> readBuffers = new LinkedBlockingQueue<>();
    BlockingQueue<Pair<SocketChannel, Packet>> packetsQueue = new LinkedBlockingQueue<>();
    AtomicBoolean terminatedSignal = new AtomicBoolean();

    ExecutorService executorService = Executors.newCachedThreadPool();

    Packet packet = new Packet(0, "test test".getBytes());
    ByteBuffer buffer1 = ByteBuffer.allocate(Integer.BYTES);
    buffer1.putInt(packet.getLength());
    buffer1.flip();
    ByteBuffer buffer2 = ByteBuffer.allocate(Integer.BYTES + packet.getLength());
    buffer2.putInt(packet.getToken());
    buffer2.put(packet.getData());
    buffer2.flip();

    readBuffers.add(Pair.of(null, buffer1));
    readBuffers.add(Pair.of(null, buffer2));

    FramesBuilderRunnable framesBuilderRunnable =
        new FramesBuilderRunnable(
            0, new StandardPacketsBuilder(1024), readBuffers, packetsQueue, terminatedSignal);
    executorService.execute(framesBuilderRunnable);

    Pair<SocketChannel, Packet> result = packetsQueue.poll(1, TimeUnit.SECONDS);
    if (result == null) {
      fail("result is null");
    }

    assertEquals(packet.getToken(), result.getRight().getToken());
    assertEquals(packet.getLength(), result.getRight().getLength());
    assertArrayEquals(packet.getData(), result.getRight().getData());

    terminatedSignal.set(true);
  }
}
