package com.jordanleex13.sensortag.SensorTag;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

/**
 * Utility functions for the SensorTag
 *
 * @author Jordan Lee
 * @since 16-06-02
 */
public class SensorTagUtil {

    public static boolean isSensorTag2(BluetoothDevice dev) {
        if (dev != null) {
            String name = dev.getName();
            if (name == null) {
                Log.e("Util", "No name for device");
                return false;
            }
            if (name.compareTo("SensorTag2") == 0) return true;
            if (name.compareTo("SensorTag2.0") == 0) return true;
            if (name.compareTo("CC2650 SensorTag") == 0) return true;
            if (name.compareTo("CC2650 SensorTag LED") == 0) return true;
        }
        Log.e("Util", "Device is null");
        return false;
    }
}
