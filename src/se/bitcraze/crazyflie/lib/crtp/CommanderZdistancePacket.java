package se.bitcraze.crazyflie.lib.crtp;

import java.nio.ByteBuffer;

/**
 * Control mode where the height is sent as an absolute
 * setpoint (intended to be the distance to the surface
 * under the Crazflie).
 *
 * Roll, pitch, yawrate are defined as rad, rad, rad/s
 */
public class CommanderZdistancePacket extends CrtpPacket {

    public final static byte PACKET_TYPE_ZDISTANCE = 0x02;

    private final float mRoll;
    private final float mPitch;
    private final float mYaw;
    private final float mZdistance;

    public CommanderZdistancePacket(float roll, float pitch, float yaw, float zdistance) {
        super(0, CrtpPort.COMMANDER_GENERIC);
        this.mRoll = roll; // * (float)(Math.PI / 180.0);
        this.mPitch = pitch; // * (float)(Math.PI / 180.0);
        this.mYaw = yaw; // * (float)(Math.PI / 180.0);
        this.mZdistance = zdistance;
    }

    @Override
    protected void serializeData(ByteBuffer buffer) {
        buffer.put(PACKET_TYPE_ZDISTANCE);
        buffer.putFloat(mRoll);
        buffer.putFloat(-mPitch);
        buffer.putFloat(mYaw);
        buffer.putFloat(mZdistance);
    }

    @Override
    protected int getDataByteCount() {
        return 1 + 4 * 4; // 1 byte with size 1, 4 floats with size 4
    }

    @Override
    public String toString() {
        return "CommanderZdistancePacket: roll: " + this.mRoll + " pitch: " + this.mPitch + " yaw: " + this.mYaw + " zdistance: " + (int) this.mZdistance;
    }

}
