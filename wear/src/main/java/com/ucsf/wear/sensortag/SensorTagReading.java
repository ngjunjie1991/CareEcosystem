package com.ucsf.wear.sensortag;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by chongwee on 19/2/2016.
 */
public class SensorTagReading {

    private String sensorType;
    private String sensorAddress;
    private Double singleDimensionValue;
    private Point3D threeDimensionValue;

    public SensorTagReading(String type, String address, double value) {
        setSensorType(type);
        setSensorAddress(address);
        setValue(value);
    }

    public SensorTagReading(String type, String address, Point3D value) {
        setSensorType(type);
        setSensorAddress(address);
        setValue(value);
    }

    public void setSensorType(String type) {
        sensorType = type;
    }

    public void setSensorAddress(String address) {
        sensorAddress = address;
    }

    public String getSensorTypeString() {
        return sensorType;
    }

    public String getSensorAddressString() {
        return sensorAddress;
    }

    public void setValue(double value) {
        singleDimensionValue = new Double(value);
    }

    public void setValue(Point3D value) {
        threeDimensionValue = value;
    }

    public List<Double> getValues() {
        ArrayList<Double> values = new ArrayList<Double>();
        if (singleDimensionValue != null) {
            values.add(singleDimensionValue);
        }
        if (threeDimensionValue != null) {
            values.add(new Double(threeDimensionValue.x));
            values.add(new Double(threeDimensionValue.y));
            values.add(new Double(threeDimensionValue.z));
        }
        return values;
    }
}
