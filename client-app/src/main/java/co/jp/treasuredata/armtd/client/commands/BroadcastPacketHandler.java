package co.jp.treasuredata.armtd.client.commands;

import co.jp.treasuredata.armtd.api.protocol.Packet;

import java.io.PrintStream;

public interface BroadcastPacketHandler {

    void handle(PrintStream out, Packet packet);

}
