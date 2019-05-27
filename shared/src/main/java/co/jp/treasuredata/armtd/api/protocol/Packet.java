package co.jp.treasuredata.armtd.api.protocol;

public final class Packet {
  private static final int BROADCAST_PACKET_TOKEN = 1921883302;

  private final int token;
  private final int length;
  private final byte[] data;

  public Packet(int token, byte[] data) {
    this(token, data.length, data);
  }

  public Packet(int token, int length, byte[] data) {
    this.token = token;
    this.length = length;
    this.data = data;
  }

  public int getToken() {
    return token;
  }

  public int getLength() {
    return length;
  }

  public byte[] getData() {
    return data;
  }

  public int byteSize() {
    return Integer.BYTES + Integer.BYTES + length;
  }

  public static boolean isBroadcast(Packet packet) {
    return packet.getToken() == BROADCAST_PACKET_TOKEN;
  }

  public static Packet broadcast(byte[] data) {
    return new Packet(BROADCAST_PACKET_TOKEN, data);
  }
}
