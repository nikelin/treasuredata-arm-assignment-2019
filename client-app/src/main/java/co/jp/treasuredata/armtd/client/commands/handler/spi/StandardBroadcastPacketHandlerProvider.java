package co.jp.treasuredata.armtd.client.commands.handler.spi;

import co.jp.treasuredata.armtd.client.commands.BroadcastPacketHandler;
import co.jp.treasuredata.armtd.client.commands.StandardBroadcastPacketHandler;

public class StandardBroadcastPacketHandlerProvider implements BroadcastPacketHandlerProvider {

    @Override
    public BroadcastPacketHandler provide() {
        return new StandardBroadcastPacketHandler();
    }
}
