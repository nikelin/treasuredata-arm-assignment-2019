package co.jp.treasuredata.armtd.server.io.spi;

import co.jp.treasuredata.armtd.server.server.commands.RouteHandler;
import co.jp.treasuredata.armtd.server.io.Server;
import co.jp.treasuredata.armtd.server.io.impl.NonBlockingServer;
import co.jp.treasuredata.armtd.api.protocol.handler.PacketHandler;

import java.io.IOException;

public class NonBlockingServerProvider implements ServerProvider {

    @Override
    public Server provide(PacketHandler packetHandler, RouteHandler routeHandler) throws IOException {
        return new NonBlockingServer(routeHandler, packetHandler);
    }

}
