package se.bitcraze.crazyflie.lib.crtp;

import java.nio.ByteBuffer;

/**
 * Created by arnaud on 31/03/17.
 */

public class StopPacket extends CrtpPacket {

    public final static byte PACKET_TYPE_STOP = 0x00;

    public StopPacket() {
        super(0, CrtpPort.COMMANDER_GENERIC);
    }

    @Override
    protected void serializeData(ByteBuffer buffer) {
        buffer.put(PACKET_TYPE_STOP);
    }

    @Override
    protected int getDataByteCount() {
        return 1; // 1 byte (type)
    }

    @Override
    public String toString() {
        return "StopPacket";
    }
}
