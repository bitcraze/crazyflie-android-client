package se.bitcraze.crazyflie.lib.crtp;

import java.nio.ByteBuffer;

/**
 * Control mode where the height is sent as an absolute
 * setpoint (intended to be the distance to the surface
 * under the Crazflie).
 * 
 * Created by arnaud on 31/03/17.
 */

public class ZDistancePacket extends CrtpPacket {
    private final float mRoll;
    private final float mPitch;
    private final float mYawrate;
    private final float mZDistance;

    /**
     * Create a new commander packet.
     *
     * @param roll (Deg.)
     * @param pitch (Deg.)
     * @param yaw (Deg./s)
     * @param zDistance (m)
     */
    public ZDistancePacket(float roll, float pitch, float yaw, float zDistance) {
        super(0, CrtpPort.COMMANDER_GENERIC);

        this.mRoll = roll; // * (float)(Math.PI / 180.0);
        this.mPitch = pitch; // * (float)(Math.PI / 180.0);
        this.mYawrate = yaw; // * (float)(Math.PI / 180.0);
        this.mZDistance = zDistance;
    }

    @Override
    protected void serializeData(ByteBuffer buffer) {
        buffer.put((byte) 0x02);
        buffer.putFloat(mRoll);
        buffer.putFloat(-mPitch); //invert axis
        buffer.putFloat(mYawrate);
        buffer.putFloat(mZDistance);
    }

    @Override
    protected int getDataByteCount() {
        return 1 + 4 * 4; // 1 byte (type), 4 floats with size 4, 1 byte (type)
    }

    @Override
    public String toString() {
        return "zDistancePacket: roll: " + this.mRoll + " pitch: " + this.mPitch + " yawrate: " + this.mYawrate + " zDistance: " + this.mZDistance;
    }
}
