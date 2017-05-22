package se.bitcraze.crazyflie.lib.crtp;

import java.nio.ByteBuffer;

/**
 * Created by arnaud on 31/03/17.
 */

public class StopPacket extends CrtpPacket {
    /**
     * Create a new commander packet.
     *
     * @param roll (Deg.)
     * @param pitch (Deg.)
     * @param yaw (Deg./s)
     * @param thrust (0-65535)
     * @param clientXmode if true, then roll and pitch values are recalculated before sending them to the Crazyflie
     */
    public StopPacket() {
        super(0, CrtpPort.GENERIC_COMMANDER);
    }

    @Override
    protected void serializeData(ByteBuffer buffer) {
        buffer.put((byte)0x00);
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
