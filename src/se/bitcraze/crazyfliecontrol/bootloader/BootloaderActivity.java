package se.bitcraze.crazyfliecontrol.bootloader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import se.bitcraze.crazyfliecontrol.bootloader.Firmware.Asset;
import se.bitcraze.crazyfliecontrol2.R;
import se.bitcraze.crazyfliecontrol2.UsbLinkAndroid;
import se.bitcraze.crazyflielib.bootloader.Bootloader;
import se.bitcraze.crazyflielib.bootloader.Bootloader.BootloaderListener;
import se.bitcraze.crazyflielib.bootloader.Utilities.BootVersion;
import se.bitcraze.crazyflielib.crazyradio.RadioDriver;
import android.app.Activity;
import android.app.DownloadManager;
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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class BootloaderActivity extends Activity {

    private static final String LOG_TAG = "Bootloader";
    private ImageButton mCheckUpdateButton;
    private ImageButton mFlashFirmwareButton;
    private Spinner mFirmwareSpinner;
    private ProgressBar mProgressBar;
    private TextView mStatusLineTextView;

    private Firmware mSelectedFirmware = null;
    private FirmwareDownloader mFirmwareDownloader;

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

        mFirmwareDownloader = new FirmwareDownloader(this);

        this.registerReceiver(mFirmwareDownloader.onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(mFirmwareDownloader.onComplete);
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

    public void updateFirmwareSpinner(List<Firmware> firmwares) {
        mCheckUpdateButton.setEnabled(true);
        if (!firmwares.isEmpty()) {
            CustomSpinnerAdapter spinnerAdapter = new CustomSpinnerAdapter(BootloaderActivity.this, R.layout.spinner_rows, new ArrayList<Firmware>());
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mFirmwareSpinner.setAdapter(spinnerAdapter);
            mFirmwareSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    Firmware firmware = (Firmware) mFirmwareSpinner.getSelectedItem();
                    mSelectedFirmware = firmware;
                    mFlashFirmwareButton.setEnabled(true);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    mFlashFirmwareButton.setEnabled(false);
                }

            });
        } else {
            //TODO
        }
    }

    public void setStatusLine (String status) {
        this.mStatusLineTextView.setText(status);
    }

    public void flashFirmware(View view) {
        //TODO: enable wakelock

        // disable buttons and spinner
        mCheckUpdateButton.setEnabled(false);
        mFlashFirmwareButton.setEnabled(false);
        mFirmwareSpinner.setEnabled(false);

        // TODO: not visible
        mStatusLineTextView.setText("Downloading firmware...");

        mFirmwareDownloader.downloadFirmware(this.mSelectedFirmware);

        Toast.makeText(this, "Flashing firmware...", Toast.LENGTH_SHORT).show();
        new FlashFirmwareTask().execute();
    }



    private class FlashFirmwareTask extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... params) {
            Bootloader bootloader = null;
            try {
                bootloader = new Bootloader(new RadioDriver(new UsbLinkAndroid(BootloaderActivity.this)));

                bootloader.addBootloaderListener(new BootloaderListener() {

                    @Override
                    public void updateStatus(String status) {
                        publishProgress(new String[]{status, null, null});
                        Log.d(LOG_TAG, "Status: " + status);
                    }

                    @Override
                    public void updateProgress(int progress) {
                        publishProgress(new String[]{null, "" + progress, null});
                        Log.d(LOG_TAG, "Progress: " + progress);
                    }

                    @Override
                    public void updateError(String error) {
                        publishProgress(new String[]{null, null, error});
                        Log.d(LOG_TAG, "Error: " + error);
                    }
                });

                //mDetails.setText("Restart the Crazyflie you want to bootload in the next 10 seconds ...");
                if (bootloader.startBootloader(false)) {

                    //TODO: externalize
                    int protocolVersion = bootloader.getProtocolVersion();
                    boolean cfType2 = (protocolVersion == BootVersion.CF1_PROTO_VER_0 ||
                                        protocolVersion == BootVersion.CF1_PROTO_VER_1) ? false : true;

                    publishProgress(new String[]{"Found Crazyflie " + (cfType2 ? "2.0" : "1.0") + ".", null, null});
                    Log.d(LOG_TAG, "Found Crazyflie " + (cfType2 ? "2.0" : "1.0") + ".");

                    //TODO: deal with Zip files and manifest.json
                    Asset selectedAsset = null;
                    if (mSelectedFirmware != null && mSelectedFirmware.getAssets().size() > 0) {
                        for (Asset asset : mSelectedFirmware.getAssets()) {
                            if (cfType2 && "cf2".equals(asset.getType())) {
                                selectedAsset = asset;;
                                break;
                            } else if (!cfType2 && "cf1".equals(asset.getType())) {
                                selectedAsset = asset;;
                            }
                        }
                    }

                    File sdcard = Environment.getExternalStorageDirectory();
                    File firmwareFile = new File(sdcard, FirmwareDownloader.DOWNLOAD_DIRECTORY + "/" + mSelectedFirmware.getTagName() + "/" + selectedAsset.getName());

                    if (!mFirmwareDownloader.isFileAlreadyDownloaded(selectedAsset, mSelectedFirmware.getTagName())) {
                        return "Problem with downloaded firmware files.";
                    }

                    long startTime = System.currentTimeMillis();
                    bootloader.flash(firmwareFile, (String[]) null);
                    String flashTime = "Flashing took " + (System.currentTimeMillis() - startTime)/1000 + " seconds.";
                    Log.d(LOG_TAG, flashTime);
                    bootloader.resetToFirmware();
                    return flashTime;
                } else {
                    return "Bootloader problem.";
                }
            } catch (IOException e) {
                return "Bootloader problem: " + e.getMessage();
            } catch (IllegalArgumentException iae) {
                return "Bootloader problem: " + iae.getMessage();
            } finally {
                if (bootloader != null) {
                    bootloader.close();
                }
            }
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            if (progress[0] != null) {
                mStatusLineTextView.setText("Status: " + progress[0]);
            } else if (progress[1] != null) {
                mProgressBar.setProgress(Integer.parseInt(progress[1]));
            } else if (progress[2] != null) {
                mStatusLineTextView.setText("Status: " + progress[2]);
            }
        }

        @Override
        protected void onPostExecute(String result) {
            mStatusLineTextView.setText("Status: " + result);
            mCheckUpdateButton.setEnabled(true);
            mFlashFirmwareButton.setEnabled(true);
            mFirmwareSpinner.setEnabled(true);
        }
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
