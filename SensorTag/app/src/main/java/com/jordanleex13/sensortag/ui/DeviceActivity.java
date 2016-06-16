package com.jordanleex13.sensortag.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.jordanleex13.sensortag.BleService;
import com.jordanleex13.sensortag.R;
import com.jordanleex13.sensortag.SensorTag.IntentNames;
import com.jordanleex13.sensortag.SensorTag.SensorTagGatt;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity that manages connection with the BLE device. Displays data in fragment view pager where each
 * fragment is a different SensorTag service.
 *
 * @author Jordan Lee
 */
public class DeviceActivity extends AppCompatActivity {

    private static final String TAG = DeviceActivity.class.getSimpleName();

    /**
     * BLE related
     */
    private String mDeviceName;
    private String mDeviceAddress;
    private BleService mBleService;
    private BluetoothDevice mBluetoothDevice;
    private boolean mBounded;
    private boolean mConnected;
    public ProgressDialog progressDialog;

    /**
     * UI elements
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;


    /**
     * Interface for monitoring the state of an application service
     */
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        /**
         * Called when a connection to the Service has been established, with
         * the {@link android.os.IBinder} of the communication channel to the
         * Service. Binds to the service, initializes the service and tries to
         * connect to the BLE device.
         *
         * @param name The concrete component name of the service that has
         * been connected.
         *
         * @param service The IBinder of the Service's communication channel,
         * which you can now make calls on.
         */
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            mBounded = true;
            Log.i(TAG, "Service Connected");
            BleService.LocalBinder temp = (BleService.LocalBinder) service;
            mBleService = temp.getInstance();

            if(!mBleService.initialize()) {
                Log.e(TAG, "Cannot initialize");
                finish();
            }
            if (mBleService.connect(mDeviceAddress)) {
                mConnected = true;
            } else {
                mConnected = false;
                Log.e(TAG, "Failed to connect to BLE device");
            }

        }

        /**
         * Called when a connection to the Service has been lost.  This typically
         * happens when the process hosting the service has crashed or been killed.
         * This does <em>not</em> remove the ServiceConnection itself -- this
         * binding to the service will remain active, and you will receive a call
         * to {@link #onServiceConnected} when the Service is next running.
         *
         * @param name The concrete component name of the service whose
         * connection has been lost.
         */
        @Override
        public void onServiceDisconnected(ComponentName name) {

            Log.i(TAG, "Service Disconnected");
            mBounded = false;
            mConnected = false;
            mBleService = null;
        }
    };

    /**
     * Receives intents sent by sendBroadcast() from BleService. Parses through the intent to find
     * appropriate action and responds accordingly
     */
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (BleService.ACTION_GATT_CONNECTED.equals(action))
            {
                Toast.makeText(DeviceActivity.this, "Connected",Toast.LENGTH_SHORT).show();

            } else if (BleService.ACTION_GATT_DISCONNECTED.equals(action))
            {
                Toast.makeText(DeviceActivity.this, "Disconnected",Toast.LENGTH_SHORT).show();

            } else if (BleService.ACTION_GATT_SERVICES_DISCOVERED.equals(action))
            {
                Log.i(TAG, "Services discovered");
                setUpGattServices(mBleService.getSupportedGattServices());

            } else if (BleService.ACTION_DATA_READ.equals(action))
            {
                Log.d(TAG, "Data read");

            } else if (BleService.ACTION_DATA_WRITE.equals(action))
            {
                Log.d(TAG, "Data written");

            } else if (BleService.ACTION_DATA_NOTIFY.equals(action))
            {
                /*
                 * Data was changed and the appropriate fragment will be notified
                 */
                String uuidStr = intent.getStringExtra(IntentNames.EXTRA_UUID);
                byte[] value = intent.getByteArrayExtra(IntentNames.EXTRA_DATA);

                sendNotificationsToFragments(uuidStr, value);

            } else
            {
                Log.e(TAG, "Unknown received");
            }
        }
    };

    /**
     * Compares the current UUID with the UUIDs of the different services and sends a broadcast corresponding
     * to the correct service.
     *
     * @param uuidStr   Data UUID of the service which will receive the broadcast
     * @param value     The byte array which contains the data
     */
    private void sendNotificationsToFragments(String uuidStr, byte[] value) {
        Intent notifyIntent;

        if (uuidStr.compareTo(SensorTagGatt.UUID_IRT_DATA.toString()) == 0) {
            Log.d(TAG, "Notify intent to temperature");
            notifyIntent = new Intent(IntentNames.ACTION_IRT_CHANGE);
            notifyIntent.putExtra(IntentNames.EXTRAS_IRT_DATA, value);
            sendBroadcast(notifyIntent);

        } else if (uuidStr.compareTo(SensorTagGatt.UUID_OPT_DATA.toString()) == 0) {
            Log.d(TAG, "Notify intent to optical");
            notifyIntent = new Intent(IntentNames.ACTION_OPT_CHANGE);
            notifyIntent.putExtra(IntentNames.EXTRAS_OPT_DATA, value);
            sendBroadcast(notifyIntent);

        } else if (uuidStr.compareTo(SensorTagGatt.UUID_MOV_DATA.toString()) == 0) {
            Log.d(TAG, "Notify intent to motion");
            notifyIntent = new Intent(IntentNames.ACTION_MOV_CHANGE);
            notifyIntent.putExtra(IntentNames.EXTRAS_MOV_DATA, value);
            sendBroadcast(notifyIntent);
        } else if (uuidStr.compareTo(SensorTagGatt.UUID_KEY_DATA.toString()) == 0) {
            Log.d(TAG, "Notify keys");
            notifyIntent = new Intent(IntentNames.ACTION_KEY_CHANGE);
            notifyIntent.putExtra(IntentNames.EXTRAS_KEY_DATA, value);
            sendBroadcast(notifyIntent);

        } else if (uuidStr.compareTo(SensorTagGatt.UUID_HUM_DATA.toString())== 0) {
            Log.d(TAG, "Notify intent to humidity");
            notifyIntent = new Intent(IntentNames.ACTION_HUM_CHANGE);
            notifyIntent.putExtra(IntentNames.EXTRAS_HUM_DATA, value);
            sendBroadcast(notifyIntent);
        } else if (uuidStr.compareTo(SensorTagGatt.UUID_BAR_DATA.toString()) == 0) {
            Log.d(TAG, "Notify intent to barometer");
            notifyIntent = new Intent(IntentNames.ACTION_BAR_CHANGE);
            notifyIntent.putExtra(IntentNames.EXTRAS_BAR_DATA, value);
            sendBroadcast(notifyIntent);

        } else {
            Log.e(TAG, "Notified something that isn't set up for");
        }
    }



    /**
     * Creates activity: Initializes variables and sets up UI. Binds service to this activity
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Log.d(TAG, "ON CREATE");

        /*
         * Gets intent from MainActivity
         */
        Intent intent = getIntent();
        mBluetoothDevice = intent.getParcelableExtra(IntentNames.EXTRAS_DEVICE);
        mDeviceName = intent.getStringExtra(IntentNames.EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(IntentNames.EXTRAS_DEVICE_ADDRESS);

        setTitle(mDeviceName);
        TextView deviceAddressText = (TextView) findViewById(R.id.device_address);
        deviceAddressText.setText("Device Address: " + mDeviceAddress);


        // Set up Buttons
        Button connectButton = (Button) findViewById(R.id.connectButton);
        connectButton.setOnClickListener(onClickListener);
        Button disconnectButton = (Button) findViewById(R.id.disconnectButton);
        disconnectButton.setOnClickListener(onClickListener);


        /*
         * Binds bluetooth service to this activity
         */
        Intent startService = new Intent(DeviceActivity.this, BleService.class);
        bindService(startService, mServiceConnection, BIND_AUTO_CREATE);
    }

    /**
     * Handles button clicks for connecting and disconnecting external BLE device.
     */
    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch(v.getId()) {
                case R.id.connectButton:
                    Log.d(TAG, "connect pressed");
                    if (!mConnected) {
                        mBleService.connect(mDeviceAddress);
                        mConnected = true;
                    } else {
                        Toast.makeText(DeviceActivity.this, "Already connected", Toast.LENGTH_SHORT).show();
                    }
                    break;

                case R.id.disconnectButton:
                    Log.d(TAG, "disconnect pressed");
                    if (mConnected) {
                        mBleService.disconnect(mDeviceAddress);
                        mConnected = false;
                    } else {
                        Toast.makeText(DeviceActivity.this, "Already disconnected", Toast.LENGTH_SHORT).show();
                    }
                    break;

                default:
                    Log.e(TAG, "Error in clicking");
                    break;
            }
        }
    };

    /**
     * Registers receiver with appropriate filter
     */
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "ON RESUME");
        Log.i(TAG, "Registering receiver");
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BleService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BleService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BleService.ACTION_DATA_READ);
        intentFilter.addAction(BleService.ACTION_DATA_WRITE);
        intentFilter.addAction(BleService.ACTION_DATA_NOTIFY);
        return intentFilter;
    }

    /**
     * Unregisters receiver
     */
    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "ON PAUSE");
        unregisterReceiver(mGattUpdateReceiver);
    }

    /**
     * Disconnects from device, closes connection and unbinds service from activity
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "ON DESTROY");
        if (mConnected) {
            mBleService.disconnect(mDeviceAddress);
        }
        mBleService.close();
        if (mBounded) {
            Log.d(TAG, "Unbinding service");
            unbindService(mServiceConnection);
            mBounded = false;
        }
        mBleService = null;
    }


    /**
     * Uses background thread that initializes Gatt services and updates user on status with a dialog
     *
     * @param gattServices A {@code List} of all the available {@code BluetoothGattService} on the device
     */
    private void setUpGattServices(final List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;

        List<BluetoothGattCharacteristic> charList = new ArrayList<BluetoothGattCharacteristic>();


        // Loops through available Gatt services to find number of characteristics
        for (int i = 0; i < gattServices.size(); i++) {
            BluetoothGattService s = gattServices.get(i);
            List<BluetoothGattCharacteristic> c = s.getCharacteristics();
            if (c.size() > 0) {
                for (int jj = 0; jj < c.size(); jj++) {
                    charList.add(c.get(jj));
                }
            }
        }
        Log.d(TAG, "Total services " + gattServices.size());
        Log.d(TAG,"Total characteristics " + charList.size());


        // Sets up UI progress dialog to display progress of background thread
        progressDialog = new ProgressDialog(DeviceActivity.this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setIndeterminate(true);
        progressDialog.setTitle("Discovering Services");
        progressDialog.setMessage("");
        progressDialog.setMax(100);
        progressDialog.setProgress(0);
        progressDialog.show();

        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {

                int nrNotificationsOn = 0;
                int maxNotifications;
                int servicesDiscovered = 0;
                int totalCharacteristics = 0;


                for (BluetoothGattService s : gattServices) {
                    List<BluetoothGattCharacteristic> chars = s.getCharacteristics();
                    totalCharacteristics += chars.size();
                }
                if (totalCharacteristics == 0) {
                    //Something bad happened, we have a problem
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.hide();
                            progressDialog.dismiss();
                            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(DeviceActivity.this);
                            alertDialogBuilder.setTitle("Error !");
                            alertDialogBuilder.setMessage(gattServices.size() + " Services found, but no characteristics found, device will be disconnected !");
                            alertDialogBuilder.setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
//                                    mBtLeService.refreshDeviceCache(mBtGatt);
//                                    //Try again
//                                    discoverServices();
                                    Log.e(TAG, "FIX THIS");
                                }
                            });
                            alertDialogBuilder.setNegativeButton("Disconnect",new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mBleService.disconnect(mDeviceAddress);
                                }
                            });
                            AlertDialog a = alertDialogBuilder.create();
                            a.show();
                        }
                    });
                    return;
                }


                final int final_totalCharacteristics = totalCharacteristics;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.setIndeterminate(false);
                        progressDialog.setTitle("Generating GUI");
                        progressDialog.setMessage("Found a total of " + gattServices.size() + " services with a total of " +
                                final_totalCharacteristics + " characteristics on this device" );

                    }
                });

                if (Build.VERSION.SDK_INT > 18) {
                    maxNotifications = 7;
                    Log.d(TAG, "Build over 18; 7 notifications allowed");

                }
                else {
                    maxNotifications = 4;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "Build under 18, only 4 notifications");
                            Toast.makeText(DeviceActivity.this, "Android version 4.3 detected, max 4 notifications enabled", Toast.LENGTH_LONG).show();
                        }
                    });
                }


                for (int ii = 0; ii < gattServices.size(); ii++) {
                    BluetoothGattService service = gattServices.get(ii);
                    List<BluetoothGattCharacteristic> chars = service.getCharacteristics();
                    if (chars.size() == 0) {
                        Log.e(TAG, "No characteristics found for this service !!!");
                        continue;
                    }
                    servicesDiscovered++;

                    final float serviceDiscoveredcalc = (float) servicesDiscovered;
                    final float serviceTotalcalc = (float) gattServices.size();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.setProgress((int) ((serviceDiscoveredcalc / (serviceTotalcalc - 1)) * 100));
                        }
                    });


                    String serviceUUID = service.getUuid().toString();
                    Log.d("DeviceActivity", "Configuring service with uuid : " + serviceUUID);


                    /*
                     One would hope that after executing those two functions the onCharacteristicChanged
                     callbacks would start rolling in. But there is one major detail missing. You need
                     to wait for your first onCharacteristicWrite callback before issuing a new write.
                     You can't do two writes immediately after each other. The Android BLE stack can only
                     handle one "remote" request at a time, meaning that if you do a write immediately
                     after another write the second request will be silently ignored. You can solve this
                     by either creating a queue for the API calls that require remote callbacks, or sleep
                     a short while between calls. See /src/com/ti/sensortag/ble/WriteQueue.java for
                     how the SensorTag app handles this issue.
                     */

                    /*
                     * NB: ACC, MAG, GYR are never found since they are included in the MOV sensor
                     */
                    if (serviceUUID.compareTo(SensorTagGatt.UUID_IRT_SERV.toString()) == 0)
                    {
                        Log.d(TAG, "IR TEMP FOUND (both ambient and infrared");
                        mBleService.enableNotifications(service, SensorTagGatt.UUID_IRT_DATA);
                        safeSleep();
                        mBleService.enableService(service, SensorTagGatt.UUID_IRT_CONF);
                        safeSleep();

                    } else if (serviceUUID.compareTo(SensorTagGatt.UUID_ACC_SERV.toString()) == 0)
                    {
                        Log.d(TAG, "ACCEL FOUND");
                        //see movement

                    } else if (serviceUUID.compareTo(SensorTagGatt.UUID_HUM_SERV.toString()) == 0)
                    {
                        Log.d(TAG, "HUMIDITY FOUND");
                        mBleService.enableNotifications(service, SensorTagGatt.UUID_HUM_DATA);
                        safeSleep();
                        mBleService.enableService(service, SensorTagGatt.UUID_HUM_CONF);
                        safeSleep();

                    } else if (serviceUUID.compareTo(SensorTagGatt.UUID_MAG_SERV.toString()) == 0)
                    {
                        Log.d(TAG, "MAGNETOMETER FOUND");
                        //see movement

                    } else if (serviceUUID.compareTo(SensorTagGatt.UUID_OPT_SERV.toString()) == 0)
                    {
                        Log.d(TAG, "OPTICAL SENSOR  (LUXOMETER) FOUND");
                        mBleService.enableNotifications(service, SensorTagGatt.UUID_OPT_DATA);
                        safeSleep();
                        mBleService.enableService(service, SensorTagGatt.UUID_OPT_CONF);
                        safeSleep();

                    } else if (serviceUUID.compareTo(SensorTagGatt.UUID_BAR_SERV.toString()) == 0)
                    {
                        Log.d(TAG, "BAROMETER FOUND");
                        mBleService.enableNotifications(service, SensorTagGatt.UUID_BAR_DATA);
                        safeSleep();
                        mBleService.enableService(service, SensorTagGatt.UUID_BAR_CONF);
                        safeSleep();

                    } else if (serviceUUID.compareTo(SensorTagGatt.UUID_GYR_SERV.toString()) == 0)
                    {
                        Log.d(TAG, "GYROSCOPE FOUND");
                        //see movement

                    } else if (serviceUUID.compareTo(SensorTagGatt.UUID_MOV_SERV.toString()) == 0)
                    {
                        Log.d(TAG, "MOTION SENSOR FOUND");
                        mBleService.enableNotifications(service, SensorTagGatt.UUID_MOV_DATA);
                        safeSleep();
                        mBleService.enableMotionService(service, SensorTagGatt.UUID_MOV_CONF, true);
                        safeSleep();

                    } else if (serviceUUID.compareTo(SensorTagGatt.UUID_KEY_SERV.toString()) == 0)
                    {
                        Log.d(TAG, "KEYS FOUND");

                        //Notify only service. No read or write
                        mBleService.enableNotifications(service, SensorTagGatt.UUID_KEY_DATA);
                        safeSleep();

                    } else if (serviceUUID.compareTo("0000ffb0-0000-1000-8000-00805f9b34fb") == 0)
                    {
                        Log.d(TAG, "Light service UUID FOUND");

                    } else if (serviceUUID.compareTo("f000ad00-0451-4000-b000-000000000000")==0)
                    {
                        Log.d(TAG, "Display service found");

                    } else if (serviceUUID.compareTo("f000ffc0-0451-4000-b000-000000000000") == 0)
                    {
                        Log.d(TAG, "TIOAD service found");

                    } else if (serviceUUID.compareTo(SensorTagGatt.UUID_TST_SERV.toString()) == 0)
                    {
                        Log.d(TAG, "IO SERVICE FOUND");
                        mBleService.enableService(service, SensorTagGatt.UUID_TST_CONF);
                        safeSleep();

                    } else if (serviceUUID.compareTo("f000ccc0-0451-4000-b000-000000000000") == 0)
                    {
                        Log.d(TAG, "OAD service found");

                    } else {
                        Log.e(TAG, "Unknown service");
                    }

                }

                final int numOfFragments = 7;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.setTitle("Enabling Services");
                        progressDialog.setMax(numOfFragments);
                        progressDialog.setProgress(0);

                        // Create the adapter that will return a fragment for each data in database
                        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

                        // Set up the ViewPager with the sections adapter.
                        mViewPager = (ViewPager) findViewById(R.id.container);
                        mViewPager.setAdapter(mSectionsPagerAdapter);

                        //two fragments on either side; not applicable for small datasets
                        mViewPager.setOffscreenPageLimit(2);
                    }
                });
                for (int i = 0; i < numOfFragments; i++) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.setProgress(progressDialog.getProgress() + 1);
                        }
                    });

                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        progressDialog.hide();
                        progressDialog.dismiss();
                    }
                });

                /*
                 * Fixes situation where turns on all IO after connection
                 */
                mBleService.changeIO(mBleService.getCharacteristicFromUUID(SensorTagGatt.UUID_TST_DATA.toString()), new byte[]{0});



            }
        });
        worker.start();

    }

    /**
     * Sleeps thread so all tasks in the BLE stack can be performed
     */
    private void safeSleep() {
        try {
            Log.d(TAG, "start sleep");
            Thread.sleep(200);
            Log.d(TAG, "end sleep");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


}
