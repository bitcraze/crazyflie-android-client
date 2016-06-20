/**
 *    ||          ____  _ __
 * +------+      / __ )(_) /_______________ _____  ___
 * | 0xBC |     / __  / / __/ ___/ ___/ __ `/_  / / _ \
 * +------+    / /_/ / / /_/ /__/ /  / /_/ / / /_/  __/
 *  ||  ||    /_____/_/\__/\___/_/   \__,_/ /___/\___/
 *
 * Copyright (C) 2015 Bitcraze AB
 *
 * Crazyflie Nano Quadcopter Client
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */

package se.bitcraze.crazyfliecontrol.bootloader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import se.bitcraze.crazyflie.lib.bootloader.Bootloader;
import se.bitcraze.crazyflie.lib.bootloader.Bootloader.BootloaderListener;
import se.bitcraze.crazyflie.lib.bootloader.Utilities.BootVersion;
import se.bitcraze.crazyflie.lib.crazyradio.RadioDriver;
import se.bitcraze.crazyfliecontrol.bootloader.FirmwareDownloader.FirmwareDownloadListener;
import se.bitcraze.crazyfliecontrol2.MainActivity;
import se.bitcraze.crazyfliecontrol2.R;
import se.bitcraze.crazyfliecontrol2.UsbLinkAndroid;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class BootloaderActivity extends Activity {

    private static final String LOG_TAG = "BootloaderActivity";
    public final static String BOOTLOADER_DIR = "bootloader";

    private Button mFlashFirmwareButton;
    private ImageButton mReleaseNotesButton;
    private Spinner mFirmwareSpinner;
    private CustomSpinnerAdapter mSpinnerAdapter;
    private ScrollView mScrollView;
    private TextView mConsoleTextView;
    private ProgressBar mProgressBar;

    private Firmware mSelectedFirmware = null;
    private FirmwareDownloader mFirmwareDownloader;
    private Bootloader mBootloader;
    private FlashFirmwareTask mFlashFirmwareTask;
    private boolean mDoubleBackToExitPressedOnce = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bootloader);
        mFlashFirmwareButton = (Button) findViewById(R.id.bootloader_flashFirmware);
        mReleaseNotesButton = (ImageButton) findViewById(R.id.bootloader_releaseNotes);
        mFirmwareSpinner = (Spinner) findViewById(R.id.bootloader_firmwareSpinner);
        mScrollView = (ScrollView) findViewById(R.id.bootloader_scrollView);
        mConsoleTextView = (TextView) findViewById(R.id.bootloader_statusLine);
        mProgressBar = (ProgressBar) findViewById(R.id.bootloader_progressBar);

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
        checkForFirmwareUpdate(getCurrentFocus());
    }

    @Override
    protected void onPause() {
        //TODO: improve
        //TODO: why does resetToFirmware not work?
        if (mFlashFirmwareTask != null && mFlashFirmwareTask.getStatus().equals(Status.RUNNING)) {
            Log.d(LOG_TAG, "OnPause: stop bootloader.");
            mFlashFirmwareTask.cancel(true);
        }
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (mFlashFirmwareTask != null && mFlashFirmwareTask.getStatus().equals(Status.RUNNING)) {
            if (mDoubleBackToExitPressedOnce) {
                super.onBackPressed();
                return;
            }
            this.mDoubleBackToExitPressedOnce = true;
            Toast.makeText(this, "Please click BACK again to cancel flashing and exit", Toast.LENGTH_SHORT).show();
            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    mDoubleBackToExitPressedOnce = false;

                }
            }, 2000);
        } else {
            super.onBackPressed();
        }
    }

    public void showReleaseNotes(View view) {
        if (mSelectedFirmware != null && mSelectedFirmware.getReleaseNotes() != null) {
            final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle("Release notes:");
            alertDialog.setMessage(mSelectedFirmware.getReleaseNotes());
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    alertDialog.dismiss();
                }
            });
            alertDialog.show();
        }
    }

    public void checkForFirmwareUpdate(View view) {
        mFirmwareDownloader.checkForFirmwareUpdate();
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
                    mReleaseNotesButton.setEnabled(true);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mFlashFirmwareButton.setEnabled(false);
                mReleaseNotesButton.setEnabled(false);
            }

        });
    }

    public void updateFirmwareSpinner(List<Firmware> firmwares) {
        mSpinnerAdapter.clear();
        Collections.sort(firmwares);
        Collections.reverse(firmwares);
        mSpinnerAdapter.addAll(firmwares);
    }

    public void appendConsole(String status) {
        Log.d(LOG_TAG, status);
        this.mConsoleTextView.append("\n" + status);
        mScrollView.post(new Runnable() {
            @Override
            public void run() {
                mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    public void appendConsoleError(String status) {
        Log.e(LOG_TAG, status);
        int start = this.mConsoleTextView.getText().length();
        this.mConsoleTextView.append("\n" + status);
        int end = this.mConsoleTextView.getText().length();
        Spannable spannableText = (Spannable) this.mConsoleTextView.getText();
        spannableText.setSpan(new ForegroundColorSpan(Color.RED), start, end, 0);
        mScrollView.post(new Runnable() {
            @Override
            public void run() {
                mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    private FirmwareDownloadListener mDownloadListener = new FirmwareDownloadListener () {
        public void downloadFinished() {
            //flash firmware once firmware is downloaded
            appendConsole("Firmware downloaded.");
            startBootloader();
        }

        public void downloadProblem(String msg) {
            //flash firmware once firmware is downloaded
            appendConsole("Firmware download failed: " + msg);
            stopFlashProcess(false);
        }
    };

    public void startFlashProcess(final View view) {
        // disable button and spinner
        mFlashFirmwareButton.setEnabled(false);
        mReleaseNotesButton.setEnabled(false);
        mFirmwareSpinner.setEnabled(false);

        //clear console
        mConsoleTextView.setText("");

        // download firmware file
        appendConsole("Downloading firmware...");

        mFirmwareDownloader.addDownloadListener(mDownloadListener);
        mFirmwareDownloader.downloadFirmware(this.mSelectedFirmware);
    }

    private void startBootloader() {

        if (!mFirmwareDownloader.isFileAlreadyDownloaded(mSelectedFirmware.getTagName() + "/" + mSelectedFirmware.getAssetName())) {
            appendConsoleError("Firmware file can not be found.");
            stopFlashProcess(false);
            return;
        }

        try {
            //fail quickly, when Crazyradio is not connected
            //TODO: fix when BLE is used as well
            //TODO: extract this to RadioDriver class?
            if (!MainActivity.isCrazyradioAvailable(this)) {
                appendConsoleError("Please make sure that a Crazyradio (PA) is connected.");
                stopFlashProcess(false);
                return;
            }
            mBootloader = new Bootloader(new RadioDriver(new UsbLinkAndroid(BootloaderActivity.this)));
        } catch (IOException ioe) {
            appendConsoleError(ioe.getMessage());
            stopFlashProcess(false);
            return;
        } catch (IllegalArgumentException iae) {
            appendConsoleError(iae.getMessage());
            stopFlashProcess(false);
            return;
        }

        new AsyncTask<Void, Void, Boolean>() {

            private ProgressDialog mProgress;

            @Override
            protected void onPreExecute() {
                mProgress = ProgressDialog.show(BootloaderActivity.this, "Searching Crazyflie in bootloader mode...", "Restart the Crazyflie you want to bootload in the next 10 seconds ...", true, false);
            }

            @Override
            protected Boolean doInBackground(Void... arg0) {
                return mBootloader.startBootloader(false);
            }

            @Override
            protected void onPostExecute(Boolean result) {
                mProgress.dismiss();
                if (!result) {
                    appendConsoleError("No Crazyflie found in bootloader mode.");
                    stopFlashProcess(false);
                    return;
                }
                flashFirmware();
            }
        }.execute();
    }

    //TODO: simplify
    public void flashFirmware() {
        //TODO: externalize
        //Check if firmware is compatible with Crazyflie
        int protocolVersion = mBootloader.getProtocolVersion();
        boolean cfType2 = !(protocolVersion == BootVersion.CF1_PROTO_VER_0 ||
                            protocolVersion == BootVersion.CF1_PROTO_VER_1);

        String cfversion = "Found Crazyflie " + (cfType2 ? "2.0" : "1.0") + ".";
        appendConsole(cfversion);

        // check if firmware and CF are compatible
        if (("CF2".equalsIgnoreCase(mSelectedFirmware.getType()) && !cfType2) ||
            ("CF1".equalsIgnoreCase(mSelectedFirmware.getType()) && cfType2)) {
            appendConsoleError("Incompatible firmware version.");
            stopFlashProcess(false);
            return;
        }

        //keep the screen on during flashing
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mFlashFirmwareTask = new FlashFirmwareTask();
        mFlashFirmwareTask.execute();
        //TODO: wait for finished task
    }

    private class FlashFirmwareTask extends AsyncTask<String, String, String> {

        boolean flashSuccessful;

        @Override
        protected void onPreExecute() {
            Toast.makeText(BootloaderActivity.this, "Flashing firmware ...", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected String doInBackground(String... params) {

            BootloaderListener bootloaderListener = new BootloaderListener() {

                @Override
                public void updateStatus(String status) {
                    publishProgress(status, null, null, null);
                }

                @Override
                public void updateProgress(int progress, int max) {
                    publishProgress(null, "" + progress, "" + max, null);
                    if (isCancelled()) {
                        mBootloader.cancel();
                    }
                }

                @Override
                public void updateError(String error) {
                    publishProgress(null, null, null, error);
                }
            };
            mBootloader.addBootloaderListener(bootloaderListener);

            File bootloaderDir = new File(getApplicationContext().getExternalFilesDir(null), BOOTLOADER_DIR);
            File firmwareFile = new File(bootloaderDir, mSelectedFirmware.getTagName() + "/" + mSelectedFirmware.getAssetName());

            long startTime = System.currentTimeMillis();
            try {
                flashSuccessful = mBootloader.flash(firmwareFile);
            } catch (IOException ioe) {
                Log.e(LOG_TAG, ioe.getMessage());
                flashSuccessful = false;
            }
            mBootloader.removeBootloaderListener(bootloaderListener);
            String flashTime = "Flashing took " + (System.currentTimeMillis() - startTime)/1000 + " seconds.";
            Log.d(LOG_TAG, flashTime);
            return flashSuccessful ? ("Flashing successful. " + flashTime) : "Flashing not successful.";
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            if (progress[0] != null) {
                appendConsole(progress[0]);
            } else if (progress[1] != null && progress[2] != null) {
                mProgressBar.setProgress(Integer.parseInt(progress[1]));
                // TODO: progress bar max is reset when activity is resumed
                mProgressBar.setMax(Integer.parseInt(progress[2]));
            } else if (progress[3] != null) {
                appendConsole(progress[3]);
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (flashSuccessful) {
                appendConsole(result);
            } else {
                appendConsoleError(result);
            }
            stopFlashProcess(true);
        }

        @Override
        protected void onCancelled(String result) {
            stopFlashProcess(false);
        }

    }

    private void stopFlashProcess(boolean reset) {
        if (reset) {
            String resetMsg = "Resetting Crazyflie to firmware mode...";
            appendConsole(resetMsg);
            Toast.makeText(BootloaderActivity.this, resetMsg, Toast.LENGTH_SHORT).show();
            mBootloader.resetToFirmware();
        }
        if (mBootloader != null) {
            mBootloader.close();
        }
        mFirmwareDownloader.removeDownloadListener(mDownloadListener);
        //re-enable widgets
        mFlashFirmwareButton.setEnabled(true);
        mReleaseNotesButton.setEnabled(true);
        mFirmwareSpinner.setEnabled(true);

        mProgressBar.setProgress(0);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    /**
     * @param context used to check the device version and DownloadManager information
     * @return true if the download manager is available
     */
    public static boolean isDownloadManagerAvailable(Context context) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
    }
}
