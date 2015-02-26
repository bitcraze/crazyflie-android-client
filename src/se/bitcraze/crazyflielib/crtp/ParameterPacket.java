package se.bitcraze.crazyflielib.crtp;

import java.nio.ByteBuffer;

/**
 * Packet used for setting hover mode
 */
public class ParameterPacket extends CrtpPacket {
    private boolean hover;
    /**
     * Create a new parameter packet.
     * @param hover
     */
    public ParameterPacket(boolean hover) {
        super((byte) 0x22);
        this.hover = hover;
    }

    @Override
    protected void serializeData(ByteBuffer buffer) {
        buffer.put((byte)10);
        buffer.put((byte)(hover ? 1 : 0));
    }

    @Override
    protected int getDataByteCount() {
        return 2; // 10 and hover
    }
}
