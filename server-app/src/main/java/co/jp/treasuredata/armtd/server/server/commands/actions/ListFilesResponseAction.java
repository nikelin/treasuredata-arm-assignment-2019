package co.jp.treasuredata.armtd.server.server.commands.actions;

import co.jp.treasuredata.armtd.api.protocol.Packet;
import co.jp.treasuredata.armtd.api.protocol.Request;
import co.jp.treasuredata.armtd.server.io.OutputChannel;
import co.jp.treasuredata.armtd.server.io.ResponseAction;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class ListFilesResponseAction implements ResponseAction {
    private final File directory;

    public ListFilesResponseAction(File directory) {
        this.directory = directory;
    }

    @Override
    public CompletableFuture<List<Packet>> execute(Request request) {
        return CompletableFuture.supplyAsync(() -> {
            File[] directoryFiles = directory.listFiles(File::isFile);

            StringBuilder responseBuilder = new StringBuilder();
            for (File file : directoryFiles) {
                responseBuilder.append(file.getName()).append("\n\r");
            }

            return new Packet(request.getToken(), responseBuilder.toString().getBytes());
        })
        .thenApply(Collections::singletonList);
    }
}
