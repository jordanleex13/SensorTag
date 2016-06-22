package com.jordanleex13.sensortag.profiles;


import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Html;
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
 * A simple {@link Fragment} subclass that displays motion data.
 *
 * Potential problems with getting data:
 *  Battery- http://mobilemodding.info/2015/06/ti-sensortag-2-power-consumption-analysys/
 */
public class MotionFragment extends Fragment {

    //private static final String TAG = MotionFragment.class.getSimpleName();
    private static final String FRAGMENT_POSITION = "com.jordanleex13.sensortag.MotionFragment.FRAGMENT_POSITION";

    /**
     * UI related variables
     */
    private int sectionNumber;
    private TextView positionText;
    private TextView accelData;
    private TextView gyroData;
    private TextView magData;
    private SeekBar periodBar;
    private TextView periodLength;
    private Switch sensorSwitch;
    private Switch wakeOnShakeSwitch;

    /**
     * BLE related variables
     */
    private BleService mBleService;
    private BluetoothGattService mThis;
    private static boolean mFirstTime = true;
    private static final int periodMinVal = 100;


    public MotionFragment() {
        // Required empty public constructor
    }

    /**
     * Instantiates a new fragment. Puts fragment number into a bundle that will be retrieved in {@code onCreate}
     *
     * @param position  The fragment number
     * @return          MotionFragment
     */
    public static MotionFragment newInstance(int position) {
        Bundle args = new Bundle();
        MotionFragment fragment = new MotionFragment();
        args.putInt(FRAGMENT_POSITION, position);
        fragment.setArguments(args);

        return fragment;
    }

    /**
     * Retrieves bundle and instantiates BLE service and motion service
     *
     * @param savedInstanceState
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sectionNumber = getArguments().getInt(FRAGMENT_POSITION);
        mBleService = BleService.getInstance();
        mThis = mBleService.getServiceFromUUID(SensorTagGatt.UUID_MOV_SERV.toString());
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
        View v = inflater.inflate(R.layout.fragment_motion, container, false);

        positionText = (TextView) v.findViewById(R.id.fragment_position);
        positionText.setText("Fragment " + String.valueOf(sectionNumber) + ": 9-Axis Motion Sensor");

        accelData = (TextView) v.findViewById(R.id.accel_data);
        accelData.setText("X:0.00G, Y:0.00G, Z:0.00G");

        gyroData = (TextView) v.findViewById(R.id.gyro_data);
        gyroData.setText("X:0.00°/s, Y:0.00°/s, Z:0.00°/s");

        magData = (TextView) v.findViewById(R.id.mag_data);
        magData.setText("X:0.00mT, Y:0.00mT, Z:0.00mT");

        periodLength = (TextView) v.findViewById(R.id.periodLength);
        periodLength.setText("Sensor period (currently : " + ((90 * 10) + periodMinVal) + "ms)");

        periodBar = (SeekBar) v.findViewById(R.id.periodBar);
        periodBar.setMax(245); // because 0-245 corresponds to 100-2550     formula: * 10 + 100
        periodBar.setProgress(90);
        periodBar.setOnSeekBarChangeListener(onSeekBarChangeListener);

        sensorSwitch = (Switch) v.findViewById(R.id.sensorSwitch);
        sensorSwitch.setOnCheckedChangeListener(onCheckedChangeListener);
        sensorSwitch.setChecked(true);

        wakeOnShakeSwitch = (Switch) v.findViewById(R.id.wakeOnShakeSwitch);
        wakeOnShakeSwitch.setOnCheckedChangeListener(onCheckedChangeListener);
        wakeOnShakeSwitch.setChecked(true);


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
            //Log.d(TAG, "Period Stop");
            int period = periodMinVal + (seekBar.getProgress() * 10);

            if (period > 2450) period = 2450;
            if (period < 100) period = 100;
            byte p = (byte)((period / 10) + 10);

            //Log.d(TAG, "Period characteristic set to: " + period);
            mBleService.changePeriod(mBleService.getCharacteristicFromUUID(SensorTagGatt.UUID_MOV_PERI.toString()), p);
        }
    };

    /**
     * Handles switch clicks which enable/disable service or turns on the wake on shake feature.
     */
    CompoundButton.OnCheckedChangeListener onCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

            if (!isChecked) {

                switch (buttonView.getId()) {
                    case R.id.sensorSwitch:
                        mBleService.disableNotifications(mThis, SensorTagGatt.UUID_MOV_DATA);
                        positionText.setAlpha(0.4f);
                        accelData.setAlpha(0.4f);
                        gyroData.setAlpha(0.4f);
                        magData.setAlpha(0.4f);
                        periodLength.setAlpha(0.4f);
                        periodBar.setEnabled(false);
                        break;

                    case R.id.wakeOnShakeSwitch:
                        mBleService.enableMotionService(mThis, SensorTagGatt.UUID_MOV_CONF, false);
                        break;

                    default:
                        break;
                }

            } else {
                switch (buttonView.getId()) {

                    case R.id.sensorSwitch:
                        if (!mFirstTime) {
                            mBleService.enableNotifications(mThis, SensorTagGatt.UUID_MOV_DATA);
                            positionText.setAlpha(1.0f);
                            accelData.setAlpha(1.0f);
                            gyroData.setAlpha(1.0f);
                            magData.setAlpha(1.0f);
                            periodLength.setAlpha(1.0f);
                            periodBar.setEnabled(true);
                        } else {
                            mFirstTime = false;
                            //Log.d(TAG, "FIRST TIME");
                        }
                        break;

                    case R.id.wakeOnShakeSwitch:
                        mBleService.enableMotionService(mThis, SensorTagGatt.UUID_MOV_CONF, true);
                        break;

                    default:
                        break;

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
        getActivity().registerReceiver(motionUpdateReceiver, makeMotionUpdateIntentFilter());
        //Log.i(TAG, "Registering MOTION receiver");
    }

    /**
     * Unregisters the broadcast receiver
     */
    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(motionUpdateReceiver);
        //Log.i(TAG, "Unregistering MOTION receiver");
    }

    /**
     * Receives updates from {@code mGattUpdateReceiver#ACTION_DATA_NOTIFY} and displays updated data
     * Converts accel, gyro, and mag data separately using different conversions of the same byte[]
     */
    private final BroadcastReceiver motionUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            final String action = intent.getAction();

            if (IntentNames.ACTION_MOV_CHANGE.equals(action)) {

                //Log.i(TAG, "***************** MOTION sensed *****************");
                byte[] value = intent.getByteArrayExtra(IntentNames.EXTRAS_MOV_DATA);
                Point3D v;

                v = SensorConversion.MOVEMENT_ACC.convert(value);
                accelData.setText(Html.fromHtml(String.format("<font color=#FF0000>X:%.2fG</font>," +
                        "<font color=#00967D>Y:%.2fG</font>, <font color=#00000>Z:%.2fG</font>", v.x,v.y,v.z)));

                v = SensorConversion.MOVEMENT_GYRO.convert(value);
                gyroData.setText(Html.fromHtml(String.format("<font color=#FF0000>X:%.2f°/s</font>, " +
                        "<font color=#00967D>Y:%.2f°/s</font>, <font color=#00000>Z:%.2f°/s</font>", v.x, v.y, v.z)));

                v = SensorConversion.MOVEMENT_MAG.convert(value);
                magData.setText(Html.fromHtml(String.format("<font color=#FF0000>X:%.2fuT</font>, " +
                        "<font color=#00967D>Y:%.2fuT</font>, <font color=#00000>Z:%.2fuT</font>", v.x, v.y, v.z)));
            }
        }
    };

    private static IntentFilter makeMotionUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(IntentNames.ACTION_MOV_CHANGE);
        return intentFilter;
    }

}
