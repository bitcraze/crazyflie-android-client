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

import se.bitcraze.crazyfliecontrol.bootloader.Firmware.Asset;
import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

public class FirmwareDownloader {

    private static final String LOG_TAG = "FirmwareDownloader";
    private Context mContext;
    private String mDownloadDirectory = "CrazyflieControl";
    private List<Firmware> mFirmwares = new ArrayList<Firmware>();

    public FirmwareDownloader(Context context) {
        this.mContext = context;
    }

    public void checkForFirmwareUpdate() {
        String releaseURL = "https://api.github.com/repos/bitcraze/crazyflie-firmware/releases";
        new DownloadWebpageTask().execute(releaseURL);
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

        @Override
        protected void onPostExecute(String result) {
            ((BootloaderActivity) mContext).updateFirmwareSpinner(mFirmwares);
            ((BootloaderActivity) mContext).setStatusLine(result);
        }
    }

    public void downloadFirmware(Firmware selectedFirmware) {
        if (selectedFirmware != null && selectedFirmware.getAssets().size() > 0) {
            for (Asset asset : selectedFirmware.getAssets()) {
                //check if file is already downloaded
                if (isFileAlreadyDownloaded(asset)) {
                    Log.d(LOG_TAG, "File " + asset.getName() + " already downloaded.");
                } else {
                    String browserDownloadUrl = asset.getBrowserDownloadUrl();
                    downloadFile(browserDownloadUrl, asset.getName());
                }
            }
        } else {
            Log.d(LOG_TAG, "Selected firmware does not have assets.");
            ((BootloaderActivity) mContext).setStatusLine("Selected firmware does not have assets.");
            return;
        }
    }

    public boolean isFileAlreadyDownloaded (Asset asset) {
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
        DownloadManager manager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
        manager.enqueue(request);
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
            // Filter out firmwares without assets
            if (firmware.getAssets().size() > 0) {
                firmwares.add(firmware);
            } else {
                Log.d(LOG_TAG, "Firmware " + tagName + " was filtered out, because it has no assets.");
            }
        }
        return firmwares;
    }

}
