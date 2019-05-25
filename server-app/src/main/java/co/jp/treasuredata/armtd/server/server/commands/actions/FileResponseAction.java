package co.jp.treasuredata.armtd.server.server.commands.actions;

import co.jp.treasuredata.armtd.api.protocol.Packet;
import co.jp.treasuredata.armtd.api.protocol.Request;
import co.jp.treasuredata.armtd.server.io.OutputChannel;
import co.jp.treasuredata.armtd.server.io.ResponseAction;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class FileResponseAction implements ResponseAction {

    private final File file;

    public FileResponseAction(File file) {
        this.file = file;
    }

    @Override
    public CompletableFuture<List<Packet>> execute(Request request) {
        return CompletableFuture
            .supplyAsync(() -> {
                try {
                    RandomAccessFile accessFile = new RandomAccessFile(file, "r");
                    FileChannel inChannel = accessFile.getChannel();
                    long fileSize = inChannel.size();
                    ByteBuffer buffer = ByteBuffer.allocate((int) fileSize);
                    inChannel.read(buffer);
                    buffer.flip();
                    inChannel.close();
                    accessFile.close();

                    return new Packet(request.getToken(), buffer.array());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            })
            .thenApply(Collections::singletonList);
    }
}
