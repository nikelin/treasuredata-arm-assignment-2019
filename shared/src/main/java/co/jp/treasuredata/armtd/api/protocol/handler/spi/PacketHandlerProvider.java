package co.jp.treasuredata.armtd.api.protocol.handler.spi;

import co.jp.treasuredata.armtd.api.protocol.handler.PacketHandler;

public interface PacketHandlerProvider {

    PacketHandler provide();

}
