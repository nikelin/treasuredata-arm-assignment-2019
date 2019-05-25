package co.jp.treasuredata.armtd.server.server.commands.impl;

import co.jp.treasuredata.armtd.api.protocol.Request;
import co.jp.treasuredata.armtd.server.io.ResponseAction;
import co.jp.treasuredata.armtd.server.io.Server;
import co.jp.treasuredata.armtd.server.server.commands.HandlerException;
import co.jp.treasuredata.armtd.server.server.commands.RouteHandler;
import co.jp.treasuredata.armtd.server.server.commands.actions.QuitResponseAction;
import co.jp.treasuredata.armtd.server.server.commands.actions.TextResponseAction;
import co.jp.treasuredata.armtd.server.server.commands.discovery.DiscoverableHandler;

import java.util.Arrays;
import java.util.List;

@DiscoverableHandler(mappings = {"disconnect"}, description = "Command to close an active client session")
public class DisconnectRouteHandler implements RouteHandler {

    @Override
    public List<ResponseAction> handleRequest(Server server, Request request) throws HandlerException {
        return Arrays.asList(
            new TextResponseAction("-- Goodbye!"),
            new QuitResponseAction()
        );
    }
}
