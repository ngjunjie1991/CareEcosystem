package com.ucsf.wear.services;

import android.content.Context;
import android.util.Log;

import com.estimote.sdk.Beacon;
import com.ucsf.core.data.DataManager;
import com.ucsf.core.data.Entry;
import com.ucsf.core.data.RSSI;
import com.ucsf.core.data.SharedTables;
import com.ucsf.core.data.Timestamp;
import com.ucsf.core.services.Annotations;
import com.ucsf.core.services.BackgroundService;
import com.ucsf.core.services.BeaconMonitoring;
import com.ucsf.core.services.ServiceId;
import com.ucsf.core.services.ServiceParameter;
import com.ucsf.wear.R;
import com.ucsf.wear.data.Settings;

import java.util.List;

/**
 * Service responsible of saving beacons RSSI.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public class RangingService extends BackgroundService implements BeaconMonitoring.RangingListener {
    private static final String   TAG                         = "ucsf:RangingService";
    private static final String   KEY_UPDATE_RANGING_INTERVAL = "a";
    private static       Provider mInstance;

    @Override
    public Provider getProvider() {
        return getProvider(this);
    }

    /** Returns the service provider. */
    public static Provider getProvider(Context context) {
        if (mInstance == null)
            mInstance = new Provider(context);
        return mInstance;
    }

    @Override
    public void onBeaconRanging(List<Beacon> beacons) {
        // Get beacons measured power
        RSSI rssi = new RSSI();
        for (Beacon beacon : beacons)
            rssi.put(BeaconMonitoring.getBeaconUniqueId(beacon),
                    BeaconMonitoring.getBeaconRSSI(beacon));

        Log.d(TAG, String.format("RSSI[timestamp: %s]:\n%s", Timestamp.getTimestamp(), rssi.toString()));
        if (rssi.getValues().isEmpty())
            return; // No need to write empty entries

        // Put the beacons RSSI into the database
        try (DataManager instance = DataManager.get(this)) {
            SharedTables.Estimote.getTable(instance).add(
                    new Entry(DataManager.KEY_PATIENT_ID    , Settings.getCurrentUserId(this)),
                    new Entry(DataManager.KEY_TIMESTAMP     , Timestamp.getTimestamp()),
                    new Entry(SharedTables.Estimote.KEY_RSSI, rssi.getValues(), true)
            );
        } catch (Exception e) {
            Log.e(TAG, "Failed to save beacons RSSI: ", e);
        }
    }

    @Override
    protected void onStart() throws Exception {
        Provider provider = getProvider();
        BeaconMonitoring.addRangingListener(this);
        BeaconMonitoring.startMonitoring(this, provider.getWaitPeriod(), provider.getScanPeriod());
    }

    @Override
    protected void onStop() {
        BeaconMonitoring.stopMonitoring();
        BeaconMonitoring.removeRangingListener(this);
    }

    /**
     * Ranging service provider class.
     */
    public static class Provider extends BackgroundService.Provider {
        private final ServiceParameter<Long> mIndoorWaitPeriod;
        private final ServiceParameter<Long> mOutdoorWaitPeriod;
        private final ServiceParameter<Long> mScanPeriod;

        private Provider(Context context) {
            super(context, RangingService.class, ServiceId.PW_RangingService);

            mIndoorWaitPeriod = addParameter("INDOOR_WAIT_PERIOD",
                    R.string.parameter_indoor_wait_period, 10000L);
            mOutdoorWaitPeriod = addParameter("OUTDOOR_WAIT_PERIOD",
                    R.string.parameter_outdoor_wait_period, 300000L);
            mScanPeriod = addParameter("SCAN_PERIOD",
                    R.string.parameter_scan_period, 1000L);
        }

        /**
         * Changes the ranging interval depending on if the patient is at home or not.
         */
        @Annotations.MappedMethod(KEY_UPDATE_RANGING_INTERVAL)
        public void setIndoorMode(boolean enable) {
            if (enable) {
                BeaconMonitoring.resetMonitoringPeriods(mIndoorWaitPeriod.get(),
                        mScanPeriod.get());
            } else {
                BeaconMonitoring.resetMonitoringPeriods(mOutdoorWaitPeriod.get(),
                        mScanPeriod.get());
            }
        }

        /**
         * Returns the period between two consecutive beacons scans.
         */
        public long getWaitPeriod() {
            return mIndoorWaitPeriod.get();
        }

        /**
         * Returns the period during which beacons scanning is done.
         */
        public long getScanPeriod() {
            return mScanPeriod.get();
        }
    }
}
