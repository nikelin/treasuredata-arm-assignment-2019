package co.jp.treasuredata.armtd.api.protocol;

public final class Packet {
    private final int token;
    private final int length;
    private final byte[] data;
    private final boolean isIdempotent;

    public Packet(int token, byte[] data) {
        this(token, data, false);
    }

    public Packet(int token, byte[] data, boolean isIdempotent) {
        this(token, data.length, data, isIdempotent);
    }

    public Packet(int token, int length, byte[] data) {
        this(token, length, data, false);
    }

    public Packet(int token, int length, byte[] data, boolean isIdempotent) {
        this.token = token;
        this.length = length;
        this.data = data;
        this.isIdempotent = isIdempotent;
    }

    public int getToken() {
        return token;
    }

    public boolean isIdempotent() {
        return isIdempotent;
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
}
