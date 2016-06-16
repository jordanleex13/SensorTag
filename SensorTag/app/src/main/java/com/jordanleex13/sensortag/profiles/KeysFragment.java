package com.jordanleex13.sensortag.profiles;


import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.jordanleex13.sensortag.BleService;
import com.jordanleex13.sensortag.R;
import com.jordanleex13.sensortag.SensorTag.IntentNames;
import com.jordanleex13.sensortag.SensorTag.SensorTagGatt;

/**
 * A simple {@link Fragment} subclass that displays key presses.
 */
public class KeysFragment extends Fragment {

    private static final String TAG = KeysFragment.class.getSimpleName();
    private static final String FRAGMENT_POSITION = "com.jordanleex13.sensortag.KeysFragment.FRAGMENT_POSITION";

    /**
     * UI related variables
     */
    private int sectionNumber;
    private TextView positionText;
    private ImageView leftKeyImage;
    private ImageView rightKeyImage;

    /**
     * BLE related variables
     */
    private BleService mBleService;
    private BluetoothGattService mThis;


    public KeysFragment() {
        // Required empty public constructor
    }

    /**
     * Instantiates a new fragment. Puts fragment number into a bundle that will be retrieved in {@code onCreate}
     *
     * @param position  The fragment number
     * @return          KeysFragment
     */
    public static KeysFragment newInstance(int position) {
        KeysFragment fragment = new KeysFragment();
        Bundle args = new Bundle();
        args.putInt(FRAGMENT_POSITION, position);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Retrieves bundle and instantiates BLE service and key service
     *
     * @param savedInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            sectionNumber = getArguments().getInt(FRAGMENT_POSITION);
        }
        mBleService = BleService.getInstance();
        mThis = mBleService.getServiceFromUUID(SensorTagGatt.UUID_KEY_SERV.toString());
    }

    /**
     * Sets up various UI elements
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
        View v = inflater.inflate(R.layout.fragment_keys, container, false);

        positionText = (TextView) v.findViewById(R.id.fragment_position);
        positionText.setText("Fragment " + String.valueOf(sectionNumber) + ": Keys Service");

        leftKeyImage = (ImageView) v.findViewById(R.id.leftKeyImage);
        leftKeyImage.setImageResource(R.drawable.leftkeyoff_300);

        rightKeyImage = (ImageView) v.findViewById(R.id.rightKeyImage);
        rightKeyImage.setImageResource(R.drawable.rightkeyoff_300);

        return v;
    }


    /**
     * Registers the broadcast receiver
     */
    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(keysUpdateReceiver, makeKeysUpdateIntentFilter());
        Log.i(TAG, "Registering KEYS receiver");
    }

    /**
     * Unregisters the broadcast receiver
     */
    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(keysUpdateReceiver);
        Log.i(TAG, "Unregistering KEYS receiver");
    }


    /**
     * Receives updates from {@code mGattUpdateReceiver#ACTION_DATA_NOTIFY} and display images corresponding
     * to updated data.
     */
    private final BroadcastReceiver keysUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

//        Bit 0      2^0        1 = left
//        Bit 1      2^1        2 = right
//                              3 = left + right

            final String action = intent.getAction();

            if (IntentNames.ACTION_KEY_CHANGE.equals(action)) {

                byte[] keyValue = mBleService.getCharacteristicFromUUID(SensorTagGatt.UUID_KEY_DATA.toString()).getValue();
                switch (keyValue[0]) {
                    /*
                     * Taken from TISensorTag source code: reads the byte and does appropriate image swapping
                     */
                    case 0x1:
                        leftKeyImage.setImageResource(R.drawable.leftkeyon_300);
                        rightKeyImage.setImageResource(R.drawable.rightkeyoff_300);
                        break;
                    case 0x2:
                        leftKeyImage.setImageResource(R.drawable.leftkeyoff_300);
                        rightKeyImage.setImageResource(R.drawable.rightkeyon_300);
                        break;
                    case 0x3:
                        leftKeyImage.setImageResource(R.drawable.leftkeyon_300);
                        rightKeyImage.setImageResource(R.drawable.rightkeyon_300);
                        break;
                    default:
                        leftKeyImage.setImageResource(R.drawable.leftkeyoff_300);
                        rightKeyImage.setImageResource(R.drawable.rightkeyoff_300);
                        break;
                }
            }
        }
    };

    private static IntentFilter makeKeysUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(IntentNames.ACTION_KEY_CHANGE);
        return intentFilter;
    }

}
