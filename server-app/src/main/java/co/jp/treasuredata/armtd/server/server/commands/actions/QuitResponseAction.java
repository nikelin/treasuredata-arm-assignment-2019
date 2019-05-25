package co.jp.treasuredata.armtd.server.server.commands.actions;

import co.jp.treasuredata.armtd.api.protocol.Packet;
import co.jp.treasuredata.armtd.api.protocol.Request;
import co.jp.treasuredata.armtd.server.io.ResponseAction;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class QuitResponseAction implements ResponseAction {

    public QuitResponseAction() {

    }

    @Override
    public CompletableFuture<List<Packet>> execute(Request request) {
        return null;
    }
}
