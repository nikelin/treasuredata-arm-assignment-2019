package co.jp.treasuredata.armtd.server.io;

import co.jp.treasuredata.armtd.api.protocol.Packet;
import co.jp.treasuredata.armtd.api.protocol.Request;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface ResponseAction {

    CompletableFuture<List<Packet>> execute(Request request);

}
