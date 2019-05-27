package co.jp.treasuredata.armtd.client.commands;

import co.jp.treasuredata.armtd.api.protocol.Packet;

import java.io.PrintStream;

public class StandardBroadcastPacketHandler implements BroadcastPacketHandler {

    @Override
    public void handle(PrintStream out, Packet packet) {
        out.println("BROADCAST [" + packet.getLength() + "] >>> " + new String(packet.getData()));
    }
}
