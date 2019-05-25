package co.jp.treasuredata.armtd.server.server.commands.actions;

import co.jp.treasuredata.armtd.api.protocol.Packet;
import co.jp.treasuredata.armtd.api.protocol.Request;
import co.jp.treasuredata.armtd.server.io.ResponseAction;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ErrorResponseAction implements ResponseAction {
    public static final int UNSUPPORTED_METHOD_CALL = 1;
    public static final int FILE_NOT_FOUND = 2;
    public static final int CORRUPTED_REQUEST = 3;
    public static final int INTERNAL_EXCEPTION = 4;

    private final Integer code;
    private final String messageDetails;

    public ErrorResponseAction(Integer code) {
        this(code, null);
    }

    public ErrorResponseAction(Integer code, String messageDetails) {
        this.code = code;
        this.messageDetails = messageDetails;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessageDetails() {
        return messageDetails;
    }

    @Override
    public CompletableFuture<List<Packet>> execute(Request request) {
        return CompletableFuture.supplyAsync(() ->
            Collections.singletonList(
                new Packet(request.getToken(), ("error " + this.code + ":" + messageDetails).getBytes())
            )
        );
    }

    public static ErrorResponseAction internalException(String details) {
        return new ErrorResponseAction(ErrorResponseAction.INTERNAL_EXCEPTION, details == null ? "" : details);
    }

    public static ErrorResponseAction corruptedRequest() {
        return new ErrorResponseAction(ErrorResponseAction.CORRUPTED_REQUEST, "corrupted.request");
    }
}
