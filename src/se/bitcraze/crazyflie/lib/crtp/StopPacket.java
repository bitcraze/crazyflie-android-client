package se.bitcraze.crazyflie.lib.crtp;

import java.nio.ByteBuffer;

/**
 * Created by arnaud on 31/03/17.
 */

public class StopPacket extends CrtpPacket {

    public StopPacket() {
        super(0, CrtpPort.COMMANDER_GENERIC);
    }

    @Override
    protected void serializeData(ByteBuffer buffer) {
        buffer.put((byte) 0x00);
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
