package se.bitcraze.crazyfliecontrol;

import com.MobileAnarchy.Android.Widgets.Joystick.DualJoystickView;
import android.os.Bundle;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.view.Menu;

public class MainActivity extends Activity {

	DualJoystickView joysticks;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    
        this.joysticks = (DualJoystickView) findViewById(R.id.joysticks);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
}
