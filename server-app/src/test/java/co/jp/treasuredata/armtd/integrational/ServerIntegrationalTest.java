package co.jp.treasuredata.armtd.integrational;

import co.jp.treasuredata.armtd.api.protocol.Packet;
import co.jp.treasuredata.armtd.api.protocol.Request;
import co.jp.treasuredata.armtd.api.protocol.handler.TDPacketHandler;
import co.jp.treasuredata.armtd.server.io.ResponseAction;
import co.jp.treasuredata.armtd.server.io.Server;
import co.jp.treasuredata.armtd.server.io.impl.NonBlockingServer;
import co.jp.treasuredata.armtd.server.server.commands.HandlerException;
import co.jp.treasuredata.armtd.server.server.commands.RouteHandler;
import co.jp.treasuredata.armtd.server.server.commands.actions.TextResponseAction;
import co.jp.treasuredata.armtd.utils.SocketUtils;
import org.junit.Test;

import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.Assert.*;

public class ServerIntegrationalTest {

    @Test
    public void testSingleConnectionBasicRequest() throws Throwable {
        final int serverPort = SocketUtils.nextFreePort();

        ExecutorService service = Executors.newCachedThreadPool();

        String checkPayload = "single connection request";
        TDPacketHandler packetHandler = new TDPacketHandler();

        CountDownLatch latch = new CountDownLatch(1000);

        NonBlockingServer server = new NonBlockingServer(new DummyRouteHandler(checkPayload, latch), packetHandler);
        service.execute(() -> {
            try {
                server.start(serverPort);
            } catch (Throwable e) {}
        });

        SocketChannel socket = SocketChannel.open();
        socket.connect(new InetSocketAddress(serverPort));

        for ( int i = 0; i < 1000; i++ ) {
            Packet packet = new Packet(i, checkPayload.getBytes());
            ByteBuffer buffer = ByteBuffer.allocate(packet.byteSize());
            packetHandler.serialise(buffer, packet);

            buffer.flip();
            while(buffer.hasRemaining()) {
                socket.write(buffer);
            }

            ByteBuffer b = ByteBuffer.allocate(512);
            socket.read(b);
            b.flip();
            int length = b.getInt();
            int token = b.getInt();
            byte[] data = new byte[length];
            b.get(data);
            Packet p = new Packet(token, data);
            assertEquals(checkPayload + " " + i, new String(p.getData()));
        }


        latch.await();
    }

    @Test
    public void testMultipleConnectionBasicRequest() throws Throwable {
        final int serverPort = SocketUtils.nextFreePort();

        ExecutorService service = Executors.newCachedThreadPool();

        String checkPayload = "single connection request";
        TDPacketHandler packetHandler = new TDPacketHandler();

        CountDownLatch latch = new CountDownLatch(1000);

        NonBlockingServer server = new NonBlockingServer(new DummyRouteHandler(checkPayload, latch), packetHandler);
        service.execute(() -> {
            try {
                server.start(serverPort);
            } catch (Throwable e) {}
        });

        for (int connection = 0; connection < 100; connection++) {
            final int connectionId = connection;
            service.execute(() -> {
                try {
                    SocketChannel socket = SocketChannel.open();
                    socket.connect(new InetSocketAddress(serverPort));

                    for (int j = 0; j < 100; j++) {
                        Packet packet = new Packet(connectionId * j, checkPayload.getBytes());
                        ByteBuffer buffer = ByteBuffer.allocate(packet.byteSize());
                        packetHandler.serialise(buffer, packet);

                        buffer.flip();
                        while (buffer.hasRemaining()) {
                            socket.write(buffer);
                        }

                        ByteBuffer b = ByteBuffer.allocate(512);
                        socket.read(b);
                        b.flip();
                        int length = b.getInt();
                        int token = b.getInt();
                        byte[] data = new byte[length];
                        b.get(data);
                        Packet p = new Packet(token, data);
                        assertEquals(checkPayload + " " + (connectionId * j), new String(p.getData()));
                    }
                } catch (Throwable e) {
                    fail(e.getMessage());
                }
            });
        }


        latch.await();
    }


    public class DummyRouteHandler implements RouteHandler {
        private final String staticMessage;
        private final CountDownLatch latch;

        DummyRouteHandler(String staticMessage, CountDownLatch latch) {
            this.staticMessage = staticMessage;
            this.latch = latch;
        }

        @Override
        public List<ResponseAction> handleRequest(Server server, Request request) throws HandlerException {
            return Collections.singletonList(
                new TextResponseAction(staticMessage + " " + request.getToken()) {
                    @Override
                    public CompletableFuture<List<Packet>> execute(Request request) {
                        return super.execute(request).thenApply((v) -> {
                           latch.countDown();
                           return v;
                        });
                    }
                }
            );
        }
    }
}
