package se.bitcraze.crazyfliecontrol.bootloader;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
import se.bitcraze.crazyflielib.bootloader.Bootloader;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

public class FirmwareDownloader {

    private static final String LOG_TAG = "FirmwareDownloader";
    private Context mContext;
    public final static String DOWNLOAD_DIRECTORY = "CrazyflieControl";
    public final static String RELEASES_JSON = "cf_releases.json";
    private List<Firmware> mFirmwares = new ArrayList<Firmware>();
    private long mDownloadReference;
    private DownloadManager mManager;

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
            String input = null;
            try {
                if (!isFileAlreadyDownloaded(RELEASES_JSON)) {
                    Log.d(LOG_TAG, "Releases JSON downloaded.");
                    input = downloadUrl(urls[0]);
                } else {
                    Log.d(LOG_TAG, "Releases JSON loaded from file.");
                    File sdcard = Environment.getExternalStorageDirectory();
                    File releasesFile = new File(sdcard, DOWNLOAD_DIRECTORY + "/" + RELEASES_JSON);
                    input = new String(Bootloader.readFile(releasesFile));
                }
                mFirmwares = parseJson(input);
            } catch (IOException ioe) {
                Log.d(LOG_TAG, ioe.getMessage());
                return "Unable to retrieve web page. URL may be invalid.";
            } catch (JSONException je) {
                Log.d(LOG_TAG, je.getMessage());
                return "Error during parsing JSON content.";
            }
            //TODO: simplify
            if (!isFileAlreadyDownloaded(RELEASES_JSON)) {
                // Write JSON to disk
                try {
                    writeToReleaseJsonFile(input);
                    Log.d(LOG_TAG, "Wrote JSON file.");
                } catch (IOException ioe) {
                    Log.d(LOG_TAG, ioe.getMessage());
                    return "Unable to save JSON file.";
                }
            }
            return "Status: Found " + mFirmwares.size() + " firmwares.";
        }

        @Override
        protected void onPostExecute(String result) {
            ((BootloaderActivity) mContext).updateFirmwareSpinner(mFirmwares);
            ((BootloaderActivity) mContext).setStatusLine(result);
        }
    }

    private void writeToReleaseJsonFile(String input) throws IOException {
        File sdcard = Environment.getExternalStorageDirectory();
        File releasesFile = new File(sdcard, DOWNLOAD_DIRECTORY + "/" + RELEASES_JSON);
        if (!releasesFile.exists()) {
            releasesFile.createNewFile();
        }
        PrintWriter out = new PrintWriter(releasesFile);
        out.println(input);
        out.flush();
        out.close();
    }

    public void downloadFirmware(Firmware selectedFirmware) {
        if (selectedFirmware != null && selectedFirmware.getAssets().size() > 0) {
            for (Asset asset : selectedFirmware.getAssets()) {
                //check if file is already downloaded
                if (isFileAlreadyDownloaded(selectedFirmware.getTagName() + "/" + asset.getName())) {
                    Log.d(LOG_TAG, "File " + asset.getName() + " already downloaded.");
                } else {
                    String browserDownloadUrl = asset.getBrowserDownloadUrl();
                    downloadFile(browserDownloadUrl, asset.getName(), selectedFirmware.getTagName());
                }
            }
        } else {
            Log.d(LOG_TAG, "Selected firmware does not have assets.");
            ((BootloaderActivity) mContext).setStatusLine("Selected firmware does not have assets.");
            return;
        }
    }

    public boolean isFileAlreadyDownloaded (String path) {
        File sdcard = Environment.getExternalStorageDirectory();
        File firmwareFile = new File(sdcard, DOWNLOAD_DIRECTORY + "/" + path);
        return firmwareFile.exists() && firmwareFile.length() > 0;
    }

    public void downloadFile (String url, String fileName, String tagName) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setDescription("Some description");
        request.setTitle(fileName);
        // in order for this if to run, you must use the android 3.2 to compile your app
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
        }
        request.setDestinationInExternalPublicDir(DOWNLOAD_DIRECTORY, tagName + "/" + fileName);

        // get download service and enqueue file
        mManager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
        mDownloadReference = mManager.enqueue(request);

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

    public BroadcastReceiver onComplete=new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            //check if the broadcast message is for our Enqueued download
//            long referenceId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
//            if(mDownloadReference == referenceId){
//            }
//            long id = intent.getStringExtra(DownloadManager.COLUMN_LOCAL_FILENAME);

            String action = intent.getAction();
            if (action.equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE) ){
                Bundle extras = intent.getExtras();
                DownloadManager.Query q = new DownloadManager.Query();
                q.setFilterById(extras.getLong(DownloadManager.EXTRA_DOWNLOAD_ID));
                Cursor c = mManager.query(q);

                if (c.moveToFirst()) {
                    int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        String filePath = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME));
                        String filename = filePath.substring(filePath.lastIndexOf('/')+1, filePath.length());

                        Toast.makeText(mContext, "Download successful: " + filename, Toast.LENGTH_SHORT).show();
                        if (filename.endsWith(".zip")) {
                            unzip(new File(filePath));
                        }
                    }
                }
                c.close();
            }
        }
      };

    public void unzip(File zipFile) {
        Log.d(LOG_TAG, "Trying to unzip file " + zipFile + "...");
        InputStream fis = null;
        ZipInputStream zis = null;
        FileOutputStream fos = null;
        String parent = zipFile.getParent();

        try {
            fis = new FileInputStream(zipFile);
            zis = new ZipInputStream(new BufferedInputStream(fis));
            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int count;
                while ((count = zis.read(buffer)) != -1) {
                    baos.write(buffer, 0, count);
                }
                String filename = ze.getName();
                byte[] bytes = baos.toByteArray();
                // write files
                File filePath = new File(parent + "/" + filename);
                fos = new FileOutputStream(filePath);
                fos.write(bytes);
                //check
                if(filePath.exists() && filePath.length() > 0) {
                    Log.d(LOG_TAG, "File " + filename + " successfully unzipped.");
                } else {
                    Log.d(LOG_TAG, "Problems writing file " + filename + ".");
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (zis != null) {
                try {
                    zis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }
}
