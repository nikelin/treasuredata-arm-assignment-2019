package co.jp.treasuredata.armtd.api.protocol.handler;

import co.jp.treasuredata.armtd.api.protocol.Packet;
import co.jp.treasuredata.armtd.api.protocol.Request;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Optional;

public class TDPacketHandler implements PacketHandler {

  @Override
  public void serialise(ByteBuffer buffer, Packet packet) {
    if (buffer.remaining() < packet.byteSize()) {
      throw new IllegalArgumentException("buffer.remaining < packet.byteSize");
    }

    buffer.putInt(packet.getLength());
    buffer.putInt(packet.getToken());
    buffer.put(packet.getData());
  }

  @Override
  public Optional<Request> parse(Packet packet) {
    if (packet.getData().length == 0) return Optional.empty();

    String result = new String(packet.getData(), 0, packet.getLength()).trim();
    if (result.isEmpty()) return Optional.empty();

    String[] requestParts = result.split(" ");

    return Optional.of(
        new Request(
            packet.getToken(),
            requestParts[0].trim(),
            Arrays.copyOfRange(requestParts, 1, requestParts.length)));
  }
}
