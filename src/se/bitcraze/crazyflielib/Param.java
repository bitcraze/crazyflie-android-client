package se.bitcraze.crazyflielib;

import se.bitcraze.crazyflielib.crtp.HoverParamPacket;

public class Param {
	CrazyradioLink cf;
	public Param(CrazyradioLink cf) {
		this.cf = cf;
	}
	
	public void setHoverMode(boolean enable ) {
		if (cf.isConnected())
			cf.send(new HoverParamPacket(enable));	
	}
}
