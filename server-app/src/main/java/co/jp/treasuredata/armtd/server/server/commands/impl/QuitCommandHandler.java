package co.jp.treasuredata.armtd.server.server.commands.impl;

import co.jp.treasuredata.armtd.api.protocol.Packet;
import co.jp.treasuredata.armtd.api.protocol.Request;
import co.jp.treasuredata.armtd.server.io.ResponseAction;
import co.jp.treasuredata.armtd.server.io.Server;
import co.jp.treasuredata.armtd.server.server.commands.actions.ErrorResponseAction;
import co.jp.treasuredata.armtd.server.server.commands.discovery.DiscoverableHandler;
import co.jp.treasuredata.armtd.server.server.commands.RouteHandler;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@DiscoverableHandler(mappings = {"q", "quit"}, description = "Command to stop the file server application")
public class QuitCommandHandler implements RouteHandler  {

    public static class ResponseActionImpl implements ResponseAction {
        private final Server server;
        public ResponseActionImpl(Server server) {
            this.server = server;
        }

        @Override
        public CompletableFuture<List<Packet>> execute(Request request) {
            return CompletableFuture
                .supplyAsync(() -> {
                    try {
                        server.stop();
                        return true;
                    } catch (Throwable e) {
                        return false;
                    }
                })
                .thenCompose((v) ->
                        ErrorResponseAction.internalException("server failed to stop").execute(request)
                );
        }
    }

    @Override
    public List<ResponseAction> handleRequest(Server server, Request request) {
        return Collections.singletonList(
            new ResponseActionImpl(server)
        );
    }
}
