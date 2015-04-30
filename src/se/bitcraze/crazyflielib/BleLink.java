package se.bitcraze.crazyflielib;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.bitcraze.crazyflielib.CrazyradioLink.ConnectionData;
import se.bitcraze.crazyflielib.crtp.CrtpPacket;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;

@SuppressLint("NewApi")
public class BleLink extends Link {

    final Logger mLogger = LoggerFactory.getLogger("BLELink");

	// Set to -40 to connect only to close-by Crazyflie
	private static final int rssiThreshold = -100;

	BluetoothAdapter mBluetoothAdapter;
	private final static int REQUEST_ENABLE_BT = 1;
	private BluetoothDevice mDevice;
	private boolean mWriteWithAnswer;
	protected BluetoothGattCharacteristic mLedChar;
	protected BluetoothGatt mGatt;
	protected boolean mConnected;
	protected List<BluetoothGattCharacteristic> mLedsChars;
	protected BluetoothGattCharacteristic mCrtpChar;
	private static Activity mContext;
	protected static boolean mWritten = true;
	private Timer mScannTimer;

	protected enum State {IDLE, CONNECTING, CONNECTED};
	protected State state = State.IDLE;

	public BleLink(Activity ctx, boolean writeWithAnswer) {
		mContext = ctx;
		mWriteWithAnswer = writeWithAnswer;
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
				mBluetoothAdapter.stopLeScan(mLeScanCallback);
				mConnected = false;
				state = State.IDLE;
				mLogger.debug("BLE connection lost");
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

				mLogger.debug( "Connected!");

				mConnected = true;
				mWritten = false;

				state = State.CONNECTED;
			}
		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status) {
			super.onCharacteristicWrite(gatt, characteristic, status);
			//mLogger.debug("On write called for char: " + characteristic.getUuid().toString());
			BleLink.mWritten  = true;
		}

		@Override
		public void onDescriptorWrite(BluetoothGatt gatt,
				BluetoothGattDescriptor descritor, int status) {
			super.onDescriptorWrite(gatt, descritor, status);
			mLogger.debug("On write called for descriptor: " + descritor.getUuid().toString());
			mWritten = true;
		}

		@Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
			super.onCharacteristicRead(gatt, characteristic, status);
			mLogger.debug("On read call for characteristic: " + characteristic.getUuid().toString());
        }

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt,
		        BluetoothGattCharacteristic characteristic) {
			//super.onCharacteristicChanged(gatt, characteristic);
		    mLogger.debug("On changed call for characteristic: " + characteristic.getUuid().toString());
		}
	};

	private LeScanCallback mLeScanCallback = new LeScanCallback() {
		@Override
		public void onLeScan(BluetoothDevice device, int rssi, byte[] anounce) {
			if (device != null && device.getName() != null) {
			    mLogger.debug("Scanned device \"" + device.getName() + "\" RSSI: " + rssi);

				if (device.getName().equals("Crazyflie") && rssi>rssiThreshold) {
					mBluetoothAdapter.stopLeScan(this);
					if (mScannTimer != null) {
						mScannTimer.cancel();
						mScannTimer = null;
					}
					state = State.CONNECTING;
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
	public void connect(ConnectionData connectionData) {
	    // TODO: connectionData is unused until BLE can address specific quadcopter
		if (state != State.IDLE) {
			throw new IllegalArgumentException("Connection already started");
		}

		final BluetoothManager bluetoothManager =
		        (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();

		if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
		    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		    mContext.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		    throw new IllegalArgumentException("Bluetooth needs to be started");
		}

		mBluetoothAdapter.stopLeScan(mLeScanCallback);
		mBluetoothAdapter.startLeScan(mLeScanCallback);
		if (mScannTimer != null) {
			mScannTimer.cancel();
		}
		mScannTimer = new Timer();
		mScannTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				mBluetoothAdapter.stopLeScan(mLeScanCallback);
				state = State.IDLE;
				mLogger.debug("BLE connection timeout");
			}
		}, 10000);

		state = State.CONNECTING;
	}

	@Override
	public void disconnect() {
	    mLogger.debug("BLELink disconnect()");
	    //TODO: does this really need to run on UI thread?
		mContext.runOnUiThread(new Runnable() {
			public void run() {
				if(mConnected) {
					mGatt.disconnect();
					mGatt.close();
					mGatt = null;
					mConnected = false;
					mBluetoothAdapter.stopLeScan(mLeScanCallback);
					if (mScannTimer != null) {
						mScannTimer.cancel();
						mScannTimer = null;
					}
					state = State.IDLE;
				}
			}
		});
	}

	@Override
	public boolean isConnected() {
		return state != State.IDLE;
	}

	int ctr = 0;
	@Override
	public void sendPacket(CrtpPacket packet) {

		// FIXME: Skipping half of the commander packets to avoid queuing up packets on slow BLE
		if ((mWriteWithAnswer == false) && ((ctr++)%2 == 0))
			return;

		class SendBlePacket implements Runnable {
			CrtpPacket pk;
			SendBlePacket(CrtpPacket pk) { this.pk = pk; }
			public void run() {
				if(mConnected && mWritten) {
					if (mWriteWithAnswer) {
						mCrtpChar.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
						mWritten = false;
					} else {
						mCrtpChar.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
						mWritten = true;
					}
					mCrtpChar.setValue(pk.toByteArray());
					mGatt.writeCharacteristic(mCrtpChar);
				}
	        }
		}
		mContext.runOnUiThread(new SendBlePacket(packet));
	}

    @Override
    public CrtpPacket receivePacket() {
        //TODO: receive packets
        return null;
    }

}
