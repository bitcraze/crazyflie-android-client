/**
 * 
 */

package se.bitcraze.crazyflielib.crtp;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Packet containing console text from the Crazyflie.
 */
public class ConsolePacket extends CrtpPacket {

    public static final int PORT = 0;
    public static final Charset CHARSET = Charset.forName("US-ASCII");

    private final String mText;

    public ConsolePacket(String text) {
        super(0, PORT);
        this.mText = text;
    }

    /**
     * Get the text contained in the packet.
     * 
     * @return the text
     */
    public String getText() {
        return mText;
    }

    /*
     * (non-Javadoc)
     * @see
     * se.bitcraze.crazyflielib.crtp.CRTPPacket#serializeData(java.nio.ByteBuffer
     * )
     */
    @Override
    protected void serializeData(ByteBuffer buffer) {
        buffer.put(mText.getBytes(CHARSET));
    }

    /*
     * (non-Javadoc)
     * @see se.bitcraze.crazyflielib.crtp.CRTPPacket#getDataByteCount()
     */
    @Override
    protected int getDataByteCount() {
        return mText.getBytes(CHARSET).length;
    }

    /**
     * Construct a console packet using given data.
     * 
     * @param data the data (must not include the CRTP header)
     * @return parsed console packet
     */
    public static ConsolePacket parse(byte[] data) {
        return new ConsolePacket(new String(data, CHARSET));
    }

}
