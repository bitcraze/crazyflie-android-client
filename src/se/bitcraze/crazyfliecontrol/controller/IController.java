package se.bitcraze.crazyfliecontrol.controller;

public interface IController {

    public float getThrust();

    public float getRoll();

    public float getPitch();

    public float getYaw();

    public void enable();
    
    public void disable();

}
