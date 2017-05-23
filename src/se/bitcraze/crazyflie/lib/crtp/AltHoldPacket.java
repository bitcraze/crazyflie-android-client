package se.bitcraze.crazyflie.lib.crtp;

import java.nio.ByteBuffer;

/**
 * Created by arnaud on 23/05/17.
 */

public class AltHoldPacket extends CrtpPacket {
    private final float mRoll;
    private final float mPitch;
    private final float mYawrate;
    private final float mZVelocity;

    /**
     * Create a new commander packet.
     *
     * @param roll (Deg.)
     * @param pitch (Deg.)
     * @param yaw (Deg./s)
     * @param zVelocity (m/s)
     */
    public AltHoldPacket(float roll, float pitch, float yaw, float zVelocity) {
        super(0, CrtpPort.GENERIC_COMMANDER);

        this.mRoll = roll; // * (float)(Math.PI / 180.0);
        this.mPitch = pitch; // * (float)(Math.PI / 180.0);
        this.mYawrate = yaw; // * (float)(Math.PI / 180.0);
        this.mZVelocity = zVelocity;
    }

    @Override
    protected void serializeData(ByteBuffer buffer) {
        buffer.put((byte)0x04);
        buffer.putFloat(mRoll);
        buffer.putFloat(-mPitch); //invert axis
        buffer.putFloat(mYawrate);
        buffer.putFloat(mZVelocity);
    }

    @Override
    protected int getDataByteCount() {
        return 4 * 4 + 1; // 4 floats with size 4, 1 byte (type)
    }

    @Override
    public String toString() {
        return "ZDistancePacket: roll: " + this.mRoll + " pitch: " + this.mPitch + " yawrate: " + this.mYawrate + " zVelocity: " + (int) this.mZVelocity;
    }
}
