package com.jordanleex13.sensortag.profiles;


import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.jordanleex13.sensortag.BleService;
import com.jordanleex13.sensortag.R;
import com.jordanleex13.sensortag.SensorTag.IntentNames;
import com.jordanleex13.sensortag.SensorTag.SensorConversion;
import com.jordanleex13.sensortag.SensorTag.SensorTagGatt;
import com.jordanleex13.sensortag.models.Point3D;

/**
 * A simple {@link Fragment} subclass that display humidity data.
 */
public class HumidityFragment extends Fragment {

    private static final String TAG = HumidityFragment.class.getSimpleName();
    private static final String FRAGMENT_POSITION = "com.jordanleex13.sensortag.HumidityFragment.FRAGMENT_POSITION";

    /**
     * UI related variables
     */
    private int sectionNumber;
    private TextView positionText;
    private TextView humData;
    private SeekBar periodBar;
    private TextView periodLength;
    private Switch sensorSwitch;

    /**
     * BLE related variables
     */
    private BleService mBleService;
    private BluetoothGattService mThis;
    private static boolean mFirstTime = true;
    private static final int periodMinVal = 100;


    public HumidityFragment() {
        // Required empty public constructor
    }

    /**
     * Instantiates a new fragment. Puts fragment number into a bundle that will be retrieved in {@code onCreate}
     *
     * @param position  The fragment number
     * @return          HumidityFragment
     */
    public static HumidityFragment newInstance(int position) {
        Bundle args = new Bundle();
        HumidityFragment fragment = new HumidityFragment();
        args.putInt(FRAGMENT_POSITION, position);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Retrieves bundle and instantiates BLE service and humidity service
     *
     * @param savedInstanceState
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sectionNumber = getArguments().getInt(FRAGMENT_POSITION);
        mBleService = BleService.getInstance();
        mThis = mBleService.getServiceFromUUID(SensorTagGatt.UUID_HUM_SERV.toString());
    }

    /**
     * Sets up various UI elements and sets listeners for periodBar and sensorSwitch
     *
     * @param inflater              Used to inflate the layout
     * @param container             Container for the fragment layout
     * @param savedInstanceState
     * @return                      Returns the view that has been inflated
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_humidity, container, false);

        positionText = (TextView) v.findViewById(R.id.fragment_position);
        positionText.setText("Fragment " + String.valueOf(sectionNumber) + ": Humidity Sensor");

        humData = (TextView) v.findViewById(R.id.hum_data);
        humData.setText("Humidity: 0.0%rH");

        periodLength = (TextView) v.findViewById(R.id.periodLength);
        periodLength.setText("Sensor period (currently : " + ((90 * 10) + periodMinVal) + "ms)");

        periodBar = (SeekBar) v.findViewById(R.id.periodBar);
        periodBar.setMax(245); // because 0-245 corresponds to 100-2550     formula: * 10 + 100
        periodBar.setProgress(90);
        periodBar.setOnSeekBarChangeListener(onSeekBarChangeListener);

        sensorSwitch = (Switch) v.findViewById(R.id.sensorSwitch);
        sensorSwitch.setOnCheckedChangeListener(onCheckedChangeListener);
        sensorSwitch.setChecked(true);

        return v;
    }

    /**
     * Handles slider touches which change period of service
     * Resolution 10 ms. Range 100 ms (0x0A) to 2.55 sec (0xFF). Default 1 second (0x64).
     */
    SeekBar.OnSeekBarChangeListener onSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            periodLength.setText("Sensor period (currently : " + ((progress * 10) + periodMinVal) + "ms)");
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            Log.d(TAG, "Period Stop");
            int period = periodMinVal + (seekBar.getProgress() * 10);

            if (period > 2450) period = 2450;
            if (period < 100) period = 100;
            byte p = (byte)((period / 10) + 10);

            Log.d(TAG, "Period characteristic set to: " + period);
            mBleService.changePeriod(mBleService.getCharacteristicFromUUID(SensorTagGatt.UUID_HUM_PERI.toString()), p);
        }
    };


    /**
     * Handles switch clicks which enable or disable the service
     */
    CompoundButton.OnCheckedChangeListener onCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (!isChecked) {
                mBleService.disableNotifications(mThis, SensorTagGatt.UUID_HUM_DATA);
                //mBleService.disableService(mThis, SensorTagGatt.UUID_IRT_CONF);
                positionText.setAlpha(0.4f);
                humData.setAlpha(0.4f);
                periodLength.setAlpha(0.4f);
                periodBar.setEnabled(false);
            } else {
                if (!mFirstTime) {
                    mBleService.enableNotifications(mThis, SensorTagGatt.UUID_HUM_DATA);
                    //mBleService.enableService(mThis, SensorTagGatt.UUID_IRT_CONF);
                    positionText.setAlpha(1.0f);
                    humData.setAlpha(1.0f);
                    periodLength.setAlpha(1.0f);
                    periodBar.setEnabled(true);
                } else {
                    mFirstTime = false;
                    Log.d(TAG, "FIRST TIME");
                }

            }
        }
    };


    /**
     * Registers the broadcast receiver
     */
    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(humidityUpdateReceiver, makeHumidityUpdateIntentFilter());
        Log.i(TAG, "Registering HUMIDITY receiver");
    }

    /**
     * Unregisters the broadcast receiver
     */
    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(humidityUpdateReceiver);
        Log.i(TAG, "Unregistering HUMIDITY receiver");
    }

    /**
     * Receives updates from {@code mGattUpdateReceiver#ACTION_DATA_NOTIFY} and displays updated data
     */
    private final BroadcastReceiver humidityUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (IntentNames.ACTION_HUM_CHANGE.equals(action)) {
                byte[] value = intent.getByteArrayExtra(IntentNames.EXTRAS_HUM_DATA);
                Point3D v = SensorConversion.HUMIDITY2.convert(value);
                humData.setText(String.format("Humidity: %.1f %%rH", v.x));
            }
        }
    };

    private static IntentFilter makeHumidityUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(IntentNames.ACTION_HUM_CHANGE);
        return intentFilter;
    }

}
