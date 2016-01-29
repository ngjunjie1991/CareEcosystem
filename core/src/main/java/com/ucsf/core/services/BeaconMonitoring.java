package com.ucsf.core.services;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.EstimoteSDK;
import com.estimote.sdk.MacAddress;
import com.estimote.sdk.Region;
import com.ucsf.core.R;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * Class retrieving beacon's information and treating them. Use the Estimote API.
 * Beacons' data are shared through the use of listeners. Such listeners have to be alive as long
 * you want to capture beacons signals. Therefore it's a good idea to use a BackgroundService as
 * a beacon listener.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public class BeaconMonitoring {
    public static final int REQUEST_ENABLE_BT = 1234;

    private static final String               TAG                     = "ucsf:BeaconMonitoring";
    private static final Region               ALL_ESTIMOTE_BEACONS    = new Region("rid", null, null, null);
    private static final Set<RangingListener> mListeners              = new HashSet<>();
    private static       Context              mContext;
    private static       BeaconManager        mBeaconManager          = null;
    private static       boolean              mRanging                = false;
    private static       Timer                mEmulTimer              = null;

    /**
     * Returns the unique id of the given beacon. In this case, we consider the minor to be unique.
     */
    public static String getBeaconUniqueId(Beacon beacon) {
        return String.valueOf(beacon.getMinor());
        /*return String.format("%d-%d-%s", beacon.getMinor(), beacon.getMajor(),
                beacon.getProximityUUID());*/
    }

    /**
     * Returns the beacon RSSI. Use this method to access this value instead of a direct call,
     * because we might use the signal strength instead in the future.
     */
    public static double getBeaconRSSI(Beacon beacon) {
        return beacon.getRssi();
    }

    /**
     * Returns if the current device supports Bluetooth Low Energy.
     */
    private static boolean hasBluetooth() {
        return BluetoothAdapter.getDefaultAdapter() != null;
    }

    /**
     * Returns if bluetooth is currently turned on on the current device.
     */
    public static boolean isBluetoothEnabled() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        return adapter == null || adapter.isEnabled(); // if there is no bluetooth, fake it for the emulator
    }

    /**
     * Asks the user to enabled bluetooth (if needed). To that end, the calling activity has to
     * override onActivityResult(int requestCode, int resultCode, Intent data) to handle the
     * request response. If the user had activated the bluetooth, the monitoring can be started.
     */
    public static void enableBluetooth(Activity activity) {
        if (!isBluetoothEnabled()) {
            // User notification
            Toast.makeText(activity, activity.getString(R.string.alert_bluetooth_required),
                    Toast.LENGTH_LONG).show();

            // Activate bluetooth
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    /**
     * Resets wait and scan periods. The service has to be running.
     */
    public static void resetMonitoringPeriods(long waitPeriod, long scanPeriod) {
        if (mBeaconManager != null)
            mBeaconManager.setForegroundScanPeriod(scanPeriod, waitPeriod);
    }

    /**
     * Starts beacon ranging. Bluetooth needs to be enabled.
     */
    public static synchronized void startMonitoring(Context context,
                                                    long waitPeriod,
                                                    final long scanPeriod) {
        context = context.getApplicationContext();

        if (!hasBluetooth()) { // We need to emulate bluetooth in the emulator
            if (mEmulTimer == null) {
                mEmulTimer = new Timer();
                mEmulTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            List<Beacon> beacons = new LinkedList<>();
                            beacons.add(new Beacon(
                                    UUID.randomUUID(),
                                    MacAddress.fromString("00:00:00:00:00:00"),
                                    0, 0, -42, -42));
                            Thread.sleep(scanPeriod);
                            for (RangingListener listener : mListeners)
                                listener.onBeaconRanging(beacons);
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to emulate beacon ranging: ", e);
                        }
                    }
                }, 0, Math.max(waitPeriod, 2000));
            }
            return;
        }

        if (!isBluetoothEnabled() || mRanging)
            return;

        if (mBeaconManager == null) {
            EstimoteSDK.initialize(context, "careecosystem-1jh", "18716600ef979034927a60bea1e46921");
            EstimoteSDK.enableDebugLogging(true);

            mBeaconManager = new BeaconManager(context);
            mBeaconManager.setRangingListener(new BeaconManager.RangingListener() {
                @Override
                public void onBeaconsDiscovered(Region region, List<Beacon> beacons) {
                    synchronized (mListeners) {
                        for (RangingListener listener : mListeners)
                            listener.onBeaconRanging(beacons);
                    }
                }
            });
            mBeaconManager.setScanStatusListener(new BeaconManager.ScanStatusListener() {
                @Override
                public void onScanStart() {
                    Log.d(TAG, "Device is now scanning for beacons.");
                }

                @Override
                public void onScanStop() {
                    Log.d(TAG, "Device is no longer scanning for beacons.");
                }
            });
        }

        mContext = context;
        mBeaconManager.setForegroundScanPeriod(scanPeriod, waitPeriod);
        mBeaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                try {
                    mBeaconManager.startRanging(ALL_ESTIMOTE_BEACONS);
                    mRanging = true;
                } catch (Exception e) {
                    Log.e(TAG, "Failed to start ranging", e);
                }
            }
        });
    }

    /**
     * Stops beacon ranging.
     */
    public static synchronized void stopMonitoring() {
        if (mEmulTimer != null) {
            mEmulTimer.cancel();
            mEmulTimer = null;
            return;
        }

        if (mBeaconManager == null || !mRanging)
            return;

        try {
            mBeaconManager.stopRanging(ALL_ESTIMOTE_BEACONS);
        } catch (Exception e) {
            Log.e(TAG, "Cannot stop but it does not matter now", e);
        } finally {
            mRanging = false;
            mBeaconManager.disconnect(); // May cause a sdk crash when restarted
            mBeaconManager = null;
        }
    }

    /**
     * Indicates if the monitor is currently ranging.
     */
    public static synchronized boolean isRunning() {
        return (mBeaconManager != null && mRanging) || mEmulTimer != null;
    }

    /**
     * Adds a ranging listener which is called each time a ranging operation is finished.
     */
    public static void addRangingListener(RangingListener listener) {
        synchronized (mListeners) {
            mListeners.add(listener);
        }
    }

    /**
     * Removes a ranging listener.
     */
    public static void removeRangingListener(RangingListener listener) {
        synchronized (mListeners) {
            mListeners.remove(listener);
        }
    }

    /**
     * Beacon ranging interface used to monitor beacons signals. The frequency of advertising can be
     * modified through {@link BeaconMonitoring#startMonitoring(Context, long, long)} or
     * {@link BeaconMonitoring#resetMonitoringPeriods(long, long)} methods.
     */
    public interface RangingListener {
        /**
         * Method called at regular interval.
         * @param beacons List of beacons in range.
         */
        void onBeaconRanging(List<Beacon> beacons);
    }
}
