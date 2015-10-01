package se.bitcraze.crazyflielib.crazyflie;

public interface LinkListener {

    public void linkQualityUpdated(int percent);

    public void linkError(String msg);

}
