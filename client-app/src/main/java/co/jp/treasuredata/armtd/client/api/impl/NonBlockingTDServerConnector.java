package co.jp.treasuredata.armtd.client.api.impl;

import co.jp.treasuredata.armtd.api.protocol.Packet;
import co.jp.treasuredata.armtd.api.protocol.handler.PacketHandler;
import co.jp.treasuredata.armtd.api.protocol.io.FramesBuilderRunnable;
import co.jp.treasuredata.armtd.api.protocol.io.impl.StandardPacketsBuilder;
import co.jp.treasuredata.armtd.client.ClientConfig;
import co.jp.treasuredata.armtd.client.api.TDServerConnector;

import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class NonBlockingTDServerConnector implements TDServerConnector {

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

    private final ClientConfig config;

    private final PrintStream consoleOut;

    public NonBlockingTDServerConnector(PrintStream consoleOut, PacketHandler handler, ClientConfig config) {
        this(consoleOut, Executors.newCachedThreadPool(), handler, config);
    }

    public NonBlockingTDServerConnector(PrintStream consoleOut, ExecutorService service, PacketHandler handler, ClientConfig config) {
        this.packetHandler = handler;
        this.executorService = service;
        this.config = config;
        this.consoleOut = consoleOut;
    }

    private void socketConnect() throws IOException {
        this.socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.connect(config.getServerHost() != null ? new InetSocketAddress(config.getServerHost(), config.getServerPort())
                : new InetSocketAddress(config.getServerPort()));
    }

    @Override
    public void connect() throws IOException, InterruptedException, ExecutionException {
        socketConnect();

        this.isTerminated.set(false);

        this.executorService.execute(new PacketsHandlerRunnable());
        this.executorService.execute(new FramesBuilderRunnable(1, new StandardPacketsBuilder(Integer.MAX_VALUE),
                readBuffers, packetsQueue, isTerminated));
        startRequestsHandler();
        startResponsesHandler();
    }

    @Override
    public synchronized void close() throws IOException {
        this.isTerminated.set(true);
        disconnect();
        this.requestsDeque.clear();
        this.readBuffers.clear();
        this.packetsQueue.clear();
        this.clientRequests.clear();
    }

    public synchronized void disconnect() throws IOException {
        if (this.socketChannel == null) {
            return;
        }

        if (this.socketChannel.isOpen()) {
            this.socketChannel.close();
        }
    }

    private synchronized void reconnect() {
        consoleOut.println("[status] Reconnection");
        if (!this.isReconnecting.compareAndSet(false, true)) {
            consoleOut.println("[status] Reconnection request declined - reconnection is in progress already.");
            return;
        }

        int attempts = 0;
        boolean connected = false;
        while (!connected && this.isReconnecting.get() && attempts < 5) {
            try {
                if (attempts > 1) {
                    consoleOut.println("[status] Reconnection attempt " + attempts);
                }

                try {
                    this.close();
                } catch (Throwable ignored) {}

                try {
                    this.connect();
                } catch (Throwable e) {
                    consoleOut.println("[error] Reconnection attempt failed");
                    continue;
                }

                consoleOut.println("[status] Reconnected to the TD File Server");

                connected = true;
            } finally {
                attempts += 1;
            }
        }

        this.isReconnecting.set(false);
    }

    private void startResponsesHandler() {
        this.executorService.execute(() -> {
            try {
                Selector selector = Selector.open();
                socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_CONNECT);

                while (!this.isTerminated.get()) {
                    selector.select();

                    Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    Iterator<SelectionKey> iterator = selectedKeys.iterator();
                    while (iterator.hasNext()) {
                        SelectionKey selectedKey = iterator.next();

                        iterator.remove();

                        if (!selectedKey.isValid()) {
                            continue;
                        } else if (selectedKey.isConnectable()) {
                            selectedKey.interestOps(SelectionKey.OP_READ);
                            ((SocketChannel) selectedKey.channel()).finishConnect();
                        } else if (selectedKey.isReadable()) {
                            try {
                                readChannel(selectedKey);
                            } catch (IOException e) {
                                e.printStackTrace();
                                continue;
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                this.isTerminated.set(true);
            }
        });
    }

    private void readChannel(SelectionKey key)  throws IOException  {
        SocketChannel channel = (SocketChannel) key.channel();

        ByteBuffer buffer = ByteBuffer.allocate(512);
        int bytesRead = channel.read(buffer);
        buffer.flip();

        if (bytesRead <= 0) return;

        ByteBuffer inputBuffer = ByteBuffer.allocate(bytesRead);
        inputBuffer.put(buffer.array(), 0, bytesRead);
        inputBuffer.flip();
        buffer.clear();
        readBuffers.add(Pair.of(channel, inputBuffer));

        consoleOut.println("Buffer received - " + bytesRead);

        buffer.clear();
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

            consoleOut.println("[status] Disconnected");

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
                    consoleOut.println("[done] finished in " + (System.currentTimeMillis() - startedAt) + "ms");
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
            while (!NonBlockingTDServerConnector.this.isTerminated.get()) {
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
