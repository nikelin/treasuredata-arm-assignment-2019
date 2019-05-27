package co.jp.treasuredata.armtd.api.protocol.io;

import co.jp.treasuredata.armtd.api.protocol.Packet;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class FramesBuilderRunnable implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(FramesBuilderRunnable.class);

    private final Map<SocketChannel, PacketsBuilder> inFlightFrame = new HashMap<>();

    private final int id;
    private final BlockingQueue<Pair<SocketChannel, ByteBuffer>> readBuffers;
    private final BlockingQueue<Pair<SocketChannel, Packet>> packetsQueue;
    private final PacketsBuilder packetsBuilder;

    private final AtomicBoolean terminateSignal;

    public FramesBuilderRunnable(int id, PacketsBuilder packetsBuilder,
                                 BlockingQueue<Pair<SocketChannel, ByteBuffer>> readBuffers,
                                 BlockingQueue<Pair<SocketChannel, Packet>> packetsQueue,
                                 AtomicBoolean terminateSignal) {
        this.id = id;
        this.packetsBuilder = packetsBuilder;
        this.packetsQueue = packetsQueue;
        this.readBuffers = readBuffers;
        this.terminateSignal = terminateSignal;
    }

    @Override
    public void run() {
        logger.info("= Frames builder #" + this.id);

        while (!this.terminateSignal.get()) {
            try {
                ByteBuffer buffer = null;
                SocketChannel socketChannel = null;

                Pair<SocketChannel, ByteBuffer> pair = this.readBuffers.poll(1, TimeUnit.SECONDS);
                if (pair != null) {
                    socketChannel = pair.getLeft();
                    buffer = pair.getRight();
                }

                if (buffer == null) {
                    continue;
                }

                PacketsBuilder builder = inFlightFrame.computeIfAbsent(socketChannel, (k) -> packetsBuilder);
                Pair<Packet, Integer> consumed;

                boolean stop = false;
                while (buffer.hasRemaining() && !stop) {
                    consumed = builder.consume(buffer);

                    if (consumed.getRight() == 0) {
                        builder.dispose();
                        continue;
                    }

                    if (consumed.getRight() == -1) {
                        builder.dispose();
                        inFlightFrame.remove(socketChannel);
                        stop = true;
                        continue;
                    }

                    if (consumed.getLeft() != null) {
                        logger.info("Packet synthesised " + consumed.getRight() + " bytes");
                        packetsQueue.add(Pair.of(socketChannel, consumed.getLeft()));
                        builder.dispose();
                    }
                }
            } catch (Throwable e) {
                logger.error("Frames builder error", e);
            }
        }

        logger.info("= Frames builder #" + this.id + " has terminated");
    }
}
