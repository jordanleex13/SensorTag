package com.jordanleex13.sensortag.models;

/**
 * Model for storing data that comes out of BLE SensorTag
 *
 * See {@code SensorConversion} for usage
 */
public class Point3D {

    public double x, y, z;

    public Point3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

}
