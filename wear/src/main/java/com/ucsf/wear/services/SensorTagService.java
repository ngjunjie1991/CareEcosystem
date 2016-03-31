package com.ucsf.wear.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.ucsf.core.data.DataManager;
import com.ucsf.core.data.Entry;
import com.ucsf.core.data.SharedTables;
import com.ucsf.core.data.Timestamp;
import com.ucsf.core.services.Annotations;
import com.ucsf.core.services.BackgroundService;
import com.ucsf.core.services.ServiceId;
import com.ucsf.core.services.ServiceParameter;
import com.ucsf.wear.R;
import com.ucsf.wear.data.Settings;
import com.ucsf.wear.sensortag.SensorTagReading;

import java.util.List;

/**
 * Service responsible of saving SensorTag readings.
 *
 * @author  Chong Wee Tan
 * @version 1.0
 */
public class SensorTagService extends BackgroundService implements SensorTagMonitoring.SensorTagListener {
    private static final String   TAG                         = "ucsf:SensorTagService";
    private static final String   KEY_UPDATE_RANGING_INTERVAL = "a";
    private static       Provider mInstance;
    private static       SensorTagMonitoring mSensorTagMonitor;

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
    public void onSensorTagReading(List<SensorTagReading> readings) {
        // Get sensor readings
        for (SensorTagReading reading : readings) {
            String sensorType = reading.getSensorTypeString();
            String sensorAddress = reading.getSensorAddressString();
            List<Double> values = reading.getValues();
            double reading0 = 0, reading1 = 0, reading2 = 0;
            if(values.size() == 1)
                reading0 = values.get(0);
            else if(values.size() == 3)
            {
                reading0 = values.get(0);
                reading1 = values.get(1);
                reading2 = values.get(2);
            }

            //TODO: Uncomment this for debugging
            //Log.d(TAG,sensorAddress + "\t" + sensorType + "\t" + reading0 + "\t" + reading1 + "\t" + reading2);

            //Put the sensor readings into the database
            try (DataManager instance = DataManager.get(this)) {
                SharedTables.SensorTag.getTable(instance).add(
                        new Entry(DataManager.KEY_PATIENT_ID    , Settings.getCurrentUserId(this)),
                        new Entry(DataManager.KEY_TIMESTAMP     , Timestamp.getTimestamp()),
                        new Entry(SharedTables.SensorTag.KEY_SENSORTAG_ID, sensorAddress),
                        new Entry(SharedTables.SensorTag.KEY_TYPE, sensorType),
                        new Entry(SharedTables.SensorTag.KEY_READING_ALL, reading0),
                        new Entry(SharedTables.SensorTag.KEY_READING_X, reading0),
                        new Entry(SharedTables.SensorTag.KEY_READING_Y, reading1),
                        new Entry(SharedTables.SensorTag.KEY_READING_Z, reading2)
                );
            } catch (Exception e) {
                Log.e(TAG, "Failed to get sensortag reading: ", e);
            }
        }

    }


    @Override
    protected void onStart() throws Exception {
        Log.d(TAG, "onStart()");
        Provider provider = getProvider();
        mSensorTagMonitor = new SensorTagMonitoring();
        mSensorTagMonitor.addSensorTagListener(this);
        mSensorTagMonitor.startMonitoring(this);

        //display foreground notification
        Notification.Builder builder = new Notification.Builder(this.getApplicationContext());

        Intent intent = new Intent(this.getApplicationContext(), SensorTagMonitoring.class);
        PendingIntent pendingIntent
                = PendingIntent.getActivity(this.getApplicationContext(), 0, intent, 0);

        Notification notification = builder
                .setSmallIcon(com.ucsf.core.R.drawable.icon)
                .setContentTitle("SensorTagTI Data Capture")
                .setContentText("Automatic Mode Enabled")
                .setContentInfo("ContentInfo")
                .setAutoCancel(true)
                .build();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        prefs.edit().putBoolean("automaticModeEnabled", true).commit();

        startForeground(54330216, notification);

        ////////////////////////////////////////////////////
        getSensortagConfig();
    }

    @Override
    protected void onStop() {
        mSensorTagMonitor.stopMonitoring();
        mSensorTagMonitor.removeSensorTagListener(this);

        //remove foreground notification
        stopForeground(true);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        prefs.edit().putBoolean("automaticModeEnabled", false).commit();

    }

    public void getSensortagConfig() {
        DeviceInterface.requestSensortagInfo(this.getApplicationContext());
        //What else do you want to do???
    }

    /**
     * Ranging service provider class.
     */
    public static class Provider extends BackgroundService.Provider {

        private Provider(Context context) {
            super(context, SensorTagService.class, ServiceId.PW_SensorTagService);
        }

        /**
         * Changes the sensortag scan interval depending on if the patient is at home or not.
         */
        @Annotations.MappedMethod(KEY_UPDATE_RANGING_INTERVAL)
        public void setIndoorMode(boolean enable) {

            if (enable) {
                mSensorTagMonitor.startMonitoring(super.context);
            } else {
                mSensorTagMonitor.stopMonitoring();
            }

        }
    }
}
