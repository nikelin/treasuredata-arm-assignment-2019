package co.jp.treasuredata.armtd.server.server.commands.impl;

import co.jp.treasuredata.armtd.server.ServerConfig;
import co.jp.treasuredata.armtd.api.protocol.Request;
import co.jp.treasuredata.armtd.server.io.ResponseAction;
import co.jp.treasuredata.armtd.server.io.Server;
import co.jp.treasuredata.armtd.server.server.commands.HandlerException;
import co.jp.treasuredata.armtd.server.server.commands.RouteHandler;
import co.jp.treasuredata.armtd.server.server.commands.actions.ErrorResponseAction;
import co.jp.treasuredata.armtd.server.server.commands.actions.FileResponseAction;
import co.jp.treasuredata.armtd.server.server.commands.actions.TextResponseAction;
import co.jp.treasuredata.armtd.server.server.commands.discovery.DiscoverableHandler;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@DiscoverableHandler(mappings = {"get"}, description = "Command to fetch a given file from the root directory specified on startup")
public class GetRouteHandler implements RouteHandler {

    private final ServerConfig config;

    public GetRouteHandler(ServerConfig config) {
        this.config = config;
    }

    @Override
    public List<ResponseAction> handleRequest(Server server, Request request) throws HandlerException {
        String requestedFilePath = request.getArguments().length == 0 ? null : request.getArguments()[0];
        File file = new File(config.getInputDirectoryPath(), requestedFilePath);

        final List<ResponseAction> result;
        if (file.exists()) {
            result = Arrays.asList(
                new TextResponseAction("ok"),
                new FileResponseAction(file)
            );
        } else {
            result = Collections.singletonList(
                new ErrorResponseAction(ErrorResponseAction.FILE_NOT_FOUND, requestedFilePath + " not found")
            );
        }

        return result;
    }
}
