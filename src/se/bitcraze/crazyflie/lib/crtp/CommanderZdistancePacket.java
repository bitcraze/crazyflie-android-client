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

    private final byte mType;
    private final float mRoll;
    private final float mPitch;
    private final float mYaw;
    private final float mZdistance;

    public CommanderZdistancePacket(float roll, float pitch, float yawrate, float zdistance) {
        super(0, CrtpPort.COMMANDER_GENERIC);
        this.mType = CrtpPacket.TYPE_ZDISTANCE;
        this.mRoll = roll;
        this.mPitch = pitch;
        this.mYaw = yawrate;
        this.mZdistance = zdistance;
    }

    @Override
    protected void serializeData(ByteBuffer buffer) {
        buffer.put(mType);
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
        return "CommanderZdistancePacket: roll: " + this.mRoll + " pitch: " + this.mPitch + " yaw: " + this.mYaw + " zdistance: " + this.mZdistance;
    }

}
