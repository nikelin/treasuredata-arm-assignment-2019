package co.jp.treasuredata.armtd.server.io;

import java.io.IOException;

public interface Server {

    void start(int port) throws IOException, InterruptedException;

    void stop() throws InterruptedException;

    void awaitTermination() throws InterruptedException;

}
