package com.ucsf.core.services;

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;

import com.ucsf.core.R;
import com.ucsf.core.data.DataManager;
import com.ucsf.core.data.Entry;
import com.ucsf.core.data.Settings;
import com.ucsf.core.data.SharedTables;
import com.ucsf.core.data.Timestamp;

/**
 * Service responsible of getting device sensors data.
 */
public abstract class SensorsService extends FrequentRecurringService {
    private static final SharedTables.Sensors.Data mData = new SharedTables.Sensors.Data();

    @Override
    public void execute(Context context) throws Exception {
        SharedTables.Sensors.Data data = getSensorsData();

        try (DataManager instance = DataManager.get(context)) {
            getSensorsTable(instance).add(
                    new Entry(DataManager.KEY_TIMESTAMP                   , Timestamp.getTimestamp()),
                    new Entry(DataManager.KEY_PATIENT_ID                  , Settings.getCurrentUserId(context)),
                    new Entry(SharedTables.Sensors.KEY_ACC_X              , data.acceleration[0]),
                    new Entry(SharedTables.Sensors.KEY_ACC_Y              , data.acceleration[1]),
                    new Entry(SharedTables.Sensors.KEY_ACC_Z              , data.acceleration[2]),
                    new Entry(SharedTables.Sensors.KEY_AZIMUTH            , data.orientation[0]),
                    new Entry(SharedTables.Sensors.KEY_PITCH              , data.orientation[1]),
                    new Entry(SharedTables.Sensors.KEY_ROLL               , data.orientation[2]),
                    new Entry(SharedTables.Sensors.KEY_HEART_RATE         , data.heartRate),
                    new Entry(SharedTables.Sensors.KEY_IS_HEART_RATE_VALID, data.isHeartRateValid),
                    new Entry(SharedTables.Sensors.KEY_STEP_COUNT         , data.stepCount)
            );
        }
    }

    protected abstract DataManager.Table getSensorsTable(DataManager instance) throws Exception;

    private SharedTables.Sensors.Data getSensorsData() {
        SharedTables.Sensors.Data data = new SharedTables.Sensors.Data();

        synchronized (mData) {
            data.acceleration     = mData.acceleration.clone();
            data.orientation      = mData.orientation.clone();
            data.heartRate        = mData.heartRate;
            data.isHeartRateValid = mData.isHeartRateValid;
            data.stepCount        = mData.stepCount;
        }

        return data;
    }

    public static abstract class Provider extends FrequentRecurringService.Provider {
        public static final String ACCELEROMETER_TAG = "IS_ACCELEROMETER_ENABLED";
        public static final String GYROSCOPE_TAG     = "IS_GYROSCOPE_ENABLED";

        private final ServiceParameter<Boolean> mIsAccelerometerEnabled;
        private final ServiceParameter<Boolean> mIsGyroscopeEnabled;
        private final ServiceParameter<Boolean> mIsHeartMonitorEnabled;
        private final ServiceParameter<Boolean> mIsStepCounterEnabled;
        private final SensorEventListener mListener;
        private final Sensor[] mSensors      = new Sensor[android.os.Build.VERSION.SDK_INT >= 20 ? 4 : 3];
        private final float[]  mR            = new float[9];
        private int            mTotStepCount = 0;
        private SensorManager  mManager;

        public Provider(Context context, Class<? extends SensorsService> serviceClass,
                        ServiceId service, long interval) {
            super(context, serviceClass, service, interval);

            mIsAccelerometerEnabled = addParameter(ACCELEROMETER_TAG,
                    R.string.parameter_sensors_accelerometer, true);
            mIsGyroscopeEnabled     = addParameter(GYROSCOPE_TAG,
                    R.string.parameter_sensors_gyroscope, true);
            mIsStepCounterEnabled   = addParameter("IS_STEP_COUNTER_ENABLED",
                    R.string.parameter_sensors_steps, true);
            if (android.os.Build.VERSION.SDK_INT >= 20)
                mIsHeartMonitorEnabled = addParameter("IS_HEART_MONITOR_ENABLED",
                        R.string.parameter_sensors_heart, false);
            else
                mIsHeartMonitorEnabled = null;

            mListener = new SensorEventListener() {
                @Override
                public void onSensorChanged(SensorEvent event) {
                    synchronized (mData) {
                        switch (event.sensor.getType()) {
                            case Sensor.TYPE_ACCELEROMETER:
                                mData.acceleration = event.values;
                                break;
                            case Sensor.TYPE_ROTATION_VECTOR:
                                SensorManager.getRotationMatrixFromVector(mR, event.values);
                                SensorManager.getOrientation(mR, mData.orientation);
                                break;
                            case Sensor.TYPE_HEART_RATE:
                                mData.heartRate = event.values[0];
                                break;
                            case Sensor.TYPE_STEP_COUNTER:
                                mData.stepCount = (int) event.values[0] - mTotStepCount;
                                mTotStepCount   = (int) event.values[0];
                                break;
                        }
                    }
                }

                @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {
                    if (sensor.getType() == Sensor.TYPE_HEART_RATE) {
                        synchronized (mData) {
                            mData.isHeartRateValid =
                                    accuracy != SensorManager.SENSOR_STATUS_UNRELIABLE &&
                                            accuracy != SensorManager.SENSOR_STATUS_NO_CONTACT;
                        }
                    }
                }
            };
        }

        @Override
        public void startService(ResponseListener listener) {
            try {
                mManager =
                        (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

                mSensors[0] = resetSensor(mIsAccelerometerEnabled.get(),
                        Sensor.TYPE_ACCELEROMETER);
                mSensors[1] = resetSensor(mIsGyroscopeEnabled.get(),
                        Sensor.TYPE_ROTATION_VECTOR);
                mSensors[2] = resetSensor(mIsStepCounterEnabled.get(),
                        Sensor.TYPE_STEP_COUNTER);
                if (android.os.Build.VERSION.SDK_INT >= 20) {
                    mSensors[3] = resetSensor(mIsHeartMonitorEnabled.get(),
                            Sensor.TYPE_HEART_RATE);

                    if (!(mIsHeartMonitorEnabled.get()))
                        mData.isHeartRateValid = false; // Make sure to invalidate heart rate monitoring
                } else
                    mData.isHeartRateValid = false; // The sensor is not available

                for (Sensor sensor : mSensors) {
                    if (sensor != null)
                        mManager.registerListener(mListener, sensor, (int) getInterval() * 100,
                                (int) getInterval() * 1000); // Only one update per interval to preserve battery life
                }

                super.startService(listener);
            } catch (Exception e) {
                listener.onFailure("Failed to initialize sensors: ", e);
            }
        }

        @Override
        public void stopService() {
            mManager.unregisterListener(mListener);
            super.stopService();
        }

        @Override
        public boolean isServiceRunning() {
            return super.isServiceRunning() && mManager != null;
        }

        /**
         * Returns the sensor corresponding to the given type if enabled or null otherwise.
         */
        private Sensor resetSensor(boolean isEnabled, int sensorType) {
            if (isEnabled)
                return mManager.getDefaultSensor(sensorType);
            /*
             * No need to unregister previously registered sensor since the service starts only if
             * it's not running, i.e. if the method stopService() was called.
             */

            return null;
        }
    }
}
