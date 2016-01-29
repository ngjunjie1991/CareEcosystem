package com.ucsf.core.data;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Class storing RSSI values.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public class RSSI {
    public static final double DEFAULT_RSSI = -999; /**< Default RSSI values when the emitter is out of range. */

    private final HashMap<String, Double> mRSSI = new HashMap<>();

    public RSSI() {}

    public RSSI(Map<String, Double> values) {
        mRSSI.putAll(values);
    }

    /**
     * Inserts the given RSSI value for the given {@link Mote mote}.
     */
    public void put(Mote mote, double power) {
        put(mote.getMoteId(), power);
    }

    /**
     * Inserts the given RSSI value for the {@link Mote mote} identified by the given id.
     */
    public void put(String id, double power) {
        mRSSI.put(id, power);
    }

    /**
     * Returns the RSSI value for the given {@link Mote mote}.
     */
    public double get(Mote mote) {
        return get(mote.getMoteId());
    }

    /**
     * Returns the RSSI value for the {@link Mote mote} identified by the given id.
     */
    private double get(String id) {
        Double power = mRSSI.get(id);
        if (power == null)
            return DEFAULT_RSSI;
        return power;
    }

    /**
     * Returns all the pairs mote id/RSSI value contained in this object.
     */
    public HashMap<String, Double> getValues() {
        return mRSSI;
    }

    @Override
    public String toString() {
        StringBuilder ss = new StringBuilder();
        ss.append("[");

        Iterator<Map.Entry<String, Double>> it = mRSSI.entrySet().iterator();
        if (it.hasNext()) {
            Map.Entry<String, Double> entry = it.next();
            ss.append(entry.getKey()).append(": ").append(entry.getValue());
        }

        while (it.hasNext()) {
            Map.Entry<String, Double> entry = it.next();
            ss.append("\n ").append(entry.getKey()).append(": ").append(entry.getValue());
        }

        ss.append("]");
        return ss.toString();
    }

}
