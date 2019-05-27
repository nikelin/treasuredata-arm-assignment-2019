package co.jp.treasuredata.armtd.api.protocol.handler;

import static org.junit.Assert.*;

import co.jp.treasuredata.armtd.api.protocol.Packet;
import co.jp.treasuredata.armtd.api.protocol.Request;
import java.nio.ByteBuffer;
import java.util.Optional;
import org.junit.Test;

public class TDPacketHandlerTest {

  private final PacketHandler packetHandler = new TDPacketHandler();

  @Test
  public void testPacketParse() {
    String checkPayload = "route part1 part2 part3";
    Packet packet = new Packet(209133, checkPayload.getBytes());

    ByteBuffer buffer = ByteBuffer.allocate(packet.byteSize());
    buffer.putInt(packet.getLength());
    buffer.putInt(packet.getToken());
    buffer.put(packet.getData());

    Optional<Request> request = packetHandler.parse(packet);

    assertTrue(request.isPresent());
    assertEquals(packet.getToken(), request.get().getToken());
    assertEquals("route", request.get().getTypeName());
    assertArrayEquals(new String[] {"part1", "part2", "part3"}, request.get().getArguments());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPacketSerialiseChecksSizeRequirements() {
    String checkPayload = "this is a test";
    Packet packet = new Packet(209133, checkPayload.getBytes());
    ByteBuffer buffer = ByteBuffer.allocate(packet.byteSize() - 1);
    packetHandler.serialise(buffer, packet);
  }

  @Test
  public void testPacketSerialise() {
    String checkPayload = "this is a test";
    Packet packet = new Packet(209133, checkPayload.getBytes());
    ByteBuffer buffer = ByteBuffer.allocate(packet.byteSize());
    packetHandler.serialise(buffer, packet);
    buffer.flip();
    assertEquals(packet.getLength(), buffer.getInt());
    assertEquals(packet.getToken(), buffer.getInt());

    byte[] data = new byte[packet.getLength()];
    buffer.get(data);

    assertArrayEquals(packet.getData(), data);
  }
}
