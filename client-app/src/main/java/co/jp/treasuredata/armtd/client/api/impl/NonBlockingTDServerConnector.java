package co.jp.treasuredata.armtd.client.api.impl;

import co.jp.treasuredata.armtd.api.protocol.Packet;
import co.jp.treasuredata.armtd.api.protocol.handler.PacketHandler;
import co.jp.treasuredata.armtd.api.protocol.io.FramesBuilderRunnable;
import co.jp.treasuredata.armtd.api.protocol.io.impl.StandardPacketsBuilder;
import co.jp.treasuredata.armtd.client.ClientConfig;
import co.jp.treasuredata.armtd.client.api.TDServerConnector;

import co.jp.treasuredata.armtd.client.commands.BroadcastPacketHandler;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.io.PrintStream;
import java.net.ConnectException;
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
    private final static int DEFAULT_MAX_RETRY_ATTEMPTS = 10;

    private final ExecutorService executorService;
    private final PacketHandler packetHandler;
    private final BroadcastPacketHandler broadcastPacketHandler;

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

    public NonBlockingTDServerConnector(PrintStream consoleOut, PacketHandler handler, BroadcastPacketHandler broadcastPacketHandler,
                                        ClientConfig config) {
        this(consoleOut, Executors.newCachedThreadPool(), handler, broadcastPacketHandler, config);
    }

    public NonBlockingTDServerConnector(PrintStream consoleOut, ExecutorService service, PacketHandler handler,
                                        BroadcastPacketHandler broadcastPacketHandler,
                                        ClientConfig config) {
        this.packetHandler = handler;
        this.broadcastPacketHandler = broadcastPacketHandler;
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
    public boolean isConnected() {
        return !this.isTerminated.get();
    }

    @Override
    public void connect() throws IOException, InterruptedException, ExecutionException {
        if (!this.isReconnecting.compareAndSet(false, true)) {
            consoleOut.println("[status] Connection request declined - there is another connection request in progress already.");
            return;
        }

        int attempts = 0;
        int maxAttempts = config.getMaxReconnectAttempts() == null ? DEFAULT_MAX_RETRY_ATTEMPTS : config.getMaxReconnectAttempts();
        boolean connected = false;
        while (!connected && this.isReconnecting.get() && attempts < maxAttempts) {
            try {
                if (attempts > 1 && config.getVerbose()) {
                    consoleOut.println("[status] Connection attempt " + attempts);
                }

                try {
                    this.close();
                } catch (Throwable ignored) {}

                try {
                    socketConnect();
                    this.isTerminated.set(false);
                    startEventsLoop();
                } catch (Throwable e) {
                    if (config.getVerbose()) {
                        e.printStackTrace();
                        ;
                        consoleOut.println("[error] Connection attempt failed");
                    }
                    try {
                        Thread.sleep(attempts * 1000);
                    } catch (Throwable ignored) {}
                    continue;
                }

                if (config.getVerbose()) {
                    consoleOut.println("[status] Connected to the TD File Server");
                }

                connected = true;
            } finally {
                attempts += 1;
            }
        }

        this.isReconnecting.set(false);

        if (!connected) {
            this.isTerminated.set(true);
        }
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

    private void startEventsLoop() {
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
                            selectedKey.cancel();
                        } else if (selectedKey.isConnectable()) {
                            selectedKey.interestOps(SelectionKey.OP_READ);
                            this.executorService.execute(new PacketsHandlerRunnable());
                            this.executorService.execute(new FramesBuilderRunnable(1, new StandardPacketsBuilder(Integer.MAX_VALUE),
                                    readBuffers, packetsQueue, isTerminated));
                            startRequestsHandler();

                            try {
                                ((SocketChannel) selectedKey.channel()).finishConnect();
                            } catch (ConnectException e) {
                                selectedKey.cancel();
                                close();
                            }
                        } else if (selectedKey.isReadable()) {
                            try {
                                int bytesRead = readChannel(selectedKey);
                                if (bytesRead == -1) {
                                    selectedKey.cancel();
                                    close();
                                }
                            } catch (IOException e) {
                                e.printStackTrace(consoleOut);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace(consoleOut);
                this.isTerminated.set(true);
            }
        });
    }

    private int readChannel(SelectionKey key)  throws IOException  {
        SocketChannel channel = (SocketChannel) key.channel();

        ByteBuffer buffer = ByteBuffer.allocate(512);
        int bytesRead = channel.read(buffer);
        buffer.flip();

        if (bytesRead <= 0) return bytesRead;

        ByteBuffer inputBuffer = ByteBuffer.allocate(bytesRead);
        inputBuffer.put(buffer.array(), 0, bytesRead);
        inputBuffer.flip();
        buffer.clear();

        readBuffers.add(Pair.of(channel, inputBuffer));

        if (config.getVerbose()) {
            consoleOut.println("[status] Buffer received - " + bytesRead);
        }

        buffer.clear();

        return bytesRead;
    }

    private void startRequestsHandler() {
        this.executorService.execute(() -> {
            while (!this.isTerminated.get()) {
                if (this.socketChannel == null) {
                    break;
                }

                final ClientRequest clientRequest;
                try {
                    clientRequest = clientRequests.poll(1000, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    continue;
                }

                if (clientRequest == null) continue;

                ByteBuffer commandBuffer = ByteBuffer.allocate(clientRequest.command.byteSize());
                packetHandler.serialise(commandBuffer, clientRequest.command);

                commandBuffer.flip();

                requestsDeque.add(clientRequest);

                try {
                    while (commandBuffer.hasRemaining()) {
                        socketChannel.write(commandBuffer);
                    }
                } catch (IOException e) {
                    continue;
                }

                commandBuffer.clear();
            }
        });
    }

    private void handleBroadcastPacket(Packet packet) {
        if (this.broadcastPacketHandler == null) {
            if (config.getVerbose()) {
                consoleOut.println("[warning] No broadcast handler defined - ignoring received broadcast packet");
            }
        } else {
            this.broadcastPacketHandler.handle(consoleOut, packet);
        }
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
                    if (config.getVerbose()) {
                        consoleOut.println("[done] finished in " + (System.currentTimeMillis() - startedAt) + "ms");
                    }

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

                if (Packet.isBroadcast(packet.getRight())) {
                    handleBroadcastPacket(packet.getRight());
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
