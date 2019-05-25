package co.jp.treasuredata.armtd.server.io;

import co.jp.treasuredata.armtd.api.protocol.Packet;

import java.util.function.Function;

public interface OutputChannel {

    default void write(Packet buffer) {
        this.write(buffer, Function.identity(), (v) -> {return null;});
    }

    void write(Packet buffer, Function<Void, Void> onComplete, Function<Throwable, Void> onFailure);

    void close();

}
