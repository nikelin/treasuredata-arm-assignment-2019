package co.jp.treasuredata.armtd.api.protocol.io;

import co.jp.treasuredata.armtd.api.protocol.Packet;
import java.nio.ByteBuffer;
import org.apache.commons.lang3.tuple.Pair;

public interface PacketsBuilder {

  void dispose();

  Pair<Packet, Integer> consume(ByteBuffer buffer);
}
