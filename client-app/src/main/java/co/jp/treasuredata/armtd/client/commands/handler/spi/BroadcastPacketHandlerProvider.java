package co.jp.treasuredata.armtd.client.commands.handler.spi;

import co.jp.treasuredata.armtd.client.commands.BroadcastPacketHandler;

public interface BroadcastPacketHandlerProvider {

    public BroadcastPacketHandler provide();

}
