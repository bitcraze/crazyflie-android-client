
package se.bitcraze.crazyflielib.crtp;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Packet of data which can be sent/received from/to the Crazyflie. All packet
 * implementations must be immutable to avoid issues with modifying packets via
 * references, e.g. in a send queue.
 */
public abstract class CrtpPacket {

    /**
     *Lists the available ports for the CRTP. 
     */
    public final static byte CONSOLE = 0x00;
    public final static byte PARAM = 0x02;
    public final static byte COMMANDER = 0x03;
    public final static byte LOGGING = 0x05;
    public final static byte DEBUGDRIVER = 0x0e;
    public final static byte LINKCTRL = 0x0f;
    public final static byte ALL = (byte) 0xff;

    private byte PORT;

    /**
     * Byte order used when serializing/deserializing packets.
     */
    public static final ByteOrder BYTE_ORDER = ByteOrder.LITTLE_ENDIAN;

    /**
     * NULL packet. Header is 0xFF without any data.
     */
    public static final CrtpPacket NULL_PACKET = new CrtpPacket(ALL) {
        @Override
        protected void serializeData(ByteBuffer buffer) {
        }

        @Override
        protected int getDataByteCount() {
            return 0;
        }
    };

    private final byte mPacketHeader;
    private byte[] mSerializedPacket;

    /**
     * Create a new packet.
     * 
     * @param channel channel to set in the header.
     * @param port port to set in the header.
     */
    public CrtpPacket(int channel, int port) {
        this((byte) (((port & 0x0F) << 4) | (channel & 0x03)));
        PORT = (byte) port;
    }

    /**
     * Create a new packet.
     * 
     * @param packetHeader header of the packet.
     */
    public CrtpPacket(byte packetHeader) {
        this.mPacketHeader = packetHeader;
        this.mSerializedPacket = null;
    }

    /**
     * Get the header of the packet.
     * 
     * @return the header of the packet.
     */
    public byte getHeader() {
        return mPacketHeader;
    }

    public byte getPort() {
        return PORT;
    }

    /**
     * Serialize the data of the packet. Must not include the header.
     * 
     * @param buffer the target buffer for serialization.
     */
    protected abstract void serializeData(ByteBuffer buffer);

    /**
     * Get the number of bytes used when serializing the data.
     * 
     * @return number of bytes required by the serialized data.
     */
    protected abstract int getDataByteCount();

    /**
     * Convert the packet to a byte array suitable for transmission.
     * 
     * @return byte array containing the header and packet data.
     */
    public byte[] toByteArray() {
        // if it's the first call, serialize the packet and cache it
        if (mSerializedPacket == null) {
            ByteBuffer buffer = ByteBuffer.allocate(getDataByteCount() + 1).order(BYTE_ORDER);
            buffer.put(mPacketHeader);
            serializeData(buffer);
            mSerializedPacket = buffer.array();
        }

        return mSerializedPacket;
    }
}
