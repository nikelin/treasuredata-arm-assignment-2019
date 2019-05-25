package co.jp.treasuredata.armtd.api.protocol.io;

import co.jp.treasuredata.armtd.api.protocol.Packet;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.ByteBuffer;

public interface PacketsBuilder {

    void dispose();

    Pair<Packet, Integer> consume(ByteBuffer buffer);

}