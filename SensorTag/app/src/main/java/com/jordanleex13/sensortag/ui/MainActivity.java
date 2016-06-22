package com.jordanleex13.sensortag.ui;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.jordanleex13.sensortag.R;
import com.jordanleex13.sensortag.SensorTag.IntentNames;
import com.jordanleex13.sensortag.SensorTag.SensorTagUtil;

/**
 * Activity for scanning available bluetooth devices
 *
 * This activity uses Bluetooth Low Energy (BLE) to scan for available devices. These are displayed
 * in a listview which can then be clicked to connect to, bringing the user to another activity
 *
 * Code is based off Android Developer Bluetooth Low Energy
 *  https://developer.android.com/guide/topics/connectivity/bluetooth-le.html
 *
 *  @author      Jordan Lee
 */
public class MainActivity extends AppCompatActivity {

    //private static final String TAG = MainActivity.class.getSimpleName();


    /**
     * Request code used when bluetooth is disabled and want user to turn it on (see onActivityResult and onResume)
     */
    private static final int REQUEST_ENABLE_BT = 0;


    /**
     * BLE related
     */
    private BluetoothAdapter mBluetoothAdapter;
    private static BluetoothManager mBluetoothManager;
    private boolean mScanning;


    /**
     * UI elements
     */
    private ListView listView;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private ProgressBar scanProgress;


    /**
     * Starts up app by initializing variables, ensuring bluetooth and BLE is enabled
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Log.d(TAG, "On Create");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            //Log.e(TAG, "Device does not support Bluetooth");

            errorAlertDialog("This Android device does not have Bluetooth or there is an error in the " +
                    "bluetooth setup. Application cannot start, will exit.");
        }

        // Checks to determine whether BLE is supported on the device.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            errorAlertDialog("This Android device does not have Bluetooth Low Energy. Application cannot start, will exit");
        }

        // UI initialization
        Button startScanButton = (Button) findViewById(R.id.startScanButton);
        Button stopScanButton = (Button) findViewById(R.id.stopScanButton);
        Button refreshScanButton = (Button) findViewById(R.id.refreshScanButton);
        startScanButton.setOnClickListener(onClickListener);
        stopScanButton.setOnClickListener(onClickListener);
        refreshScanButton.setOnClickListener(onClickListener);
        scanProgress = (ProgressBar) findViewById(R.id.scanProgressBar);

        listView = (ListView) findViewById(R.id.listOfBTDevices);
        mLeDeviceListAdapter = new LeDeviceListAdapter(this);
        listView.setAdapter(mLeDeviceListAdapter);

        listView.setOnItemClickListener(onItemClickListener);

    }

    private void errorAlertDialog(String message) {
        AlertDialog.Builder aB = new AlertDialog.Builder(MainActivity.this);
        aB.setTitle("Error !");
        aB.setCancelable(false);
        aB.setMessage(message);
        aB.setNegativeButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                System.exit(0);
            }
        });
        AlertDialog a = aB.create();
        a.show();
    }



    /**
     * Handles button clicks to start and stop bluetooth scanning
     */
    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.startScanButton:
                    //Log.d(TAG, "Pressed start button");

                    // Used for Low Energy Bluetooth
                    scanLeDevice(true);
                    break;

                case R.id.stopScanButton:
                    //Log.d(TAG, "Pressed stop button");
                    scanLeDevice(false);
                    break;

                case R.id.refreshScanButton:
                    mLeDeviceListAdapter.clear();
                    mLeDeviceListAdapter.notifyDataSetChanged();
                    break;

                default:
                    break;
            }
        }
    };

    /**
     * Handles listview clicks; will launch new activity if device clicked is a SensorTag2
     */
    AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
            if (device == null) {
                //Log.e(TAG, "Error. Device appears in list but is not in range");
                Toast.makeText(MainActivity.this, "Device not in range. Refresh list", Toast.LENGTH_SHORT).show();
                return;
            }

            /*
             * Only starts DeviceActivity if device is a SensorTag
             */
            if (SensorTagUtil.isSensorTag2(device)) {

                //Log.d(TAG, device.getName() + "\n" + device.getAddress());

                final Intent intent = new Intent(MainActivity.this, DeviceActivity.class);
                intent.putExtra(IntentNames.EXTRAS_DEVICE, device);
                intent.putExtra(IntentNames.EXTRAS_DEVICE_NAME, device.getName());
                intent.putExtra(IntentNames.EXTRAS_DEVICE_ADDRESS, device.getAddress());

                if (mScanning) {
                    scanLeDevice(false);
                }
                startActivity(intent);

            } else {
                Toast.makeText(MainActivity.this, "Not a SensorTag device", Toast.LENGTH_SHORT).show();
            }
        }

    };


    /**
     * Checks for bluetooth when activity is resumed.
     */
    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

    }

    /**
     * Stops scan and clears current list of bluetooth devices
     */
    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        //Don't know if same BT devices will be there when we return
        mLeDeviceListAdapter.clear();
    }


    /**
     * Starts or stops bluetooth LE scan with user input
     *
     * @param enable defines whether to scan or not
     *               if true then scan
     *               else stop scan
     */
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            //Log.d(TAG, "Starting scan");
            mScanning = true;
            scanProgress.setVisibility(View.VISIBLE);
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            //Log.d(TAG, "Stopping scan and refreshing list");
            mScanning = false;
            scanProgress.setVisibility(View.INVISIBLE);
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }

    }

    /**
     * Callback for the bluetooth scan that will add device to the list adapter
     */
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            mLeDeviceListAdapter.addDevice(device);
            mLeDeviceListAdapter.updateRssiValue(device, rssi);
            mLeDeviceListAdapter.notifyDataSetChanged();

        }
    };


    /**
     * Clears all bluetooth functionality
     */
    @Override
    protected void onDestroy() {
        //Log.i(TAG, "On Destroy");
        mBluetoothManager = null;
        mBluetoothAdapter = null;
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                //Log.d(TAG, "Selected settings");
                break;
            default:
                break;
        }
        return true;
    }

    /**
     * Gets result from the popup activity when bluetooth is disabled (see onResume for function call)
     *
     * @param requestCode REQUEST_ENABLE_BT
     * @param resultCode  either RESULT_OK or RESULT_CANCELLED
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK)
                return;
                //Log.d(TAG, "Bluetooth enabled");
            else {
                //Log.e(TAG, "Bluetooth not enabled");
                finish();
            }
        }
    }


}

