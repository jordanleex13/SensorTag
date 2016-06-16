package com.jordanleex13.sensortag.profiles;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.jordanleex13.sensortag.BleService;
import com.jordanleex13.sensortag.R;
import com.jordanleex13.sensortag.SensorTag.SensorTagGatt;

/**
 * A simple {@link Fragment} subclass that controls the IO elements in the SensorTag2.
 *
 * {@see https://evothings.com/forum/viewtopic.php?t=1514}
 */
public class IOFragment extends Fragment {

    private static final String TAG = IOFragment.class.getSimpleName();
    private static final String FRAGMENT_POSITION = "com.jordanleex13.sensortag.IOFragment.FRAGMENT_POSITION";

    /**
     * UI related variables
     */
    private int sectionNumber;
    private TextView positionText;
    private Switch toggleRedSwitch;
    private Switch toggleGreenSwitch;
    private Switch toggleBuzzerSwitch;

    private boolean redOn = false;
    private boolean greenOn = false;
    private boolean buzzerOn = false;

    /**
     * BLE related variables
     */
    private BleService mBleService;
    private BluetoothGattService mThis;
    private BluetoothGattCharacteristic mDataCharacteristic;


    public IOFragment() {
        // Required empty public constructor
    }

    /**
     * Instantiates a new fragment. Puts fragment number into a bundle that will be retrieved in {@code onCreate}
     *
     * @param position      The fragment number
     * @return              IOFragment
     */
    public static IOFragment newInstance(int position) {
        IOFragment fragment = new IOFragment();
        Bundle args = new Bundle();
        args.putInt(FRAGMENT_POSITION, position);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Retrieves bundle, instantiates BLE service and IO service
     *
     * @param savedInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sectionNumber = getArguments().getInt(FRAGMENT_POSITION);
        mBleService = BleService.getInstance();
        mThis = mBleService.getServiceFromUUID(SensorTagGatt.UUID_TST_SERV.toString());
        mDataCharacteristic = mBleService.getCharacteristicFromUUID(SensorTagGatt.UUID_TST_DATA.toString());
    }

    /**
     * Sets up various UI elements and sets listeners for the switches
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
        View v = inflater.inflate(R.layout.fragment_io, container, false);

        positionText = (TextView) v.findViewById(R.id.fragment_position);
        positionText.setText("Fragment " + String.valueOf(sectionNumber) + ": IO service");

        toggleRedSwitch = (Switch) v.findViewById(R.id.toggleRedLight);
        toggleGreenSwitch = (Switch) v.findViewById(R.id.toggleGreenLight);
        toggleBuzzerSwitch = (Switch) v.findViewById(R.id.toggleBuzzer);

        toggleRedSwitch.setOnCheckedChangeListener(onCheckedChangeListener);
        toggleGreenSwitch.setOnCheckedChangeListener(onCheckedChangeListener);
        toggleBuzzerSwitch.setOnCheckedChangeListener(onCheckedChangeListener);

        return v;
    }


    /**
     * Listener for the switches in the UI. Follows the below format for writing byte arrays
     */
    CompoundButton.OnCheckedChangeListener onCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {

//        Bit 0      2^0        1 = red
//        Bit 1      2^1        2 = green
//                              3 = red + green
//        Bit 2      2^2        4 = buzzer
//                              5 = red + buzzer
//                              6 = green + buzzer
//                              7 = all

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                /*
                 * Turning On an IO component
                 */
                switch(buttonView.getId()) {

                    case R.id.toggleRedLight:
                        Log.i(TAG, "RED LIGHT on");
                        if (greenOn && buzzerOn) {
                            mBleService.changeIO(mDataCharacteristic, new byte[]{7});
                        } else if (greenOn) {
                            mBleService.changeIO(mDataCharacteristic, new byte[]{3});
                        } else if (buzzerOn) {
                            mBleService.changeIO(mDataCharacteristic, new byte[]{5});
                        } else {
                            mBleService.changeIO(mDataCharacteristic, new byte[]{1});
                        }
                        redOn = true;
                        break;

                    case R.id.toggleGreenLight:
                        Log.i(TAG, "Green light on");
                        if (redOn && buzzerOn) {
                            mBleService.changeIO(mDataCharacteristic, new byte[]{7});
                        } else if (redOn) {
                            mBleService.changeIO(mDataCharacteristic, new byte[]{3});
                        } else if (buzzerOn) {
                            mBleService.changeIO(mDataCharacteristic, new byte[]{6});
                        } else {
                            mBleService.changeIO(mDataCharacteristic, new byte[]{2});
                        }
                        greenOn = true;
                        break;

                    case R.id.toggleBuzzer:
                        Log.i(TAG, "Buzzer on");
                        if (greenOn && redOn) {
                            mBleService.changeIO(mDataCharacteristic, new byte[]{7});
                        } else if (redOn) {
                            mBleService.changeIO(mDataCharacteristic, new byte[]{5});
                        } else if (greenOn) {
                            mBleService.changeIO(mDataCharacteristic, new byte[]{6});
                        } else {
                            mBleService.changeIO(mDataCharacteristic, new byte[]{4});
                        }
                        buzzerOn = true;
                        break;

                    default:
                        Log.e(TAG, "Should not be default");
                        break;
                }
            } else {
                /*
                 * Turning Off an IO component
                 */
                switch(buttonView.getId()) {

                    case R.id.toggleRedLight:
                        Log.i(TAG, "RED LIGHT off");
                        if (greenOn && buzzerOn) {
                            mBleService.changeIO(mDataCharacteristic, new byte[]{6});
                        } else if (greenOn) {
                            mBleService.changeIO(mDataCharacteristic, new byte[]{2});
                        } else if (buzzerOn) {
                            mBleService.changeIO(mDataCharacteristic, new byte[]{4});
                        } else {
                            mBleService.changeIO(mDataCharacteristic, new byte[]{0});
                        }
                        redOn = false;
                        break;

                    case R.id.toggleGreenLight:
                        Log.i(TAG, "Green light off");
                        if (redOn && buzzerOn) {
                            mBleService.changeIO(mDataCharacteristic, new byte[]{5});
                        } else if (redOn) {
                            mBleService.changeIO(mDataCharacteristic, new byte[]{1});
                        } else if (buzzerOn) {
                            mBleService.changeIO(mDataCharacteristic, new byte[]{4});
                        } else {
                            mBleService.changeIO(mDataCharacteristic, new byte[]{0});
                        }
                        greenOn = false;
                        break;

                    case R.id.toggleBuzzer:
                        Log.i(TAG, "Buzzer off");
                        if (greenOn && redOn) {
                            mBleService.changeIO(mDataCharacteristic, new byte[]{3});
                        } else if (redOn) {
                            mBleService.changeIO(mDataCharacteristic, new byte[]{1});
                        } else if (greenOn) {
                            mBleService.changeIO(mDataCharacteristic, new byte[]{2});
                        } else {
                            mBleService.changeIO(mDataCharacteristic, new byte[]{0});
                        }
                        buzzerOn = false;
                        break;

                    default:
                        Log.e(TAG, "Should not be default");
                        break;
                }

            }

        }

    };

}
