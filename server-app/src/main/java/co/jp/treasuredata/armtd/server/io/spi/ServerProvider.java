package co.jp.treasuredata.armtd.server.io.spi;

import co.jp.treasuredata.armtd.api.protocol.handler.PacketHandler;
import co.jp.treasuredata.armtd.server.io.Server;
import co.jp.treasuredata.armtd.server.server.commands.RouteHandler;
import java.io.IOException;

public interface ServerProvider {
  Server provide(PacketHandler packetHandler, RouteHandler routeHandler) throws IOException;
}
