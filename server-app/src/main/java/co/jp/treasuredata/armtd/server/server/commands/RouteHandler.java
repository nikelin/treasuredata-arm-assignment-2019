package co.jp.treasuredata.armtd.server.server.commands;

import co.jp.treasuredata.armtd.api.protocol.Request;
import co.jp.treasuredata.armtd.server.io.ResponseAction;
import co.jp.treasuredata.armtd.server.io.Server;
import java.util.List;

public interface RouteHandler {

  List<ResponseAction> handleRequest(Server server, Request request) throws HandlerException;
}
