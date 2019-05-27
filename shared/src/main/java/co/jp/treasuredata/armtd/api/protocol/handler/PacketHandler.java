package co.jp.treasuredata.armtd.api.protocol.handler;

import co.jp.treasuredata.armtd.api.protocol.Packet;
import co.jp.treasuredata.armtd.api.protocol.Request;
import java.nio.ByteBuffer;
import java.util.Optional;

public interface PacketHandler {

  Optional<Request> parse(Packet packet);

  void serialise(ByteBuffer buffer, Packet packet);
}
