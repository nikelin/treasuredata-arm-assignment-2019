package co.jp.treasuredata.armtd.api.protocol.io.impl;

import co.jp.treasuredata.armtd.api.protocol.Packet;
import co.jp.treasuredata.armtd.api.protocol.io.PacketsBuilder;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class StandardPacketsBuilder implements PacketsBuilder {
    private int totalBytesRead;
    private int lengthDataReceived;

    private int lengthData;
    private boolean lengthRead;

    private boolean tokenRead;
    private int tokenData;

    private ByteBuffer bodyData;

    private final int maxInputFrameSize;

    public StandardPacketsBuilder(int maxInputFrameSize) {
        this.maxInputFrameSize = maxInputFrameSize;
    }

    private int readLength(ByteBuffer input) {
        try {
            int length = input.getInt();
            if (length > maxInputFrameSize || length == 0) {
                return 0;
            }
            totalBytesRead += Integer.BYTES;
            lengthDataReceived = 0;
            lengthRead = true;
            lengthData = length;
            return Integer.BYTES;
        } catch (Throwable e) {
            return -1;
        }
    }

    private int readToken(ByteBuffer input) {
        try {
            int token = input.getInt();
            totalBytesRead += Integer.BYTES;
            tokenRead = true;
            tokenData = token;
            return Integer.BYTES;
        } catch (Throwable e) {
            return -1;
        }
    }

    private int readBody(ByteBuffer input) {
        if (lengthData <= 0) return -1;

        if (this.bodyData == null) {
            this.bodyData = ByteBuffer.allocate(lengthData);
        }

        int bytesRead = Math.min(lengthData - lengthDataReceived, input.remaining());
        if (bytesRead == 0) {
            return 0;
        }

        byte[] inputPart = new byte[bytesRead];
        input.get(inputPart);
        bodyData.put(inputPart);
        lengthDataReceived += bytesRead;
        totalBytesRead += bytesRead;
        return bytesRead;
    }

    @Override
    public void dispose() {
        this.totalBytesRead = 0;
        this.lengthDataReceived = 0;

        this.lengthData = 0;
        this.lengthRead = false;
        this.tokenRead = false;
        this.tokenData = 0;

        if (this.bodyData != null) {
            this.bodyData.clear();
            this.bodyData = null;
        }
    }

    @Override
    public Pair<Packet, Integer> consume(ByteBuffer buffer) {
        if (!lengthRead) {
            return Pair.of(null, readLength(buffer));
        } else if (!tokenRead) {
            return Pair.of(null, readToken(buffer));
        } else if (lengthDataReceived == this.lengthData) {
            return Pair.of(new Packet(tokenData, lengthData, Arrays.copyOfRange(bodyData.array(), 0, lengthData)),
                    totalBytesRead);
        } else {
            int bodyBytesRead = readBody(buffer);
            if (bodyBytesRead == -1) {
                return Pair.of(null, -1);
            } else if (lengthDataReceived == this.lengthData ) {
                return Pair.of(new Packet(tokenData, lengthData, Arrays.copyOfRange(bodyData.array(), 0, lengthData)),
                        totalBytesRead);
            } else {
                return Pair.of(null, totalBytesRead);
            }
        }
    }
}
