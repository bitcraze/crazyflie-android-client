package se.bitcraze.crazyfliecontrol.controller;

import se.bitcraze.crazyfliecontrol.FlyingDataEvent;

public interface IController {

    public float getThrust();

    public float getRoll();

    public float getPitch();

    public float getYaw();

    public void enable();
    
    public void disable();

	public void setOnFlyingDataListener(FlyingDataEvent flyingDataListener);

}
