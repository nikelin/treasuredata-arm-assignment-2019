package co.jp.treasuredata.armtd.utils;

import org.apache.commons.lang3.tuple.Pair;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ReadForeverCompletionHandler implements CompletionHandler<Integer, Pair<AsynchronousSocketChannel, ByteBuffer>> {
    private final CompletableFuture<Void> signal;

    private static final byte[] EOM = new byte[] { 32, 98, 12 };

    public ReadForeverCompletionHandler(CompletableFuture<Void> signal) {
        this.signal = signal;
    }

    @Override
    public void completed (Integer bytesRead, Pair < AsynchronousSocketChannel, ByteBuffer > attachment){
        if (bytesRead == -1) {
            signal.complete(null);
            return;
        }

        final AsynchronousSocketChannel channel = attachment.getLeft();
        ByteBuffer receivedData = attachment.getRight();
        byte[] eomCheck = new byte[] {
            receivedData.get(receivedData.position() - 3),
            receivedData.get(receivedData.position() - 2),
            receivedData.get(receivedData.position() - 1)
        };
        if (!Arrays.equals(EOM, eomCheck)) {
            try {
                channel.read(attachment.getRight(), attachment, this);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        } else {
            signal.complete(null);
        }
    }

    @Override
    public void failed (Throwable exc, Pair < AsynchronousSocketChannel, ByteBuffer > attachment){
        signal.completeExceptionally(exc);
    }
}