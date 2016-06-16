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
 * A simple {@link Fragment} subclass that displays barometric data.
 */
public class BarometerFragment extends Fragment {

    private static final String TAG = BarometerFragment.class.getSimpleName();
    private static final String FRAGMENT_POSITION = "com.jordanleex13.sensortag.BarometerFragment.FRAGMENT_POSITION";

    /**
     * UI related variables
     */
    private int sectionNumber;
    private TextView positionText;
    private TextView barData;
    private TextView periodLength;
    private SeekBar periodBar;
    private Switch sensorSwitch;

    /**
     * BLE related variables
     */
    private BleService mBleService;
    private BluetoothGattService mThis;
    private static boolean mFirstTime = true;
    private static final int periodMinVal = 100;


    public BarometerFragment() {
        // Required empty public constructor
    }

    /**
     * Instantiates a new fragment. Puts fragment number into a bundle that will be retrieved in {@code onCreate}
     *
     * @param position  The fragment number
     * @return          BarometerFragment
     */
    public static BarometerFragment newInstance(int position) {
        BarometerFragment fragment = new BarometerFragment();
        Bundle args = new Bundle();
        args.putInt(FRAGMENT_POSITION, position);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Retrieves bundle and instantiates BLE service and barometer service
     *
     * @param savedInstanceState
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sectionNumber = getArguments().getInt(FRAGMENT_POSITION);
        mBleService = BleService.getInstance();
        mThis = mBleService.getServiceFromUUID(SensorTagGatt.UUID_BAR_SERV.toString());
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
        View v = inflater.inflate(R.layout.fragment_barometer, container, false);

        positionText = (TextView) v.findViewById(R.id.fragment_position);
        positionText.setText("Fragment " + sectionNumber + ": Barometer");

        barData = (TextView) v.findViewById(R.id.bar_data);
        barData.setText("Pressure Data: 0.0mBar, 0.0m");

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
     * Handles slider touches which change period of service.
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
            mBleService.changePeriod(mBleService.getCharacteristicFromUUID(SensorTagGatt.UUID_BAR_PERI.toString()), p);
        }
    };

    /**
     * Handles switch clicks which enable or disable the service
     */
    CompoundButton.OnCheckedChangeListener onCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (!isChecked) {
                mBleService.disableNotifications(mThis, SensorTagGatt.UUID_BAR_DATA);
                positionText.setAlpha(0.4f);
                barData.setAlpha(0.4f);
                periodLength.setAlpha(0.4f);
                periodBar.setEnabled(false);
            } else {
                if (!mFirstTime) {
                    mBleService.enableNotifications(mThis, SensorTagGatt.UUID_BAR_DATA);
                    positionText.setAlpha(1.0f);
                    barData.setAlpha(1.0f);
                    periodLength.setAlpha(1.0f);
                    periodBar.setEnabled(true);
                } else {
                    mFirstTime = false;
                    Log.d(TAG, "FIRST TIME. Service already enabled");
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
        getActivity().registerReceiver(barometerUpdateReceiver, makeBarometerUpdateIntentFilter());
        Log.i(TAG, "Registering BAROMETER receiver");
    }

    /**
     * Unregisters the broadcast receiver
     */
    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(barometerUpdateReceiver);
        Log.i(TAG, "Unregistering BAROMETER receiver");
    }

    /**
     * Receives updates from {@code mGattUpdateReceiver#ACTION_DATA_NOTIFY} and displays updated data
     */
    private final BroadcastReceiver barometerUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (IntentNames.ACTION_BAR_CHANGE.equals(action)) {
                byte[] value = intent.getByteArrayExtra(IntentNames.EXTRAS_BAR_DATA);
                Point3D v = SensorConversion.BAROMETER.convert(value);

                barData.setText(String.format("Pressure Data: %.1f mBar", v.x / 100));

            }
        }
    };

    private static IntentFilter makeBarometerUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(IntentNames.ACTION_BAR_CHANGE);
        return intentFilter;
    }
}
