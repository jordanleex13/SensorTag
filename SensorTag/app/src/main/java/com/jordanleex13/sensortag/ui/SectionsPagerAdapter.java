package com.jordanleex13.sensortag.ui;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.Log;

import com.jordanleex13.sensortag.profiles.BarometerFragment;
import com.jordanleex13.sensortag.profiles.HumidityFragment;
import com.jordanleex13.sensortag.profiles.IOFragment;
import com.jordanleex13.sensortag.profiles.KeysFragment;
import com.jordanleex13.sensortag.profiles.MotionFragment;
import com.jordanleex13.sensortag.profiles.OpticalFragment;
import com.jordanleex13.sensortag.profiles.TemperatureFragment;

/**
 * Displays data fragments for each service in the SensorTag
 *
 * @author Jordan Lee
 * @since 16-06-07
 */
public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

    private static final String TAG = SectionsPagerAdapter.class.getSimpleName();

    public SectionsPagerAdapter(FragmentManager fm) {
        super(fm);
        Log.d(TAG, "Constructor for adapter");
    }

    /**
     * Called to instantiate the fragment for the given page
     *
     * @param position position of the fragment
     * @return new fragment
     */
    @Override
    public Fragment getItem(int position) {
        Log.d(TAG, "Get item: " + position);

        switch (position) {

            case 0:
                return MotionFragment.newInstance(position);
            case 1:
                return OpticalFragment.newInstance(position);
            case 2:
                return TemperatureFragment.newInstance(position);
            case 3:
                return KeysFragment.newInstance(position);
            case 4:
                return IOFragment.newInstance(position);
            case 5:
                return BarometerFragment.newInstance(position);
            case 6:
                return HumidityFragment.newInstance(position);


            default:
                return null;
        }

    }


    @Override
    public int getCount() {
        return 7;
    }

}
