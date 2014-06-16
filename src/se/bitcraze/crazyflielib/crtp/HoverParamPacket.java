package se.bitcraze.crazyflielib.crtp;

import java.nio.ByteBuffer;


public class HoverParamPacket extends CrtpPacket{
	/**
	 *Possible states
	 */
	public final static int IDLE = 0;
	public final static int WAIT_TOC = 1;
	public final static int WAIT_READ = 2;
	public final static int WAIT_WRITE = 3;

	public final static int TOC_CHANNEL = 0;
	public final static int READ_CHANNEL = 1;
	public final static int WRITE_CHANNEL = 2;
	
	public final static int PORT = 0x02;
	
	private byte isEnabled;
		
	public HoverParamPacket(boolean isEnabled) {
		super(WRITE_CHANNEL, PORT);
		if(isEnabled) {
			this.isEnabled=(byte) 0x01;
		} else {
			this.isEnabled=(byte) 0x00;
		}				
	}

	@Override
	protected void serializeData(ByteBuffer buffer) {
		buffer.put((byte) 10); //id on uint8_t
		buffer.put(isEnabled);		
	}

	@Override
	protected int getDataByteCount() {		
		return 2; //two byte
	}
	
    public static HoverParamPacket parse(byte[] data) {
    	if(data[1] == 1) {
    		return new HoverParamPacket(true);
    	} else {
    		return new HoverParamPacket(false);
    	}
    }
	
}