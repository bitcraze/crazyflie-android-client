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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.DownloadManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;

import javax.net.ssl.HttpsURLConnection;

import se.bitcraze.crazyflie.lib.bootloader.Bootloader;
import se.bitcraze.crazyflie.lib.bootloader.FirmwareRelease;

public class FirmwareDownloader {

    private static final String LOG_TAG = "FirmwareDownloader";

    private static final String RELEASES_JSON = "cf_releases.json";
    private static final String RELEASE_URL = "https://api.github.com/repos/bitcraze/crazyflie-release/releases";

    private Context mContext;
    private final File mBootloaderDir;
    private List<FirmwareRelease> mFirmwareReleases = new ArrayList<FirmwareRelease>();
    private long mDownloadReference = -42;
    private DownloadManager mManager;
    private AsyncTask mDownloadTask;

    public FirmwareDownloader(Context context) {
        this.mContext = context;
        this.mBootloaderDir = new File(mContext.getExternalFilesDir(null), BootloaderActivity.BOOTLOADER_DIR);
    }

    public void checkForFirmwareUpdate() {
        File releasesFile = new File(mBootloaderDir, RELEASES_JSON);
        mBootloaderDir.mkdirs();

        if (isNetworkAvailable()) {
            Log.d(LOG_TAG, "Network connection available.");
            if (!isFileAlreadyDownloaded(RELEASES_JSON) || isFileTooOld(releasesFile, 21600000)) {
                ((BootloaderActivity) mContext).appendConsole("Checking for updates...");
                new DownloadWebpageTask().execute(RELEASE_URL);
            } else {
                loadLocalFile(releasesFile);
            }
        } else {
            Log.d(LOG_TAG, "Network connection not available.");
            if (isFileAlreadyDownloaded(RELEASES_JSON)) {
                loadLocalFile(releasesFile);
            } else {
                ((BootloaderActivity) mContext).appendConsoleError("No local file found.\nNo network connection available.\nPlease check your connectivity.");
            }
        }
    }

    /**
     * Check network connectivity
     *
     * @return true if network is available, false otherwise
     */
    public boolean isNetworkAvailable() {
        ConnectivityManager connMgr = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    private void loadLocalFile(File releasesFile) {
        Log.d(LOG_TAG, "Loading releases JSON from local file...");
        try {
            String input = new String(Bootloader.readFile(releasesFile));
            mFirmwareReleases = parseJson(input);
        } catch (JSONException jsone) {
            Log.d(LOG_TAG, jsone.getMessage());
            ((BootloaderActivity) mContext).appendConsoleError("Error while parsing JSON content.");
            return;
        } catch (IOException ioe) {
            Log.d(LOG_TAG, ioe.getMessage());
            ((BootloaderActivity) mContext).appendConsoleError("Problems loading JSON file.");
            return;
        }
        ((BootloaderActivity) mContext).updateFirmwareSpinner(mFirmwareReleases);
        ((BootloaderActivity) mContext).appendConsole("Found " + mFirmwareReleases.size() + " firmware files.");
    }

    private boolean isFileTooOld (File file, long time) {
        if (file.exists() && file.length() > 0) {
            return System.currentTimeMillis() - file.lastModified() > time;
        }
        return false;
    }

    private class DownloadWebpageTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {
            String input = null;
            try {
                input = downloadUrl(urls[0]);
                Log.d(LOG_TAG, "Releases JSON downloaded.");
                mFirmwareReleases = parseJson(input);
            } catch (IOException ioe) {
                Log.d(LOG_TAG, ioe.getMessage());
                return "Unable to retrieve web page. Check your connectivity.";
            } catch (JSONException je) {
                Log.d(LOG_TAG, je.getMessage());
                return "Error during parsing JSON content.";
            }

            // Write JSON to disk
            try {
                writeToReleaseJsonFile(input);
                Log.d(LOG_TAG, "Wrote JSON file.");
            } catch (IOException ioe) {
                Log.d(LOG_TAG, ioe.getMessage());
                return "Unable to save JSON file.";
            }
            return "Found " + mFirmwareReleases.size() + " firmware files.";
        }

        @Override
        protected void onPostExecute(String result) {
            ((BootloaderActivity) mContext).updateFirmwareSpinner(mFirmwareReleases);
            ((BootloaderActivity) mContext).appendConsole(result);
        }
    }

    private void writeToReleaseJsonFile(String input) throws IOException {
        File releasesFile = new File(mBootloaderDir, RELEASES_JSON);
        mBootloaderDir.mkdirs();
        if (!releasesFile.exists()) {
            releasesFile.createNewFile();
        }
        PrintWriter out = new PrintWriter(releasesFile);
        out.println(input);
        out.flush();
        out.close();
    }

    /**
     * Base path is CrazyflieControl directory
     *
     * @param path
     * @return
     */
    public boolean isFileAlreadyDownloaded(String path) {
        File firmwareFile = new File(mBootloaderDir, path);
        return firmwareFile.exists() && firmwareFile.length() > 0;
    }

    private String downloadUrl(String myUrl) throws IOException {
        BufferedReader reader = null;
        StringBuilder builder = new StringBuilder();
        URL url = new URL(myUrl);

        // Retrofitting support for TLSv1.2, because GitHub only supports TLSv1.2
        try {
            HttpsURLConnection.setDefaultSSLSocketFactory(new TLSSocketFactory());
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();

        try {
            String responseMsg = urlConnection.getResponseMessage();
            int responseCode = urlConnection.getResponseCode();

            if (responseCode == 200) {
                reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "UTF-8"));
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
            } else {
                Log.d(LOG_TAG, "The response is: " + responseMsg);
                return "The response is: " + responseMsg;
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
            urlConnection.disconnect();
        }
        return builder.toString();
    }

    private List<FirmwareRelease> parseJson(String input) throws JSONException {

        List<FirmwareRelease> firmwareReleases = new ArrayList<FirmwareRelease>();

        JSONArray releasesArray = new JSONArray(input);
        for (int i = 0; i < releasesArray.length(); i++) {
            JSONObject releaseObject = releasesArray.getJSONObject(i);
            String tagName = releaseObject.getString("tag_name");
            String name = releaseObject.getString("name");
            String createdAt = releaseObject.getString("created_at");
            String body = releaseObject.getString("body");
            JSONArray assetsArray = releaseObject.getJSONArray("assets");
            if (assetsArray != null && assetsArray.length() > 0) {
                for (int n = 0; n < assetsArray.length(); n++) {
                    JSONObject assetsObject = assetsArray.getJSONObject(n);
                    String assetName = assetsObject.getString("name");
                    int size = assetsObject.getInt("size");
                    String downloadURL = assetsObject.getString("browser_download_url");
                    // hardcoded filter for DFU zip file (find a better way to handle this)
                    if (assetName.contains("_dfu")) {
                        continue;
                    }
                    FirmwareRelease firmwareRelease = new FirmwareRelease(tagName, name, createdAt);
                    firmwareRelease.setReleaseNotes(body);
                    firmwareRelease.setAsset(assetName, size, downloadURL);
                    firmwareReleases.add(firmwareRelease);
                }
            } else {
                // Filter out firmwares without assets
                Log.d(LOG_TAG, "Firmware " + tagName + " was filtered out, because it has no assets.");
            }
        }
        return firmwareReleases;
    }

}
