package se.bitcraze.crazyflielib;

import se.bitcraze.crazyflielib.crtp.CrtpPacket;

import java.util.List;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BleLink extends AbstractLink {

	BluetoothAdapter mBluetoothAdapter;
	private final static int REQUEST_ENABLE_BT = 1;
	private BluetoothDevice mDevice;
	protected BluetoothGattCharacteristic mLedChar;
	protected BluetoothGatt mGatt;
	protected boolean mConnected;
	protected List<BluetoothGattCharacteristic> mLedsChars;
	protected BluetoothGattCharacteristic mCrtpChar;
	private static Activity mContext;
	protected static boolean mWritten = true;

	public BleLink(Activity ctx) {
		mContext = ctx;
	}
	
	private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status,
				int newState) {
			super.onConnectionStateChange(gatt, status, newState);
			if (newState ==BluetoothProfile.STATE_CONNECTED) {
				gatt.discoverServices();
				mGatt = gatt;
			} else {
				mConnected = false;
				notifyConnectionLost();
			}
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			super.onServicesDiscovered(gatt, status);
			if (status != BluetoothGatt.GATT_SUCCESS) {
				gatt.disconnect();
			} else {
				BluetoothGattService cfService = gatt.getService(UUID.fromString("00000201-1c7f-4f9e-947b-43b7c00a9a08"));
				mCrtpChar = cfService.getCharacteristic(UUID.fromString("00000202-1c7f-4f9e-947b-43b7c00a9a08"));

				gatt.setCharacteristicNotification(mCrtpChar, true);
				
				BluetoothGattDescriptor descriptor = mCrtpChar.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805F9B34FB"));
				descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
				gatt.writeDescriptor(descriptor);
				
				Log.e("BleLink", "Connected!");
				
				mConnected = true;
				mWritten = false;
				
				notifyConnectionSetupFinished();
			}
		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status) {
			super.onCharacteristicWrite(gatt, characteristic, status);
			//Log.e("bleTest", "On write called for char: " + characteristic.getUuid().toString());
			BleLink.mWritten  = true;
		}
		
		@Override
		public void onDescriptorWrite(BluetoothGatt gatt,
				BluetoothGattDescriptor descritor, int status) {
			super.onDescriptorWrite(gatt, descritor, status);
			Log.e("bleTest", "On write called for descriptor: " + descritor.getUuid().toString());
			mWritten = true;
		}
		
		@Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
			super.onCharacteristicRead(gatt, characteristic, status);
			Log.e("bleTest", "On read call for characteristic: " + characteristic.getUuid().toString());
        }
		
		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt,
		        BluetoothGattCharacteristic characteristic) {
			//super.onCharacteristicChanged(gatt, characteristic);
			Log.e("bleTest", "On changed call for characteristic: " + characteristic.getUuid().toString());
			Log.e("bleTest", "Data len is: " + characteristic.getStringValue(0).length());
			Log.e("bleTest", "Data is: " + characteristic.getStringValue(0));
		}
	};

	private LeScanCallback mLeScanCallback = new LeScanCallback() {
		@Override
		public void onLeScan(BluetoothDevice device, int rssi, byte[] anounce) {
			if (device != null && device.getName() != null) {
				Log.e("bluetoothTest", device.getName());

				if (device.getName().equals("Crazyflie")) {
					mBluetoothAdapter.stopLeScan(this);
					mDevice = device;
					mContext.runOnUiThread(new Runnable() {
						@Override
                        public void run() {
							mDevice.connectGatt(BleLink.mContext, false, mGattCallback);
						}
					});
				}
			}
		}
	};
	
	@Override
	public void connect() {
		final BluetoothManager bluetoothManager =
		        (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();

		if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
		    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		    mContext.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}

		mBluetoothAdapter.stopLeScan(mLeScanCallback);
		mBluetoothAdapter.startLeScan(mLeScanCallback);
		
		notifyConnectionInitiated();
	}

	@Override
	public void disconnect() {
		if(mConnected) {
			mGatt.disconnect();
			mGatt.close();
			mGatt = null;
			notifyDisconnected();
		}
	}

	@Override
	public boolean isConnected() {
		return mConnected;
	}

	@Override
	public void send(CrtpPacket packet) {
		if(mConnected && mWritten) {
			//mCrtpChar.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
			mCrtpChar.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
			mCrtpChar.setValue(packet.toByteArray());
			mWritten = false;
			mGatt.writeCharacteristic(mCrtpChar);
		}
	}

}
