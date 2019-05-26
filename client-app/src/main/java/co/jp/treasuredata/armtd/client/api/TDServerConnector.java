package co.jp.treasuredata.armtd.client.api;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public interface TDServerConnector {

    CompletableFuture<List<String>> listFiles();

    CompletableFuture<String> fetchFile(String name);

    CompletableFuture<Void> quit();

    boolean isConnected();

    void connect() throws IOException, InterruptedException, ExecutionException;

    void disconnect() throws IOException, InterruptedException;

    void close() throws IOException, InterruptedException;

}
