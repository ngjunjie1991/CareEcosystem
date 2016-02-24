package com.ucsf.wear.sensortag;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by chongwee on 19/2/2016.
 */
public class SensorTagReading {

    private String sensorType;
    private Double singleDimensionValue;
    private Point3D threeDimensionValue;

    public SensorTagReading(String type, double value) {
        setSensorType(type);
        setValue(value);
    }

    public SensorTagReading(String type, Point3D value) {
        setSensorType(type);
        setValue(value);
    }

    public void setSensorType(String type) {
        sensorType = type;
    }

    public String getSensorTypeString() {
        return sensorType;
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
