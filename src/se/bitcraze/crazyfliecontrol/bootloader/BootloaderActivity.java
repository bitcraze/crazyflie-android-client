package se.bitcraze.crazyfliecontrol.bootloader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import se.bitcraze.crazyfliecontrol.bootloader.FirmwareDownloader.FirmwareDownloadListener;
import se.bitcraze.crazyfliecontrol2.R;
import se.bitcraze.crazyfliecontrol2.UsbLinkAndroid;
import se.bitcraze.crazyflielib.bootloader.Bootloader;
import se.bitcraze.crazyflielib.bootloader.Bootloader.BootloaderListener;
import se.bitcraze.crazyflielib.bootloader.Utilities.BootVersion;
import se.bitcraze.crazyflielib.crazyradio.RadioDriver;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class BootloaderActivity extends Activity {

    private static final String LOG_TAG = "BootloaderActivity";
    private ImageButton mCheckUpdateButton;
    private ImageButton mFlashFirmwareButton;
    private Spinner mFirmwareSpinner;
    private CustomSpinnerAdapter mSpinnerAdapter;
    private ProgressBar mProgressBar;
    private TextView mStatusLineTextView;

    private Firmware mSelectedFirmware = null;
    private FirmwareDownloader mFirmwareDownloader;
    private Bootloader mBootloader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bootloader);
        mCheckUpdateButton = (ImageButton) findViewById(R.id.bootloader_checkUpdate);
        mFlashFirmwareButton = (ImageButton) findViewById(R.id.bootloader_flashFirmware);
        mFirmwareSpinner = (Spinner) findViewById(R.id.bootloader_firmwareSpinner);
        mProgressBar = (ProgressBar) findViewById(R.id.bootloader_progressBar);
        mStatusLineTextView = (TextView) findViewById(R.id.bootloader_statusLine);

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

        initializeFirmwareSpinner();
        mFirmwareDownloader = new FirmwareDownloader(this);

        this.registerReceiver(mFirmwareDownloader.onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(mFirmwareDownloader.onComplete);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mFirmwareDownloader.isFileAlreadyDownloaded(FirmwareDownloader.RELEASES_JSON)) {
            mFirmwareDownloader.checkForFirmwareUpdate();
        } else {
            mFlashFirmwareButton.setEnabled(false);
            //TODO: force update of spinner adapter even though firmware list is empty
        }
    }

    public void checkForFirmwareUpdate(View view) {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            mStatusLineTextView.setText("Status: Checking for updates...");
            mCheckUpdateButton.setEnabled(false);
            mFirmwareDownloader.checkForFirmwareUpdate();
        } else {
            mStatusLineTextView.setText("Status: No internet connection available.");
        }
    }

    private void initializeFirmwareSpinner() {
        mSpinnerAdapter = new CustomSpinnerAdapter(BootloaderActivity.this, R.layout.spinner_rows, new ArrayList<Firmware>());
        mSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mFirmwareSpinner.setAdapter(mSpinnerAdapter);
        mFirmwareSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Firmware firmware = (Firmware) mFirmwareSpinner.getSelectedItem();
                if (firmware != null) {
                    mSelectedFirmware = firmware;
                    mFlashFirmwareButton.setEnabled(true);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mFlashFirmwareButton.setEnabled(false);
            }

        });
    }

    public void updateFirmwareSpinner(List<Firmware> firmwares) {
        mCheckUpdateButton.setEnabled(true);
        mSpinnerAdapter.clear();
        mSpinnerAdapter.addAll(firmwares);
    }

    public void setStatusLine (String status) {
        this.mStatusLineTextView.setText(status);
    }

    private FirmwareDownloadListener mDownloadListener = new FirmwareDownloadListener () {
        public void downloadFinished() {
            //flash firmware once firmware is downloaded
            mStatusLineTextView.setText("Firmware downloaded.");
            startBootloader();
        }
    };

    public void startFlashProcess(final View view) {
        //TODO: enable wakelock

        // disable buttons and spinner
        mCheckUpdateButton.setEnabled(false);
        mFlashFirmwareButton.setEnabled(false);
        mFirmwareSpinner.setEnabled(false);

        // download firmware file

        // TODO: not visible
        mStatusLineTextView.setText("Downloading firmware...");

        mFirmwareDownloader.addDownloadListener(mDownloadListener);
        mFirmwareDownloader.downloadFirmware(this.mSelectedFirmware);
    }

    private void startBootloader() {

        try {
            mBootloader = new Bootloader(new RadioDriver(new UsbLinkAndroid(BootloaderActivity.this)));
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage());
            return;
        }

        new AsyncTask<Void, Void, Boolean>() {

            private ProgressDialog mProgress;

            @Override
            protected void onPreExecute() {
                mProgress = ProgressDialog.show(BootloaderActivity.this, "Start bootloader", "Searching for Crazyflie in bootloader mode...", true, false);
            }

            @Override
            protected Boolean doInBackground(Void... arg0) {
                return mBootloader.startBootloader(false);
            }

            @Override
            protected void onPostExecute(Boolean result) {
                mProgress.dismiss();
                if (!result) {
                    Toast.makeText(BootloaderActivity.this, "No Crazyflie found in bootloader mode.", Toast.LENGTH_SHORT).show();
                    reenableWidgets();
                    mFirmwareDownloader.removeDownloadListener(mDownloadListener);
                    return;
                }
                flashFirmware();
            }
        }.execute();
    }

    //TODO: simplify
    //TODO: mStatusLineTextView.setText("Status: Restart the Crazyflie you want to bootload in the next 10 seconds ...");
    public void flashFirmware() {
        //TODO: externalize
        //Check if firmware is compatible with Crazyflie
        int protocolVersion = mBootloader.getProtocolVersion();
        boolean cfType2 = (protocolVersion == BootVersion.CF1_PROTO_VER_0 ||
                            protocolVersion == BootVersion.CF1_PROTO_VER_1) ? false : true;

        String cfversion = "Found Crazyflie " + (cfType2 ? "2.0" : "1.0") + ".";
        mStatusLineTextView.setText(cfversion);
        Log.d(LOG_TAG, cfversion);

        if (("CF2".equalsIgnoreCase(mSelectedFirmware.getType()) && !cfType2) ||
            ("CF1".equalsIgnoreCase(mSelectedFirmware.getType()) && cfType2)) {
            mBootloader.resetToFirmware();
            Log.d(LOG_TAG, "Incompatible firmware version.");
            mStatusLineTextView.setText("Status: Incompatible firmware version.");
            reenableWidgets();
            return;
        }

        if (!mFirmwareDownloader.isFileAlreadyDownloaded(mSelectedFirmware.getTagName() + "/" + mSelectedFirmware.getAssetName())) {
            mStatusLineTextView.setText("Status: Firmware file can not be found.");
            reenableWidgets();
            return;
        }

        //keep the screen on during flashing
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        new FlashFirmwareTask().execute();
        //TODO: wait for finished task
    }

    private class FlashFirmwareTask extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {
            Toast.makeText(BootloaderActivity.this, "Flashing firmware ...", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected String doInBackground(String... params) {

            mBootloader.addBootloaderListener(new BootloaderListener() {

                @Override
                public void updateStatus(String status) {
                    publishProgress(new String[]{status, null, null, null});
                }

                @Override
                public void updateProgress(int progress, int max) {
                    publishProgress(new String[]{null, "" + progress, "" + max,  null});
                }

                @Override
                public void updateError(String error) {
                    publishProgress(new String[]{null, null, null, error});
                }
            });

            File sdcard = Environment.getExternalStorageDirectory();
            File firmwareFile = new File(sdcard, FirmwareDownloader.DOWNLOAD_DIRECTORY + "/" + mSelectedFirmware.getTagName() + "/" + mSelectedFirmware.getAssetName());

            long startTime = System.currentTimeMillis();
            //TODO: fix for NRF51 files
            mBootloader.flash(firmwareFile, "stm32");
            String flashTime = "Flashing took " + (System.currentTimeMillis() - startTime)/1000 + " seconds.";
            Log.d(LOG_TAG, flashTime);
            return flashTime;
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            if (progress[0] != null) {
                mStatusLineTextView.setText("Status: " + progress[0]);
            } else if (progress[1] != null && progress[2] != null) {
                mProgressBar.setProgress(Integer.parseInt(progress[1]));
                // TODO: progress bar max is reset when activity is resumed
                mProgressBar.setMax(Integer.parseInt(progress[2]));
                Log.d(LOG_TAG, "setMax: " + Integer.parseInt(progress[2]));
            } else if (progress[3] != null) {
                mStatusLineTextView.setText("Status: " + progress[3]);
            }
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(BootloaderActivity.this, "Resetting Crazyflie to firmware mode...", Toast.LENGTH_SHORT).show();
            mBootloader.resetToFirmware();
            if (mBootloader != null) {
                mBootloader.close();
            }
            reenableWidgets();
            mProgressBar.setProgress(0);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    public void reenableWidgets() {
        mCheckUpdateButton.setEnabled(true);
        mFlashFirmwareButton.setEnabled(true);
        mFirmwareSpinner.setEnabled(true);
    }

    /**
     * @param context used to check the device version and DownloadManager information
     * @return true if the download manager is available
     */
    public static boolean isDownloadManagerAvailable(Context context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return true;
        }
        return false;
    }
}
