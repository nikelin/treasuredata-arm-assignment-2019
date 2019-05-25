package co.jp.treasuredata.armtd.server.server.commands.actions;

import co.jp.treasuredata.armtd.api.protocol.Packet;
import co.jp.treasuredata.armtd.api.protocol.Request;
import co.jp.treasuredata.armtd.server.io.ResponseAction;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class FileResponseAction implements ResponseAction {

    private final File file;

    public FileResponseAction(File file) {
        this.file = file;
    }

    @Override
    public CompletableFuture<List<Packet>> execute(Request request) {
        CompletableFuture<List<Packet>> future = new CompletableFuture<>();

        try {
            RandomAccessFile accessFile = new RandomAccessFile(file, "r");
            if (!file.exists()) {
                future.completeExceptionally(new IOException("file.not.exists"));
            } else {
                FileChannel inChannel = accessFile.getChannel();
                long fileSize = inChannel.size();
                ByteBuffer buffer = ByteBuffer.allocate((int) fileSize);
                inChannel.read(buffer);
                buffer.flip();
                inChannel.close();
                accessFile.close();

                future.complete(Collections.singletonList(new Packet(request.getToken(), buffer.array())));
            }
        } catch (IOException e) {
            future.completeExceptionally(new RuntimeException(e));
        }

        return future;
    }
}
