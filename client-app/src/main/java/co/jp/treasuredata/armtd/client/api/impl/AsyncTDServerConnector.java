package co.jp.treasuredata.armtd.client.api.impl;

import co.jp.treasuredata.armtd.api.protocol.Packet;
import co.jp.treasuredata.armtd.api.protocol.handler.PacketHandler;
import co.jp.treasuredata.armtd.api.protocol.io.FramesBuilderRunnable;
import co.jp.treasuredata.armtd.api.protocol.io.impl.StandardPacketsBuilder;
import co.jp.treasuredata.armtd.client.api.TDServerConnector;

import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class AsyncTDServerConnector implements TDServerConnector {

    private final String hostname;
    private final int port;
    private final ExecutorService executorService;
    private final PacketHandler packetHandler;

    private final BlockingDeque<ClientRequest> requestsDeque = new LinkedBlockingDeque<>();

    private final BlockingQueue<ClientRequest> clientRequests = new LinkedBlockingQueue<>();
    private final BlockingQueue<Pair<SocketChannel, ByteBuffer>> readBuffers = new LinkedBlockingQueue<>();
    private final BlockingQueue<Pair<SocketChannel, Packet>> packetsQueue = new LinkedBlockingQueue<>();

    private final AtomicInteger requestTokenSeq = new AtomicInteger();

    private final AtomicBoolean isTerminated = new AtomicBoolean(true);
    private volatile SocketChannel socketChannel;

    private final AtomicBoolean isReconnecting = new AtomicBoolean(false);

    private final Duration writeTimeout = Duration.of(10, ChronoUnit.SECONDS);
    private final Duration readTimeout = Duration.of(10, ChronoUnit.SECONDS);

    public AsyncTDServerConnector(PacketHandler handler, String hostname, int port) {
        this(Executors.newCachedThreadPool(), handler, hostname, port);
    }

    public AsyncTDServerConnector(ExecutorService service, PacketHandler handler, String hostname, int port) {
        this.packetHandler = handler;
        this.executorService = service;
        this.hostname = hostname;
        this.port = port;
    }

    private void socketConnect() throws IOException {
        this.socketChannel = SocketChannel.open();
        this.socketChannel.configureBlocking(true);
        this.socketChannel.connect(new InetSocketAddress(this.hostname, this.port));
        this.socketChannel.finishConnect();
    }

    @Override
    public void connect() throws IOException, InterruptedException, ExecutionException {
        socketConnect();

        this.isTerminated.compareAndSet(true, false);

        this.executorService.execute(new PacketsHandlerRunnable());
        this.executorService.execute(new FramesBuilderRunnable(1, new StandardPacketsBuilder(Integer.MAX_VALUE),
                readBuffers, packetsQueue, isTerminated));
        startRequestsHandler();
        startResponsesHandler();
    }

    @Override
    public void close() throws IOException {
        this.isTerminated.set(true);
        disconnect();
    }

    public void disconnect() throws IOException {
        if (this.socketChannel == null) {
            return;
        }

        if (!this.socketChannel.isOpen()) {
            this.socketChannel.close();
        }
    }

    private synchronized void reconnect() {
        System.out.println("[status] Reconnection");
        if (!this.isReconnecting.get()) {
            System.out.println("[status] Reconnection request declined - reconnection is in progress already.");
            return;
        }

        int attempts = 0;
        boolean connected = false;
        while (!connected && this.isReconnecting.get() && attempts < 5) {
            try {
                if (attempts > 1) {
                    System.out.println("[status] Reconnection attempt " + attempts);
                }

                try {
                    this.disconnect();
                } catch (Throwable ignored) { }

                try {
                    this.socketConnect();
                } catch (Throwable e) {
                    System.out.println("[error] Reconnection attempt failed");
                    continue;
                }

                System.out.println("[status] Reconnected to the TD File Server");

                connected = true;
            } finally {
                attempts += 1;
            }
        }

        this.isReconnecting.set(false);

    }

    private void startResponsesHandler() {
        this.executorService.execute(() -> {
            while (!this.isTerminated.get()) {
                try {
                    if (this.socketChannel == null) {
                        this.reconnect();
                    }

                    ByteBuffer buffer = ByteBuffer.allocate(512);
                    int bytesRead = -1;
                    int attempts = 5;
                    while (attempts > 0) {
                        try {
                            bytesRead = socketChannel.read(buffer);
                            break;
                        } catch (IOException e) {
                            attempts -= 1;
                        }
                    }

                    if (bytesRead == -1) {
                        break;
                    } else {
                        ByteBuffer inputBuffer = ByteBuffer.allocate(bytesRead);
                        inputBuffer.put(buffer.array(), 0, bytesRead);
                        inputBuffer.flip();

                        readBuffers.add(Pair.of(null, inputBuffer));
                    }

                    buffer.clear();
                } catch (Throwable e) {
                    e.printStackTrace();;
                }
            }
        });
    }

    private void startRequestsHandler() {
        this.executorService.execute(() -> {
            boolean stopped = false;
            while (!stopped && !this.isTerminated.get()) {
                if (this.socketChannel == null) {
                    this.reconnect();
                }

                final ClientRequest clientRequest;
                try {
                    clientRequest = clientRequests.poll(1000, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    continue;
                }

                if (clientRequest == null) continue;

                int attempts = clientRequest.command.isIdempotent() ? 5 : 1;
                ByteBuffer commandBuffer = ByteBuffer.allocate(clientRequest.command.byteSize());
                packetHandler.serialise(commandBuffer, clientRequest.command);

                while (attempts > 0) {
                    commandBuffer.flip();
                    try {
                        requestsDeque.add(clientRequest);
                        while(commandBuffer.hasRemaining()) {
                            socketChannel.write(commandBuffer);
                        }

                        break;
                    } catch (Throwable e) {
                        reconnect();
                        attempts -= 1;
                        stopped = true;
                    }
                }

                commandBuffer.clear();
            }

            System.out.println("[status] Disconnected");

            this.reconnect();
        });
    }

    private CompletableFuture<Response> sendCmd(String commandText, int expectedResponses) {
        CompletableFuture<Response> promise = new CompletableFuture<>();
        this.clientRequests.add(new ClientRequest(new Packet(requestTokenSeq.getAndIncrement(), commandText.getBytes()),
                promise, expectedResponses));
        return promise;
    }

    @Override
    public CompletableFuture<List<String>> listFiles() {
        return CompletableFuture.supplyAsync(System::currentTimeMillis)
            .thenCompose((startedAt) ->
                sendCmd("index", 1)
                .thenApply((data) ->
                    Arrays.asList(new String(data.data).trim().split("\n\r"))
                )
                .thenApply((list) -> {
                    System.out.println("[done] finished in " + (System.currentTimeMillis() - startedAt) + "ms");
                    return list;
                })
            );
    }

    @Override
    public CompletableFuture<String> fetchFile(String name) {
        return sendCmd("get " + name, 2)
                .thenApply((v) -> new String(v.data).trim());
    }

    @Override
    public CompletableFuture<Void> quit() {
        return sendCmd("quit", 1)
                .thenAccept((v) -> {});
    }

    private class PacketsHandlerRunnable implements Runnable {

        @Override
        public void run() {
            while (!AsyncTDServerConnector.this.isTerminated.get()) {
                final Pair<SocketChannel, Packet> packet;
                try {
                    packet = packetsQueue.poll(1, TimeUnit.SECONDS);
                } catch (Throwable e) {
                    continue;
                }

                if (packet == null) {
                    continue;
                }

                ClientRequest clientRequest = requestsDeque.peek();
                if (clientRequest == null) {
                    continue;
                }

                Response response = new Response(clientRequest, packet.getRight().getData());
                clientRequest.responses.add(response);

                if (clientRequest.expectedResponses == clientRequest.responses.size()) {
                    requestsDeque.remove();
                    clientRequest.promise.complete(response);
                }
            }
        }
    }

    private class ClientRequest {
        private final Packet command;
        private final CompletableFuture<Response> promise;
        private final int expectedResponses;
        private final List<Response> responses = new ArrayList<>();

        ClientRequest(Packet command, CompletableFuture<Response> promise, int expectedResponses) {
            this.command = command;
            this.promise = promise;
            this.expectedResponses = expectedResponses;
        }
    }

    private class Response {
        private final ClientRequest originalRequest;
        private final byte[] data;

        public Response(ClientRequest originalRequest, byte[] data) {
            this.originalRequest = originalRequest;
            this.data = data;
        }
    }

}
