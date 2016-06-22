package com.jordanleex13.sensortag.profiles;


import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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
 * A simple {@link Fragment} subclass that displays temperature data
 */
public class TemperatureFragment extends Fragment {

    //private static final String TAG = TemperatureFragment.class.getSimpleName();
    private static final String FRAGMENT_POSITION = "com.jordanleex13.sensortag.TemperatureFragment.FRAGMENT_POSITION";

    /**
     * UI related variables
     */
    private int sectionNumber;
    private TextView positionText;
    private TextView ambientTemperature;
    private TextView irTemperature;
    private SeekBar periodBar;
    private TextView periodLength;
    private Switch sensorSwitch;

    /**
     * BLE related variables
     */
    private BleService mBleService;
    private BluetoothGattService mThis;
    private static boolean mFirstTime = true;
    private static final int periodMinVal = 300;


    public TemperatureFragment() {
        // Required empty public constructor
    }

    /**
     * Instantiates a new fragment. Puts fragment number into a bundle that will be retrieved in {@code onCreate}
     *
     * @param position      The fragment number
     * @return              TemperatureFragment
     */
    public static TemperatureFragment newInstance(int position) {
        Bundle args = new Bundle();
        TemperatureFragment fragment = new TemperatureFragment();
        args.putInt(FRAGMENT_POSITION, position);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Retrieves bundle and instantiates BLE service and temperature service
     *
     * @param savedInstanceState
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sectionNumber = getArguments().getInt(FRAGMENT_POSITION);
        mBleService = BleService.getInstance();
        mThis = mBleService.getServiceFromUUID(SensorTagGatt.UUID_IRT_SERV.toString());
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
        View v = inflater.inflate(R.layout.fragment_temperature, container, false);

        positionText = (TextView) v.findViewById(R.id.fragment_position);
        positionText.setText("Fragment " + String.valueOf(sectionNumber) + ": Temperature Sensor");

        ambientTemperature = (TextView) v.findViewById(R.id.ambient_temperature);
        ambientTemperature.setText("Ambient Temperature: 0.0'C");
        irTemperature = (TextView) v.findViewById(R.id.ir_temperature);
        irTemperature.setText("IR Temperature: 0.0'C");

        periodLength = (TextView) v.findViewById(R.id.periodLength);
        periodLength.setText("Sensor period (currently : " + ((70 * 10) + periodMinVal) + "ms)");

        periodBar = (SeekBar) v.findViewById(R.id.periodBar);
        periodBar.setMax(225); // because 0-225 corresponds to 300-2550     formula: * 10 + 300
        periodBar.setProgress(70);
        periodBar.setOnSeekBarChangeListener(onSeekBarChangeListener);

        sensorSwitch = (Switch) v.findViewById(R.id.sensorSwitch);
        sensorSwitch.setOnCheckedChangeListener(onCheckedChangeListener);
        sensorSwitch.setChecked(true);


        return v;
    }

    /**
     * Handles slider touches which change period of service
     * Resolution 10 ms. Range 300 ms (0x1E) to 2.55 sec (0xFF). Default 1 second (0x64)
     */
    SeekBar.OnSeekBarChangeListener onSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            periodLength.setText("Sensor period (currently : " + ((progress * 10) + periodMinVal) + "ms)");
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            //Log.d(TAG, "Period Start");
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            //Log.d(TAG, "Period Stop");
            int period = periodMinVal + (seekBar.getProgress() * 10);

            if (period > 2450) period = 2450;
            if (period < 100) period = 100;
            byte p = (byte)((period / 10) + 10);

            //Log.d(TAG, "Period characteristic set to: " + period);
            mBleService.changePeriod(mBleService.getCharacteristicFromUUID(SensorTagGatt.UUID_IRT_PERI.toString()), p);

        }
    };


    /**
     * Handles switch clicks which enable or disable the service
     */
    CompoundButton.OnCheckedChangeListener onCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (!isChecked) {
                mBleService.disableNotifications(mThis, SensorTagGatt.UUID_IRT_DATA);
                positionText.setAlpha(0.4f);
                ambientTemperature.setAlpha(0.4f);
                irTemperature.setAlpha(0.4f);
                periodLength.setAlpha(0.4f);
                periodBar.setEnabled(false);
            } else {
                if (!mFirstTime) {
                    mBleService.enableNotifications(mThis, SensorTagGatt.UUID_IRT_DATA);
                    positionText.setAlpha(1.0f);
                    ambientTemperature.setAlpha(1.0f);
                    irTemperature.setAlpha(1.0f);
                    periodLength.setAlpha(1.0f);
                    periodBar.setEnabled(true);
                } else {
                    mFirstTime = false;
                    //Log.d(TAG, "FIRST TIME");
                }

            }
        }
    };

    /**
     * Registers broadcast receiver
     */
    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(temperatureUpdateReceiver, makeTemperatureUpdateIntentFilter());
        //Log.i(TAG, "REGISTERING temperature RECEIVER");
    }

    /**
     * Unregisters broadcast receiver
     */
    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(temperatureUpdateReceiver);
        //Log.i(TAG, "UNREGISTERING temperature RECEIVER");
    }

    /**
     * Receives updates from {@code mGattUpdateReceiver#ACTION_DATA_NOTIFY} and displays updated data
     */
    private final BroadcastReceiver temperatureUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (IntentNames.ACTION_IRT_CHANGE.equals(action)) {
                byte[] value = intent.getByteArrayExtra(IntentNames.EXTRAS_IRT_DATA);
                Point3D v = SensorConversion.IR_TEMPERATURE.convert(value);
                ambientTemperature.setText(String.format("Ambient Temperature: %.1f°C", v.x));
                irTemperature.setText(String.format("IR Temperature: %.1f°C", v.z));
            }
        }
    };

    private static IntentFilter makeTemperatureUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(IntentNames.ACTION_IRT_CHANGE);
        return intentFilter;
    }

}
