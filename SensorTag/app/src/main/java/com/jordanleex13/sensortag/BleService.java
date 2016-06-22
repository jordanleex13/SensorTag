package com.jordanleex13.sensortag;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
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
import android.os.Binder;
import android.os.IBinder;

import com.jordanleex13.sensortag.SensorTag.IntentNames;
import com.jordanleex13.sensortag.SensorTag.SensorTagGatt;

import java.util.List;
import java.util.UUID;


/**
 * Bluetooth Service that binds to DeviceActivity. Contains many methods that manage the bluetooth connection
 * including initialize, connect, disconnect.
 * Contains {@code BluetoothGattCallback} which is activated whenever there is a change in the connection
 * or something is received
 * Once phone is connected to BLE device, methods for enabling and disabling services and notifications are used
 *
 *
 * Generic Attribute Profile (GATT)—The GATT profile is a general specification for sending and receiving
 * short pieces of data known as "attributes" over a BLE link. All current Low Energy application profiles are based on GATT.
 *
 * Attribute Protocol (ATT)—GATT is built on top of the Attribute Protocol (ATT). This is also referred to as GATT/ATT.
 * ATT is optimized to run on BLE devices. To this end, it uses as few bytes as possible. Each attribute is uniquely
 * identified by a Universally Unique Identifier (UUID), which is a standardized 128-bit format for a string ID used
 * to uniquely identify information. The attributes transported by ATT are formatted as <b>characteristics</b>
 * and <b>services</b>.
 *
 * Code is drawn from Android Developer Bluetooth Low Energy
 *  https://developer.android.com/guide/topics/connectivity/bluetooth-le.html
 * Source code found at: https://github.com/googlesamples/android-BluetoothLeGatt
 *
 * Code is drawn from TISensorTag2 Source Code and modified for this application
 */
public class BleService extends Service {

    //private static final String TAG = BleService.class.getSimpleName();

    /**
     * BLE related variables
     */
    private static BleService mThis = null;
    private IBinder mBinder = new LocalBinder();
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private String mBluetoothDeviceAddress;

    /**
     * Shows the state of the connection between phone and BLE device-------- See {@code BluetoothProfile}
     */
    private int mConnectionState = STATE_DISCONNECTED;
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    /**
     * Used as keys for intents
     */
    public final static String ACTION_GATT_CONNECTED = "com.jordanleex13.sensortag.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.jordanleex13.sensortag.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.jordanleex13.sensortag.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_READ = "com.jordanleex13.sensortag.ACTION_DATA_READ";
    public final static String ACTION_DATA_NOTIFY = "com.jordanleex13.sensortag.ACTION_DATA_NOTIFY";
    public final static String ACTION_DATA_WRITE = "com.jordanleex13.sensortag.ACTION_DATA_WRITE";


    // Empty Constructor
    public BleService() {
    }

    public class LocalBinder extends Binder {   //11111111111111
        public BleService getInstance() {
            ////Log.d(TAG, "getInstance local binder");
            return BleService.this;
        }
    }


    /**
     * Return the communication channel to the service.  May return null if
     * clients (ie: DeviceActivity) can not bind to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        //Log.d(TAG, "onBind");
        return mBinder;
    }

    /**
     * Called when all clients have disconnected from a particular interface
     * published by the service. The default implementation does nothing and
     * returns false.
     */
    @Override
    public boolean onUnbind(Intent intent) {
        //Log.d(TAG, "on UNBIND");
        close();
        return super.onUnbind(intent);
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        //Log.d(TAG, "Closing gatt. MUST USE connectGatt() rather than simply connect()");
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Sets up the bluetooth manager and adapter
     *
     * @return Return true if both manager and adapter properly set up
     */
    public boolean initialize() {

        mThis = this;

        //Log.i(TAG, "Initializing BLE");
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                //Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            //Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        return true;
    }



    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return        Return true if the connection is initiated successfully. The connection result
     *                is reported asynchronously through the
     *                {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *                callback.
     */
    public boolean connect(final String address) {              //33333333333
        if (mBluetoothAdapter == null || address == null) {
            //Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        int connectionState = mBluetoothManager.getConnectionState(device, BluetoothProfile.GATT);

        if (connectionState == STATE_DISCONNECTED) {

            // Previously connected device.  Try to reconnect. Current address equals previously connected address
            if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null) {
                //Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
                if (mBluetoothGatt.connect()) {
                    mConnectionState = STATE_CONNECTING;
                    return true;
                } else {
                    //Log.w(TAG, "GATT reconnect failed");
                    return false;
                }
            }

            // Gets object for BLE device from its address
            if (device == null) {
                //Log.w(TAG, "Device not found.  Unable to connect.");
                return false;
            }

            mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
            //Log.d(TAG, "Trying to create a new connection.");
            mBluetoothDeviceAddress = address;
            mConnectionState = STATE_CONNECTING;
            return true;

        } else {
            //Log.w(TAG, "Attempt to connect in state: " + connectionState);
            return false;
        }

    }



    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect(final String address) {
        if (mBluetoothAdapter == null) {
            //Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        /*
         * Check the current connection with the device
         */
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        int connectionState = mBluetoothManager.getConnectionState(device, BluetoothProfile.GATT);

        if (mBluetoothGatt != null) {
            if (connectionState != STATE_DISCONNECTED) {
                //Log.d(TAG, "Disconnecting in state: " + connectionState);
                mBluetoothGatt.disconnect();
            } else {
                //Log.w(TAG, "Attempt to disconnect in state: " + connectionState);
            }
        }
    }

    /**
     * Various callback methods defined by the BLE API.
     */
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        /**
         * Callback indicating when GATT client has connected/disconnected to/from a remote
         * GATT server.
         *
         * @param gatt      GATT client
         * @param status    Status of the connect or disconnect operation.
         *                  {@link BluetoothGatt#GATT_SUCCESS} if the operation succeeds.
         * @param newState  Returns the new connection state. Can be one of
         *                  {@link BluetoothProfile#STATE_DISCONNECTED} or
         *                  {@link BluetoothProfile#STATE_CONNECTED}
         */
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;

            if (mBluetoothGatt == null) {
                //Log.e(TAG, "mBluetoothGatt not created!");
                return;
            }
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                //Log.i(TAG, "Connected to GATT server.");
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(ACTION_GATT_CONNECTED);


                /*
                 * Attempts to discover services after successful connection. Will report result through
                 * {@code BluetoothGattCallback#onServicesDiscovered(android.bluetooth.BluetoothGatt, int)} callback.
                 */
                //Log.i(TAG, "Attempting to start service discovery:");
                if (mBluetoothGatt.discoverServices()) {
                    //Log.i(TAG, "Has services");
                } else {
                    //Log.e(TAG, "NO SERVICES DISCOVERED");
                }

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                //Log.i(TAG, "Disconnected from GATT server.");
                mConnectionState = STATE_DISCONNECTED;
                broadcastUpdate(ACTION_GATT_DISCONNECTED);
            } else {
                //Log.e(TAG, "Not connected or disconnected. Something went wrong");
            }
        }


        /**
         * Callback invoked when the list of remote services, characteristics and descriptors
         * for the remote device have been updated, ie new services have been discovered.
         *
         * @param gatt   GATT client invoked {@link BluetoothGatt#discoverServices}
         * @param status {@link BluetoothGatt#GATT_SUCCESS} if the remote device
         *               has been explored successfully.
         */
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

            if (status == BluetoothGatt.GATT_SUCCESS) {
                //Log.i(TAG, "SUCCESSFULLY DISCOVERED SERVICES");
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                //Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        /**
         * Callback reporting the result of a characteristic read operation.
         *
         * @param gatt              GATT client invoked {@link BluetoothGatt#readCharacteristic}
         * @param characteristic    Characteristic that was read from the associated
         *                          remote device.
         * @param status            {@link BluetoothGatt#GATT_SUCCESS} if the read operation
         *                          was completed successfully.
         */
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
//            //Log.i(TAG, "Callback: characteristic read from " + characteristic.getUuid().toString() + "\nStatus: " + status);
            broadcastUpdate(ACTION_DATA_READ, characteristic, status);
        }

        /**
         * Callback indicating the result of a characteristic write operation.
         *
         * @param gatt           GATT client invoked {@link BluetoothGatt#writeCharacteristic}
         * @param characteristic Characteristic that was written to the associated
         *                       remote device.
         * @param status         The result of the write operation
         *                       {@link BluetoothGatt#GATT_SUCCESS} if the operation succeeds.
         */
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
//            //Log.i(TAG, "Callback: Characteristic write from  " + characteristic.getUuid().toString() + "\nStatus: " + status);
            broadcastUpdate(ACTION_DATA_WRITE, characteristic, status);
        }

        /**
         * Callback triggered as a result of a remote characteristic notification.
         *
         * @param gatt              GATT client the characteristic is associated with
         * @param characteristic    Characteristic that has been updated as a result
         *                          of a remote notification event.
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
//            //Log.i(TAG, "Callback: characteristic changed " + characteristic.getUuid().toString());
            broadcastUpdate(ACTION_DATA_NOTIFY, characteristic, BluetoothGatt.GATT_SUCCESS);
        }


        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            //Log.i(TAG, "Callback: Descriptor read from " + descriptor.getUuid().toString() + "\nStatus: " + status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            //Log.i(TAG, "Callback: Descriptor write from " + descriptor.getUuid().toString() + "\nStatus: " + status);

        }
    };

    /**
     * Sends broadcast to notify other activities that either
     *  1) Connection state has changed
     *  2) Services have been discovered
     *
     * @param action Intent key that will allow program to identify different broadcasts
     */
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    /**
     * Sends broadcast to notify other activities that BluetoothGattCharacteristic has been accessed or altered
     *
     * The UUID of the characteristic, the value (data) within that characteristic, and the status of the transaction are stored as extras
     *
     * @param action            Intent key that will allow program to identify different broadcasts
     * @param characteristic    The characteristic that has been accessed or altered
     * @param status            Status of transaction. Need it to be BluetoothGatt.GATT_SUCCESS
     */
    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic, final int status) {
        final Intent intent = new Intent(action);
        intent.putExtra(IntentNames.EXTRA_UUID, characteristic.getUuid().toString());
        intent.putExtra(IntentNames.EXTRA_DATA, characteristic.getValue());
        intent.putExtra(IntentNames.EXTRA_STATUS, status);
        sendBroadcast(intent);
    }


    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;
        return mBluetoothGatt.getServices();
    }



    /**
     * Enables service with corresponding configuration UUID----------- see {@code SensorTagGatt} for full list of UUIDs
     * Works with all services on SensorTag2 except for Motion Services which is a special case detailed below
     *
     * @param service       BluetoothGattService to be enabled
     * @param configUuid    Configuration UUID of that service
     */
    public void enableService(BluetoothGattService service, UUID configUuid) {
        BluetoothGattCharacteristic config = service.getCharacteristic(configUuid);
        config.setValue(new byte[] {1});
        mBluetoothGatt.writeCharacteristic(config);
        //Log.i(TAG, "Enabling " + service.getUuid());
    }

    /**
     * Enables motion service with corresponding configuration UUID----------- see {@code SensorTagGatt} for full list of UUIDs
     *
     * @param service       BluetoothGattService to be enabled
     * @param configUuid    Configuration UUID of that service
     */
    public void enableMotionService(BluetoothGattService service, UUID configUuid, boolean wakeOnShake) {

//        Byte 1
//        Bit0	    2^0     Gyroscope z axis enable
//        Bit1	    2^1     Gyroscope y axis enable
//        Bit2	    2^2     Gyroscope x axis enable
//        Bit3	    2^3     Accelerometer z axis enable
//        Bit4	    2^4     Accelerometer y axis enable
//        Bit5	    2^5     Accelerometer x axis enable
//        Bit6	    2^6     Magnetometer enable (all axes)
//        Bit7	    2^7     Wake-On-Motion Enable

//        Byte 2
//        8:9	Accelerometer range (0=2G, 1=4G, 2=8G, 3=16G)
//        10:15	Not used

        byte b[] = new byte[] {0x7F,0x00};
        // 0x7F (hexadecimal) = 127 (decimal)
        // 127 = 2^0 + 2^1 ... 2^6
        // Enables all bits from 0-6

        if (wakeOnShake) {
            b[0] = (byte)0xFF;  // Enables bit 7
        }

        BluetoothGattCharacteristic config = service.getCharacteristic(configUuid);
        config.setValue(b);
        mBluetoothGatt.writeCharacteristic(config);
        //Log.i(TAG, "Enabling motion services with wakeOnShake: " + wakeOnShake);
    }


    /**
     * Disables service with corresponding configuration UUID----------- see {@code SensorTagGatt} for full list of UUIDs
     * Works with all services on SensorTag2 except for Motion Services which is a special case detailed below
     *
     * @param service       BluetoothGattService to be disabled
     * @param configUuid    Configuration UUID of that service
     */
    public void disableService(BluetoothGattService service, UUID configUuid) {
        BluetoothGattCharacteristic config = service.getCharacteristic(configUuid);
        config.setValue(new byte[] {0});
        mBluetoothGatt.writeCharacteristic(config);
        //Log.i(TAG, "Disabling " + service.getUuid());

    }

    /**
     * Disables motion service with corresponding configuration UUID----------- see {@code SensorTagGatt} for full list of UUIDs
     *
     * @param service BluetoothGattService to be disabled
     * @param configUuid Configuration UUID of that service
     */
    public void disableMotionService(BluetoothGattService service, UUID configUuid) {
        byte b[] = new byte[] {0x00,0x00};

        BluetoothGattCharacteristic config = service.getCharacteristic(configUuid);
        config.setValue(b);
        mBluetoothGatt.writeCharacteristic(config);
        //Log.i(TAG, "Disabling motion services");
    }

    /**
     * Enables service notifications with corresponding data UUID----------- see {@code SensorTagGatt} for full list of UUIDs
     * Works with all services on SensorTag2
     *
     * @param service       BluetoothGattService to enable notifications
     * @param dataUuid      Data UUID of that service
     */
    public void enableNotifications(BluetoothGattService service, UUID dataUuid) {

        BluetoothGattCharacteristic dataCharacteristic = service.getCharacteristic(dataUuid);
        mBluetoothGatt.setCharacteristicNotification(dataCharacteristic, true); //Enabled locally

        BluetoothGattDescriptor config = dataCharacteristic.getDescriptor(SensorTagGatt.UUID_NOTIFICATIONS);
        config.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(config); //Enabled remotely
    }

    /**
     * Disables service notifications with corresponding data UUID----------- see {@code SensorTagGatt} for full list of UUIDs
     * Works with all services on SensorTag2
     *
     * @param service       BluetoothGattService to disable notifications
     * @param dataUuid      Data UUID of that service
     */
    public void disableNotifications(BluetoothGattService service, UUID dataUuid) {

        UUID notificationUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

        BluetoothGattCharacteristic dataCharacteristic = service.getCharacteristic(dataUuid);
        mBluetoothGatt.setCharacteristicNotification(dataCharacteristic, false); //Disabled locally

        BluetoothGattDescriptor config = dataCharacteristic.getDescriptor(notificationUuid);
        config.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(config); //Disabled remotely
    }

    public void changePeriod(BluetoothGattCharacteristic periodCharacteristic, byte p) {

        byte[] val = new byte[1];
        val[0] = p;

        periodCharacteristic.setValue(val);
        mBluetoothGatt.writeCharacteristic(periodCharacteristic);
        //Log.i(TAG, "Changing period of service " + periodCharacteristic.getUuid().toString());
    }



    public void changeIO(BluetoothGattCharacteristic characteristic, byte[] val) {
        characteristic.setValue(val);
        mBluetoothGatt.writeCharacteristic(characteristic);
    }


    /**
     * Helpful utility functions
     */
    public static BluetoothGatt getBtGatt() {
        return mThis.mBluetoothGatt;
    }

    public static BluetoothManager getBtManager() {
        return mThis.mBluetoothManager;
    }

    public static BleService getInstance() {
        return mThis;
    }

    public BluetoothGattCharacteristic getCharacteristicFromUUID(String charUuid) {
        List<BluetoothGattService> gattServiceList = getSupportedGattServices();

        for (BluetoothGattService service: gattServiceList) {
            List<BluetoothGattCharacteristic> gattCharacteristicList = service.getCharacteristics();

            for (BluetoothGattCharacteristic characteristic: gattCharacteristicList) {
                if (characteristic.getUuid().toString().compareTo(charUuid) == 0) {

                    //Log.d(TAG, "Found characteristic with UUID " + charUuid);
                    return characteristic;
                }
            }

        }
        //Log.e(TAG, "Cannot find characteristic from UUID");
        return null;
    }

    public BluetoothGattService getServiceFromUUID(String serviceUuid) {
        List<BluetoothGattService> gattServiceList = getSupportedGattServices();

        for (BluetoothGattService service : gattServiceList) {
            if (service.getUuid().toString().compareTo(serviceUuid) == 0) {
                //Log.d(TAG, "Found service with UUID " + serviceUuid);
                return service;
            }

        }
        //Log.e(TAG, "Cannot find service from UUID");
        return null;
    }

    public String getConnectedDeviceAddress() {
        return this.mBluetoothDeviceAddress;
    }

}
