package se.bitcraze.crazyfliecontrol;

import android.content.Context;
import android.hardware.usb.UsbManager;

public class RadioLink implements Runnable {

	private boolean stop = false;
	private int channel = 10;
	
	public void stop() {
		this.stop  = true;
	}
	
	public int getChannel() {
		return channel;
	}

	public void setChannel(int channel) {
		this.channel = channel;
	}
	
	//Run the Radio link loop to send attitude setpoint to the coper
	public void run() {		
		while (!stop) {
			;
		}
	}
}
