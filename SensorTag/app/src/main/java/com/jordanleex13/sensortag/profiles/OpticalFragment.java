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
 * A simple {@link Fragment} subclass that displays light intensity data.
 */
public class OpticalFragment extends Fragment {

    private static final String TAG = OpticalFragment.class.getSimpleName();
    private static final String FRAGMENT_POSITION = "com.jordanleex13.sensortag.OpticalFragment.FRAGMENT_POSITION";

    /**
     * UI related variables
     */
    private int sectionNumber;
    private TextView positionText;
    private TextView opticalData;
    private SeekBar periodBar;
    private TextView periodLength;
    private Switch sensorSwitch;

    /**
     * BLE related variables
     */
    private BleService mBleService;
    private BluetoothGattService mThis;
    private static boolean mFirstTime;
    private static final int periodMinVal = 100;


    public OpticalFragment() {
        // Required empty public constructor
    }

    /**
     * Instantiates a new fragment. Puts fragment number into a bundle that will be retrieved in {@code onCreate}
     *
     * @param position      The fragment number
     * @return              OpticalFragment
     */
    public static OpticalFragment newInstance(int position) {
        Bundle args = new Bundle();
        OpticalFragment fragment = new OpticalFragment();
        args.putInt(FRAGMENT_POSITION, position);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Retrieves bundle and instantiates BLE service and optical service
     *
     * @param savedInstanceState
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sectionNumber = getArguments().getInt(FRAGMENT_POSITION);
        mBleService = BleService.getInstance();
        mThis = mBleService.getServiceFromUUID(SensorTagGatt.UUID_OPT_SERV.toString());
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
        View v = inflater.inflate(R.layout.fragment_optical, container, false);


        positionText = (TextView) v.findViewById(R.id.fragment_position);
        positionText.setText("Fragment " + String.valueOf(sectionNumber) + ": Luxometer");

        opticalData = (TextView) v.findViewById(R.id.optical_data);
        opticalData.setText("Light Intensity: 0.0 Lux");

        periodLength = (TextView) v.findViewById(R.id.periodLength);
        periodLength.setText("Sensor period (currently : " + ((70 * 10) + periodMinVal) + "ms)");

        periodBar = (SeekBar) v.findViewById(R.id.periodBar);
        periodBar.setMax(245);              // because 0-245 corresponds to 100-2550     formula: * 10 + 100
        periodBar.setProgress(70);
        periodBar.setOnSeekBarChangeListener(onSeekBarChangeListener);

        sensorSwitch = (Switch) v.findViewById(R.id.sensorSwitch);
        sensorSwitch.setOnCheckedChangeListener(onCheckedChangeListener);
        sensorSwitch.setChecked(true);

        return v;
    }



    /**
     * Handles slider touches which change period of service
     * Resolution 10 ms. Range 100 ms (0x0A) to 2.55 sec (0xFF). Default 800 milliseconds (0x50).
     */
    SeekBar.OnSeekBarChangeListener onSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            //Log.d(TAG, "Period changed : " + progress);
            periodLength.setText("Sensor period (currently : " + ((progress * 10) + periodMinVal) + "ms)");

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            Log.d(TAG, "Period Start");
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            Log.d(TAG, "Period Stop");
            int period = periodMinVal + (seekBar.getProgress() * 10);

            if (period > 2450) period = 2450;
            if (period < 100) period = 100;
            byte p = (byte)((period / 10) + 10);

            Log.d(TAG, "Period characteristic set to: " + period);
            mBleService.changePeriod(mBleService.getCharacteristicFromUUID(SensorTagGatt.UUID_OPT_PERI.toString()), p);
        }
    };

    /**
     * Handles switch clicks which enable or disable the service
     */
    CompoundButton.OnCheckedChangeListener onCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (!isChecked) {
                mBleService.disableNotifications(mThis, SensorTagGatt.UUID_OPT_DATA);
                positionText.setAlpha(0.4f);
                opticalData.setAlpha(0.4f);
                periodLength.setAlpha(0.4f);
                periodBar.setEnabled(false);
            } else {
                if (!mFirstTime) {
                    mBleService.enableNotifications(mThis, SensorTagGatt.UUID_OPT_DATA);
                    positionText.setAlpha(1.0f);
                    opticalData.setAlpha(1.0f);
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
     * Registers broadcast receiver
     */
    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(opticalUpdateReceiver, makeOpticalUpdateIntentFilter());
        Log.i(TAG, "REGISTERING optical RECEIVER");
    }

    /**
     * Unregisters broadcast receiver
     */
    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(opticalUpdateReceiver);
        Log.i(TAG, "UNREGISTERING optical RECEIVER");
    }

    /**
     * Receives updates from {@code mGattUpdateReceiver#ACTION_DATA_NOTIFY} and displays updated data
     */
    private final BroadcastReceiver opticalUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (IntentNames.ACTION_OPT_CHANGE.equals(action)) {
                byte[] value = intent.getByteArrayExtra(IntentNames.EXTRAS_OPT_DATA);
                Point3D v = SensorConversion.LUXOMETER.convert(value);
                opticalData.setText("Light intensity: " + String.format("%.2f Lux", v.x));
            }
        }
    };

    private static IntentFilter makeOpticalUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(IntentNames.ACTION_OPT_CHANGE);
        return intentFilter;
    }

}


