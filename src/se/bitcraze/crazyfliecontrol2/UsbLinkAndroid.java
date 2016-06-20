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

package se.bitcraze.crazyfliecontrol2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;
import se.bitcraze.crazyflie.lib.usb.CrazyUsbInterface;

public class UsbLinkAndroid implements CrazyUsbInterface{

    private static final String LOG_TAG = "UsbLinkAndroid";

    private static int TRANSFER_TIMEOUT = 1000;

    private UsbManager mUsbManager;
    private UsbDevice mUsbDevice;
    private UsbInterface mIntf;
    private UsbEndpoint mEpIn;
    private UsbEndpoint mEpOut;
    private UsbDeviceConnection mConnection;
    private Context mContext;


    private static PendingIntent mPermissionIntent;


    public UsbLinkAndroid(Context context) throws IOException {
        this.mContext = context;
    }

    /**
     * Initialize the USB device. Determines endpoints and prepares communication.
     *
     * @param vid
     * @param pid
     * @throws IOException if the device cannot be opened
     * @throws SecurityException
     */
    public void initDevice(int vid, int pid) throws IOException, SecurityException {
        mUsbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
        if (mUsbManager == null) {
            throw new IllegalArgumentException("UsbManager == null!");
        }

        List<UsbDevice> usbDevices = findUsbDevices(mUsbManager, (short) vid, (short) pid);
        if (usbDevices.isEmpty() || usbDevices.get(0) == null) {
            throw new IOException("USB device not found. (VID: " + vid + ", PID: " + pid + ")");
        }
        // TODO: Only gets the first USB device that is found
        this.mUsbDevice = usbDevices.get(0);

        //request permissions
        if (mUsbDevice != null && !mUsbManager.hasPermission(mUsbDevice)) {
            Log.d(LOG_TAG, "Request permission");
            mPermissionIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(mContext.getPackageName()+".USB_PERMISSION"), 0);
            mUsbManager.requestPermission(mUsbDevice, mPermissionIntent);
        } else if (mUsbDevice != null && mUsbManager.hasPermission(mUsbDevice)) {
            Log.d(LOG_TAG, "Has permission");
        } else {
            Log.d(LOG_TAG, "device == null");
            return;
        }

        Log.d(LOG_TAG, "setDevice " + this.mUsbDevice);
        // find interface
        if (this.mUsbDevice.getInterfaceCount() != 1) {
            Log.e(LOG_TAG, "Could not find interface");
            return;
        }
        mIntf = this.mUsbDevice.getInterface(0);
        // device should have two endpoints
        if (mIntf.getEndpointCount() != 2) {
            Log.e(LOG_TAG, "Could not find endpoints");
            return;
        }
        // endpoints should be of type bulk
        UsbEndpoint ep = mIntf.getEndpoint(0);
        if (ep.getType() != UsbConstants.USB_ENDPOINT_XFER_BULK) {
            Log.e(LOG_TAG, "Endpoint is not of type bulk");
            return;
        }
        // check endpoint direction
        if (ep.getDirection() == UsbConstants.USB_DIR_IN) {
            mEpIn = mIntf.getEndpoint(0);
            mEpOut = mIntf.getEndpoint(1);
        } else {
            mEpIn = mIntf.getEndpoint(1);
            mEpOut = mIntf.getEndpoint(0);
        }

        UsbDeviceConnection connection = mUsbManager.openDevice(mUsbDevice);
        if (connection != null && connection.claimInterface(mIntf, true)) {
            Log.d(LOG_TAG, "open SUCCESS");
            mConnection = connection;
        } else {
            Log.d(LOG_TAG, "open FAIL");
            throw new IOException("could not open usb connection");
        }
    }

    /* (non-Javadoc)
     * @see se.bitcraze.crazyflie.lib.IUsbLink#releaseInterface()
     */
    public void releaseInterface() {
        Log.d(LOG_TAG, "releaseInterface()");
        if (mConnection != null && mIntf != null){
            mConnection.releaseInterface(mIntf);
            mConnection = null;
            mIntf = null;
        }
    }

    /* (non-Javadoc)
     * @see se.bitcraze.crazyflie.lib.IUsbLink#sendControlTransfer(int, int, int, int, byte[])
     */
    public int sendControlTransfer(int requestType, int request, int value, int index, byte[] data){
        if(mConnection != null){
            int dataLength = (data == null) ? 0 : data.length;
            return mConnection.controlTransfer(requestType, request, value, index, data, dataLength, TRANSFER_TIMEOUT);
        }
        return -1;
    }

    /* (non-Javadoc)
     * @see se.bitcraze.crazyflie.lib.IUsbLink#sendBulkTransfer(byte[], byte[])
     */
    public int sendBulkTransfer(byte[] data, byte[] receiveData){
        int returnCode = -1;
        if(mConnection != null){
            mConnection.bulkTransfer(mEpOut, data, data.length, TRANSFER_TIMEOUT);
            returnCode = mConnection.bulkTransfer(mEpIn, receiveData, receiveData.length, TRANSFER_TIMEOUT);
        }
        return returnCode;
    }

    @Override
    public void bulkWrite(byte[] data) {
    }

    @Override
    public byte[] bulkRead() {
        return null;
    }

    public UsbDeviceConnection getConnection(){
        return mConnection;
    }

    @Override
    public List<UsbDevice> findDevices(int vid, int pid) {
        return findUsbDevices(mUsbManager, (short) vid, (short) pid);
    }

    public static List<UsbDevice> findUsbDevices(UsbManager usbManager, int vendorId, int productId) {
        List<UsbDevice> usbDeviceList = new ArrayList<UsbDevice>();
        if (usbManager != null) {
            HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
            // Iterate over USB devices
            for (Entry<String, UsbDevice> e : deviceList.entrySet()) {
                Log.i(LOG_TAG, "String: " + e.getKey() + " " + e.getValue().getVendorId() + " " + e.getValue().getProductId());
                UsbDevice device = e.getValue();
                if (device.getVendorId() == vendorId && device.getProductId() == productId) {
                    usbDeviceList.add(device);
                }
            }
        }
        return usbDeviceList;
    }

    /* (non-Javadoc)
     * @see se.bitcraze.crazyflie.lib.IUsbLink#getFirmwareVersion()
     */
    public float getFirmwareVersion() {
        if (mConnection == null) {
            return 0.0f;
        }
        byte[] rawDescs = mConnection.getRawDescriptors();
        return Float.parseFloat(Integer.toHexString(rawDescs[13]) + "." + Integer.toHexString(rawDescs[12]));
    }

    /* (non-Javadoc)
     * @see se.bitcraze.crazyflielib.IUsbLink#getSerialNumber()
     */
    public String getSerialNumber() {
        return mConnection.getSerial();
    }

    /* (non-Javadoc)
     * @see se.bitcraze.crazyflie.lib.IUsbLink#isUsbConnected()
     */
    public boolean isUsbConnected() {
        return mUsbDevice != null && mConnection != null;
    }

    public static boolean isUsbDevice(UsbDevice usbDevice, int vid, int pid) {
        return usbDevice.getVendorId() == vid && usbDevice.getProductId() == pid;
    }

    //TODO: redundant?
    public boolean isUsbDeviceConnected(int vid, int pid) {
        return isUsbConnected() && isUsbDevice(mUsbDevice, vid, pid);
    }

}
