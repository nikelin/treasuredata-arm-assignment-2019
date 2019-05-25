package co.jp.treasuredata.armtd.server.server.commands.actions;

import co.jp.treasuredata.armtd.api.protocol.Packet;
import co.jp.treasuredata.armtd.api.protocol.Request;
import co.jp.treasuredata.armtd.server.io.ResponseAction;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TextResponseAction implements ResponseAction {

    private final String message;
    private final boolean addNewline;

    public TextResponseAction(String message) {
        this(message, false);
    }

    public TextResponseAction(String message, boolean addNewline) {
        this.message = message;
        this.addNewline = addNewline;
    }

    public String getMessage() {
        return message;
    }

    public boolean isAddNewline() {
        return addNewline;
    }

    @Override
    public CompletableFuture<List<Packet>> execute(Request request) {
        return CompletableFuture
            .supplyAsync(() -> {
                String newLine = (this.addNewline ? "\n\r" : "");
                String textResponse = newLine + message + newLine;
                return new Packet(request.getToken(), textResponse.getBytes());
            })
            .thenApply(Collections::singletonList);
    }
}
