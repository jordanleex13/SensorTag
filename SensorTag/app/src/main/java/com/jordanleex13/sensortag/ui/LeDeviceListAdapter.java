package com.jordanleex13.sensortag.ui;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.jordanleex13.sensortag.R;

import java.util.ArrayList;

/**
 * Displays the list of available BLE devices
 *
 * @author Jordan Lee
 * @since 16-05-31
 */
public class LeDeviceListAdapter extends BaseAdapter {

    /**
     * Data lists
     */
    private ArrayList<BluetoothDevice> mLeDevices;
    private ArrayList<Integer> rssiValues;

    private LayoutInflater mInflator;

    public LeDeviceListAdapter(Context context) {
        super();
        mLeDevices = new ArrayList<BluetoothDevice>();
        rssiValues = new ArrayList<>();
        mInflator = LayoutInflater.from(context);
    }

    /**
     * Adds device to {@code mLeDevices} if not already in list.
     * @param device Device to be added to list
     */
    public void addDevice(BluetoothDevice device) {
        if(!mLeDevices.contains(device)) {
            mLeDevices.add(device);
        }
    }

    /**
     * If the device is in {@code mLeDevices}, the rssi value will be updated. The structure is set up
     * such that the index of the device and the RSSI value are the same.
     *
     * @param device    The device in question
     * @param RSSI      The RSSI value of said device
     */
    public void updateRssiValue(BluetoothDevice device, int RSSI) {
        if (mLeDevices.contains(device)) {
            rssiValues.add(mLeDevices.indexOf(device), RSSI);
        } else {
            Log.w("updateRssiValue", "Didn't add RSSI");
        }
    }

    public BluetoothDevice getDevice(int position) {
        return mLeDevices.get(position);
    }
    public void clear() {
        mLeDevices.clear();
    }
    @Override
    public int getCount() {
        return mLeDevices.size();
    }
    @Override
    public Object getItem(int i) {
        return mLeDevices.get(i);
    }
    @Override
    public long getItemId(int i) {
        return i;
    }


    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder viewHolder;

        // General ListView optimization code.
        if (view == null) {
            view = mInflator.inflate(R.layout.listitem_device, null);
            viewHolder = new ViewHolder();
            viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
            viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
            viewHolder.deviceRSSI = (TextView) view.findViewById(R.id.device_rssi);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }


        BluetoothDevice device = mLeDevices.get(i);
        int rssiValue = rssiValues.get(i);

        final String deviceName = device.getName();
        if (deviceName != null && deviceName.length() > 0)
            viewHolder.deviceName.setText(deviceName);
        else
            viewHolder.deviceName.setText("Unknown device");

        viewHolder.deviceAddress.setText(device.getAddress());
        viewHolder.deviceRSSI.setText("RSSI: " + String.valueOf(rssiValue));

        return view;
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        TextView deviceRSSI;
    }

}
