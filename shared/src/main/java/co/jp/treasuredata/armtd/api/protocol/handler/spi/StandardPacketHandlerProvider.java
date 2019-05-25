package co.jp.treasuredata.armtd.api.protocol.handler.spi;

import co.jp.treasuredata.armtd.api.protocol.handler.PacketHandler;
import co.jp.treasuredata.armtd.api.protocol.handler.TDPacketHandler;

public class StandardPacketHandlerProvider implements PacketHandlerProvider {

    @Override
    public PacketHandler provide() {
        return new TDPacketHandler();
    }

}
