package se.bitcraze.crazyfliecontrol.bootloader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import se.bitcraze.crazyfliecontrol.bootloader.BootloaderActivity.Firmware.Asset;
import se.bitcraze.crazyfliecontrol2.R;
import se.bitcraze.crazyfliecontrol2.UsbLinkAndroid;
import se.bitcraze.crazyflielib.bootloader.Bootloader;
import se.bitcraze.crazyflielib.bootloader.Bootloader.BootloaderListener;
import se.bitcraze.crazyflielib.bootloader.Utilities.BootVersion;
import se.bitcraze.crazyflielib.crazyradio.RadioDriver;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class BootloaderActivity extends Activity {

    private static final String LOG_TAG = "Bootloader";
    private Button mCheckUpdateButton;
    private Button mFlashFirmwareButton;
    private Spinner mFirmwareSpinner;
    private ProgressBar mProgressBar;
    private TextView mStatusLineTextView;

    private Firmware mSelectedFirmware = null;
    private String mDownloadDirectory = "CrazyflieControl";

    private List<Firmware> mFirmwares = new ArrayList<Firmware>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bootloader);
        mCheckUpdateButton = (Button) findViewById(R.id.bootloader_checkUpdate);
        mFlashFirmwareButton = (Button) findViewById(R.id.bootloader_flashFirmware);
        mFirmwareSpinner = (Spinner) findViewById(R.id.bootloader_firmwareSpinner);
        mProgressBar = (ProgressBar) findViewById(R.id.bootloader_progressBar);
        mStatusLineTextView = (TextView) findViewById(R.id.bootloader_statusLine);

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
    }


    public void checkForFirmwareUpdate(View view) {
        String releaseURL = "https://api.github.com/repos/bitcraze/crazyflie-firmware/releases";
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            mStatusLineTextView.setText("Status: Checking for updates...");
            mCheckUpdateButton.setEnabled(false);
            new DownloadWebpageTask().execute(releaseURL);
        } else {
            mStatusLineTextView.setText("Status: No internet connection available.");
        }
    }

    private class DownloadWebpageTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {

            // params comes from the execute() call: params[0] is the url.
            try {
                String input = downloadUrl(urls[0]);
                mFirmwares = parseJson(input);

                return "Status: Found " + mFirmwares.size() + " firmwares.";
            } catch (IOException e) {
                return "Unable to retrieve web page. URL may be invalid.";
            } catch (JSONException e) {
                return "Error during parsing JSON content.";
            }
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            mCheckUpdateButton.setEnabled(true);
            if (!mFirmwares.isEmpty()) {
                mFirmwareSpinner.setVisibility(View.VISIBLE);
                ArrayAdapter<Firmware> dataAdapter = new ArrayAdapter<BootloaderActivity.Firmware>(BootloaderActivity.this, android.R.layout.simple_spinner_item, mFirmwares);
                dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                mFirmwareSpinner.setAdapter(dataAdapter);
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
            mStatusLineTextView.setText(result);
       }
    }

    private String downloadUrl(String myUrl) throws IOException {
        BufferedReader reader = null;
        StringBuilder builder = new StringBuilder();
        HttpClient client = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(myUrl);
        try {
            HttpResponse response = client.execute(httpGet);
            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();

            if (statusCode == 200) {
                HttpEntity entity = response.getEntity();
                InputStream content = entity.getContent();
                reader = new BufferedReader(new InputStreamReader(content, "UTF-8"));

                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
            } else {
                Log.d(LOG_TAG, "The response is: " + response);
                return "The response is: " + response;
            }
        } finally {
            // Makes sure that the InputStream is closed after the app is finished using it.
            if (reader != null) {
                reader.close();
            }
        }
        return builder.toString();
    }

    public List<Firmware> parseJson(String input) throws JSONException {

        List<Firmware> firmwares = new ArrayList<Firmware>();

        JSONArray releasesArray = new JSONArray(input);
        for (int i = 0; i < releasesArray.length(); i++) {
            JSONObject releaseObject = releasesArray.getJSONObject(i);
            String tagName = releaseObject.getString("tag_name");
            String name = releaseObject.getString("name");
            String createdAt = releaseObject.getString("created_at");
            Firmware firmware = new Firmware(tagName, name, createdAt);
            JSONArray assetsArray = releaseObject.getJSONArray("assets");
            if (assetsArray != null && assetsArray.length() > 0) {
                for (int n = 0; n < assetsArray.length(); n++) {
                    JSONObject assetsObject = assetsArray.getJSONObject(n);
                    String assetName = assetsObject.getString("name");
                    int size = assetsObject.getInt("size");
                    String downloadURL = assetsObject.getString("browser_download_url");
                    Asset asset = new Firmware().new Asset(assetName, size, downloadURL);
                    firmware.addAsset(asset);
                }
            }
            firmwares.add(firmware);
        }
        return firmwares;
    }

    public class Firmware {

        private String mTagName;
        private String mName;
        private String mCreatedAt;

        private List<Asset> mAssets = new ArrayList<Asset>();

        public Firmware() {
        }

        public Firmware(String tagName, String name, String createdAt) {
            this.mTagName = tagName;
            this.mName = name;
            this.mCreatedAt = createdAt;
        }

        public String getTagName() {
            return mTagName;
        }

        public String getName() {
            return mName;
        }

        public String getCreatedAt() {
            return mCreatedAt;
        }

        public List<Asset> getAssets() {
            return mAssets;
        }

        public void setAssets(List<Asset> mAssets) {
            this.mAssets = mAssets;
        }

        public void addAsset(Asset asset) {
            this.mAssets.add(asset);
        }

        @Override
        public String toString() {
            return mTagName + " (" + mCreatedAt + ")"; //used by spinner
        }

        public class Asset {

            private String mAssetName;
            private int mSize;
            private String mBrowserDownloadUrl;

            public Asset(String name, int size, String browserDownloadUrl) {
                this.mAssetName = name;
                this.mSize = size;
                this.mBrowserDownloadUrl = browserDownloadUrl;
            }

            public String getName() {
                return mAssetName;
            }

            public int getSize() {
                return mSize;
            }

            public String getBrowserDownloadUrl() {
                return mBrowserDownloadUrl;
            }

            public String getType() {
                if (mAssetName.startsWith("cf1") || mAssetName.startsWith("Crazyflie1")) {
                    return "cf1";
                } else if (mAssetName.startsWith("cf2") || mAssetName.startsWith("Crazyflie2")) {
                    return "cf2";
                } else {
                    return "unknown";
                }
            }
        }

    }


    public void flashFirmware(View view) {
        //TODO: enable wakelock

        // disable buttons and spinner
        mCheckUpdateButton.setEnabled(false);
        mFlashFirmwareButton.setEnabled(false);
        mFirmwareSpinner.setEnabled(false);

        mStatusLineTextView.setText("Downloading firmware...");

        if (this.mSelectedFirmware != null && mSelectedFirmware.getAssets().size() > 0) {
            for (Asset asset : this.mSelectedFirmware.getAssets()) {
                //check if file is already downloaded
                if (isFileAlreadyDownloaded(asset)) {
                    Log.d(LOG_TAG, "File " + asset.getName() + " already downloaded.");
                } else {
                    String browserDownloadUrl = asset.getBrowserDownloadUrl();
                    downloadFile(browserDownloadUrl, asset.getName());
                }
            }
        } else {
            mStatusLineTextView.setText("Selected firmware does not have assets.");
            return;
        }

        Toast.makeText(this, "Flashing firmware...", Toast.LENGTH_SHORT).show();

        new FlashFirmwareTask().execute();
    }

    private boolean isFileAlreadyDownloaded (Asset asset) {
        int assetSize = asset.getSize();
        File sdcard = Environment.getExternalStorageDirectory();
        File firmwareFile = new File(sdcard, mDownloadDirectory + "/" + asset.getName());
        return firmwareFile.exists() && firmwareFile.length() == assetSize;
    }

    public void downloadFile (String url, String fileName) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setDescription("Some description");
        request.setTitle(fileName);
        // in order for this if to run, you must use the android 3.2 to compile your app
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        }
        request.setDestinationInExternalPublicDir(mDownloadDirectory, fileName);

        // get download service and enqueue file
        DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        manager.enqueue(request);
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

                    //TODO: deal with Zip files
                    String fileName = null;
                    if (mSelectedFirmware != null && mSelectedFirmware.getAssets().size() > 0) {
                        for (Asset asset : mSelectedFirmware.getAssets()) {
                            if (cfType2 && "cf2".equals(asset.getType())) {
                                fileName = asset.getName();
                                break;
                            } else if (!cfType2 && "cf1".equals(asset.getType())) {
                                fileName = asset.getName();
                            }
                        }
                    }

                    File sdcard = Environment.getExternalStorageDirectory();
                    File firmwareFile = new File(sdcard, mDownloadDirectory + "/" + fileName);
                    if (firmwareFile == null || !firmwareFile.exists()) {
                        return "Problems with downloaded firmware files.";
                    }
                    long startTime = System.currentTimeMillis();
//                    bootloader.flash(firmwareFile, TargetTypes.STM32);
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
