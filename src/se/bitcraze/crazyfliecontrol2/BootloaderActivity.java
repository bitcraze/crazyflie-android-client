package se.bitcraze.crazyfliecontrol2;

import java.io.BufferedReader;
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

import se.bitcraze.crazyfliecontrol2.BootloaderActivity.Firmware.Asset;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class BootloaderActivity extends Activity {

    private static final String LOG_TAG = "Bootloader";
    private TextView statusLine;
    private Spinner firmwareSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bootloader);
        statusLine = (TextView) findViewById(R.id.statusLine);
        firmwareSpinner = (Spinner) findViewById(R.id.firmwareSpinner);

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
    }

    public void checkForFirmwareUpdate(View view) {
        String releaseURL = "https://api.github.com/repos/bitcraze/crazyflie-firmware/releases";
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            statusLine.setText("Status: Checking for updates...");
            new DownloadWebpageTask().execute(releaseURL);
        } else {
            statusLine.setText("Status: No internet connection available.");
        }
    }

    private class DownloadWebpageTask extends AsyncTask<String, Void, String> {
        List<Firmware> firmwares = new ArrayList<Firmware>();

        @Override
        protected String doInBackground(String... urls) {

            // params comes from the execute() call: params[0] is the url.
            try {
                String input = downloadUrl(urls[0]);
                firmwares = parseJson(input);

                return "Status: Found " + firmwares.size() + " firmwares.";
            } catch (IOException e) {
                return "Unable to retrieve web page. URL may be invalid.";
            } catch (JSONException e) {
                return "Error during parsing JSON content.";
            }
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            if (!firmwares.isEmpty()) {
                ArrayAdapter<Firmware> dataAdapter = new ArrayAdapter<BootloaderActivity.Firmware>(BootloaderActivity.this, android.R.layout.simple_spinner_item, firmwares);
                dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                firmwareSpinner.setAdapter(dataAdapter);
                firmwareSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        // TODO Auto-generated method stub
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }

                });
            }
            statusLine.setText(result);
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
        Toast.makeText(this, "Flashing firmware...", Toast.LENGTH_SHORT).show();
    }
}
