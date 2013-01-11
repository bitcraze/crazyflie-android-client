package se.bitcraze.crazyfliecontrol;

import struct.StructClass;
import struct.StructField;

@StructClass 
public class CommanderPacket {
	@StructField(order = 0)
	public byte port;
	@StructField(order = 1)
	public float pitch;
	@StructField(order = 2)
	public float roll;
	@StructField(order = 3)
	public float yaw;
	@StructField(order = 4)
	public short thrust;
};