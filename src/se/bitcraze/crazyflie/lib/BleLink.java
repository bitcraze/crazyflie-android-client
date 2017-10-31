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

package se.bitcraze.crazyflie.lib;

import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import android.os.Handler;
import se.bitcraze.crazyflie.lib.crazyradio.ConnectionData;
import se.bitcraze.crazyflie.lib.crtp.CrtpDriver;
import se.bitcraze.crazyflie.lib.crtp.CrtpPacket;

@SuppressLint("NewApi")
public class BleLink extends CrtpDriver {

    final Logger mLogger = LoggerFactory.getLogger("BLELink");

	// Set to -40 to connect only to close-by Crazyflie
	private static final int rssiThreshold = -100;

	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothDevice mDevice;
	private BluetoothGattCharacteristic mLedChar;
	private List<BluetoothGattCharacteristic> mLedsChars;
	private BluetoothGatt mGatt;
	private BluetoothGattCharacteristic mCrtpChar;
	private BluetoothGattCharacteristic mCrtpDownChar;
	private Timer mScannTimer;

	private static final String CF_DEVICE_NAME = "Crazyflie";
	private static final String CF_LOADER_DEVICE_NAME = "Crazyflie Loader";

    private static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

	private static UUID CF_SERVICE = UUID.fromString("00000201-1C7F-4F9E-947B-43B7C00A9A08");
    private static UUID CRTP = UUID.fromString("00000202-1C7F-4F9E-947B-43B7C00A9A08");
    private static UUID CRTPUP = UUID.fromString("00000203-1C7F-4F9E-947B-43B7C00A9A08");
    private static UUID CRTPDOWN = UUID.fromString("00000204-1C7F-4F9E-947B-43B7C00A9A08");

	private final static int REQUEST_ENABLE_BT = 1;
	protected boolean mWritten = true;
	private Activity mContext;
	private boolean mWriteWithAnswer;
	protected boolean mConnected;

	protected enum State {IDLE, CONNECTING, CONNECTED};
	protected State state = State.IDLE;

    private final BlockingQueue<CrtpPacket> mInQueue;

	public BleLink(Activity ctx, boolean writeWithAnswer) {
		mContext = ctx;
		mWriteWithAnswer = writeWithAnswer;
        this.mInQueue = new LinkedBlockingQueue<CrtpPacket>();
	}

	private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mLogger.debug("onConnectionStateChange: STATE_CONNECTED");
                gatt.discoverServices();
                mGatt = gatt;
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mLogger.debug("onConnectionStateChange: STATE_DISCONNECTED");
                // This is necessary to handle a disconnect on the copter side
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                mConnected = false;
                state = State.IDLE;
                // it should actually be notifyConnectionLost, but there is
                // no difference between a deliberate disconnect and a lost connection
                notifyDisconnected();
            } else {
                mLogger.debug("onConnectionStateChange: else: " + newState);
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                mConnected = false;
                state = State.IDLE;
                notifyConnectionLost("BLE connection lost");
            }
        }

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			super.onServicesDiscovered(gatt, status);
			if (status != BluetoothGatt.GATT_SUCCESS) {
				gatt.disconnect();
			} else {
				BluetoothGattService cfService = gatt.getService(CF_SERVICE);
				mCrtpChar = cfService.getCharacteristic(CRTP);
				mCrtpDownChar = cfService.getCharacteristic(CRTPDOWN);

				gatt.setCharacteristicNotification(mCrtpDownChar, true);

                BluetoothGattDescriptor descriptor = mCrtpDownChar.getDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
				descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
				gatt.writeDescriptor(descriptor);

				mLogger.debug( "Connected!");

				mConnected = true;
				mWritten = false;

				state = State.CONNECTED;
				notifyConnected();
			}
		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			super.onCharacteristicWrite(gatt, characteristic, status);
			//mLogger.debug("On write called for char: " + characteristic.getUuid().toString());
			mWritten  = true;
		}

		@Override
		public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
			super.onDescriptorWrite(gatt, descriptor, status);
			mLogger.debug("On write called for descriptor: " + descriptor.getUuid().toString());
			mWritten = true;
		}

		@Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			super.onCharacteristicRead(gatt, characteristic, status);
			mLogger.debug("On read call for characteristic: " + characteristic.getUuid().toString());
        }

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
			//super.onCharacteristicChanged(gatt, characteristic);
		    mLogger.debug("On changed call for characteristic: " + characteristic.getUuid().toString());
            CrtpPacket packet = unpack(characteristic.getValue());
            if (packet != null) {
                try {
                    mInQueue.put(packet);
                } catch (InterruptedException ie) {
                    //
                }
            }
		}
	};

	private LeScanCallback mLeScanCallback = new LeScanCallback() {
		@Override
		public void onLeScan(BluetoothDevice device, int rssi, byte[] anounce) {
			if (device != null && device.getName() != null) {
			    mLogger.debug("Scanned device \"" + device.getName() + "\" RSSI: " + rssi);

				if (device.getName().equals(CF_DEVICE_NAME) && rssi>rssiThreshold) {
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
							mDevice.connectGatt(mContext, false, mGattCallback);
						}
					});
				}
			}
		}
	};

	@Override
	public void connect() {
		if (state != State.IDLE) {
			throw new IllegalArgumentException("Connection already started");
		}

		final BluetoothManager bluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
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
				notifyConnectionFailed("BLE connection timeout");
			}
		}, 5000);

		state = State.CONNECTING;
		notifyConnectionRequested();
	}

    @Override
    public void disconnect() {
        mContext.runOnUiThread(new Runnable() {
            public void run() {
                if(mConnected) {
                    mGatt.disconnect();
                    //delay close command to fix potential NPE
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mGatt.close();
                            mGatt = null;
                        }
                    }, 100);
                    mConnected = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    if (mScannTimer != null) {
                        mScannTimer.cancel();
                        mScannTimer = null;
                    }
                    state = State.IDLE;
                    notifyDisconnected();
                }
            }
        });
    }

	@Override
	public boolean isConnected() {
		return state == State.CONNECTED;
	}

	int ctr = 0;
	@Override
	public void sendPacket(CrtpPacket packet) {

		// FIXME: Skipping half of the commander packets to avoid queuing up packets on slow BLE
		if ((mWriteWithAnswer == false) && ((ctr++)%2 == 0)) {
			return;
		}

		class SendBlePacket implements Runnable {
			CrtpPacket pk;

            public SendBlePacket(CrtpPacket pk) {
                this.pk = pk;
            }

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
    public CrtpPacket receivePacket(int time) {
        try {
            return mInQueue.poll((long) time, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            mLogger.error("InterruptedException: " + e.getMessage());
            return null;
        }
    }

    private byte[] tempByteArray = new byte[32];
    private int tempPid = -1;
    private int tempLength = -1;

    private CrtpPacket unpack (byte[] packet) {
        mLogger.debug("Received value for characteristic: {}, length: {}", packet.toString(), packet.length);

        ControlByte header = new ControlByte(packet);

        if (header.isStart()) {
            Arrays.fill(tempByteArray, (byte) 0);
            if (header.getLength() < 20) {
                System.arraycopy(packet, 1, tempByteArray, 0, packet.length-1);
                return new CrtpPacket(tempByteArray);
            } else {
                System.arraycopy(packet, 1, tempByteArray, 0, packet.length-1);
                tempPid = header.getPid();
                tempLength = header.getLength();
            }
        } else {
            if (header.getPid() == tempPid) {
                System.arraycopy(packet, 1, tempByteArray, 19, packet.length-1);
                return new CrtpPacket(tempByteArray);
            } else {
                tempPid = -1;
                tempLength= 0;
                mLogger.debug("Bletooth link: Error while receiving long data: PID does not match!");
            }
        }
        return null;
    }


    private class ControlByte {

        boolean start = false;
        int pid = -1;
        int length = -1;

        public ControlByte(byte[] array) {
            this.start = (array[0]&0x80) != 0;
            this.pid = (array[0]>>5)&0x03;
            this.length = (array[0]&0x1F);
        }

        public boolean isStart() {
            return start;
        }

        public int getLength() {
            return length;
        }

        public int getPid() {
            return pid;
        }

    }

    /* CONNECTION LISTENER */

    @Override
    protected void notifyLinkQualityUpdated(int percent) {
    }

    @Override
    protected void notifyDisconnected() {
    }

    @Override
    protected void notifyConnectionFailed(String msg) {
    }

    @Override
    protected void notifyConnectionLost(String msg) {
    }

    @Override
    protected void notifyConnectionRequested() {
    }

    @Override
    public void notifyConnected() {
    }

    @Override
    public void notifySetupFinished() {
    }
}
