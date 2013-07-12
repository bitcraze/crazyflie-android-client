package se.bitcraze.crazyflielib.crtp;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Packet of data which can be sent/received from/to the Crazyflie.
 */
public abstract class CRTPPacket {
	
	public static final ByteOrder BYTE_ORDER = ByteOrder.LITTLE_ENDIAN;
	
	public static final CRTPPacket NULL_PACKET = new CRTPPacket((byte) 0xff) {
			@Override
			protected void serializeData(ByteBuffer buffer) {}
			
			@Override
			protected int getDataByteCount() {
				return 0;
			}
		};
	
	private final byte mPacketHeader;
	private byte[] mSerializedPacket;
	
	public CRTPPacket(int channel, int port) {
		this((byte)( ((port & 0x0F) << 4) | (channel & 0x03) ));
	}
	
	public CRTPPacket(byte packetHeader) {
		this.mPacketHeader = packetHeader;
		this.mSerializedPacket = null;
	}
	
	public byte getHeader() {
		return mPacketHeader;
	}
	
	protected abstract void serializeData(ByteBuffer buffer);
	
	protected abstract int getDataByteCount();
	
	public byte[] toByteArray() {
		// if it's the first call, serialize the packet and cache it
		if( mSerializedPacket == null ) {
			ByteBuffer buffer = ByteBuffer.allocate(getDataByteCount() + 1).order(BYTE_ORDER);
			buffer.put(mPacketHeader);
			serializeData(buffer);
			mSerializedPacket = buffer.array();
		}
		
		return mSerializedPacket;
	}
}
