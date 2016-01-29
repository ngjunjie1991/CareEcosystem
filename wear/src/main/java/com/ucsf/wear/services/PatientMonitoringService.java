package com.ucsf.wear.services;

import android.app.AlarmManager;
import android.content.Context;
import android.util.Log;

import com.ucsf.core.data.DataManager;
import com.ucsf.core.data.Entry;
import com.ucsf.core.data.SharedTables;
import com.ucsf.core.data.Timestamp;
import com.ucsf.core.services.Messages;
import com.ucsf.core.services.ServiceId;
import com.ucsf.core.services.ServiceParameter;
import com.ucsf.core.services.UserMonitoringService;
import com.ucsf.wear.R;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Watch implementation of the {@link UserMonitoringService monitoring service}. Check also if
 * the patient is wearing the watch.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public class PatientMonitoringService extends UserMonitoringService {
    private static final String   TAG                = "ucsf:PatientWatcher";
    private static final double   ACCELERATION_SCALE = 1.0 / 10;
    private static final double   ORIENTATION_SCALE  = 1.0 / Math.PI;
    private static final double   THRESHOLD          = 0.5;
    private static final long     MORNING_RANGE      = AlarmManager.INTERVAL_HALF_HOUR;
    private static       Provider mInstance;

    /**
     * Returns the monitoring service provider.
     */
    public static Provider getProvider(Context context) {
        if (mInstance == null)
            mInstance = new Provider(context);
        return mInstance;
    }

    @Override
    public Provider getProvider() {
        return getProvider(getContext());
    }

    /**
     * Monitoring service provider class.
     */
    public static class Provider extends UserMonitoringService.Provider {
        private final ServiceParameter<Long>       mStartParameter;
        private final ServiceParameter<Long>       mEndParameter;
        private final PersistentParameter<Boolean> mIsPatientWearingWatch;

        public Provider(Context context) {
            super(context, PatientMonitoringService.class, ServiceId.PW_PatientWatcherService);

            mStartParameter = addParameter("START_DAY", R.string.parameter_start_day,
                    8 * AlarmManager.INTERVAL_HOUR);
            mEndParameter   = addParameter("END_DAY"  , R.string.parameter_end_day,
                    21 * AlarmManager.INTERVAL_HOUR);

            mIsPatientWearingWatch = new PersistentParameter<>("PATIENT_WEARING_WATCH", true);
        }

        /**
         * Returns the calendar time corresponding to the given parameter.
         */
        private Calendar getCalendarTime(ServiceParameter<Long> parameter) {
            return getCalendarTime(parameter, 0L);
        }

        /**
         * Returns the calendar time corresponding to the given parameter with the given offset (in
         * milliseconds).
         */
        private Calendar getCalendarTime(ServiceParameter<Long> parameter, long offset) {
            int time = (int) (parameter.get() + offset);

            Calendar calendar = GregorianCalendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, time / 3600000);
            calendar.set(Calendar.MINUTE, (time / 60000) % 60);
            calendar.set(Calendar.SECOND, (time / 1000) % 60);

            return calendar;
        }

        @Override
        public void check() {
            super.check();
            checkPatientWearingWatch();
        }

        /**
         * Checks if the patient is wearing the watch
         */
        private void checkPatientWearingWatch() {
            // Only perform this check during day period
            Calendar currentTime = GregorianCalendar.getInstance();

            if (currentTime.compareTo(getCalendarTime(mEndParameter)) > 0 ||
                    currentTime.compareTo(getCalendarTime(mStartParameter)) < 0)
                return;

            // Get the last period acceleration and orientation data
            String currentTimestamp  = Timestamp.getTimestamp();
            String previousTimestamp = Timestamp.getTimestamp(-getInterval());

            try (DataManager instance = DataManager.get(context)) {
                DataManager.Cursor cursor = SharedTables.Sensors.getTable(instance).fetch(
                        new String[]{
                                SharedTables.Sensors.KEY_ACC_X,
                                SharedTables.Sensors.KEY_ACC_Y,
                                SharedTables.Sensors.KEY_ACC_Z,
                                SharedTables.Sensors.KEY_AZIMUTH,
                                SharedTables.Sensors.KEY_PITCH,
                                SharedTables.Sensors.KEY_ROLL
                        },
                        new DataManager.Condition.LessEqual<>(DataManager.KEY_TIMESTAMP,
                                currentTimestamp),
                        new DataManager.Condition.GreaterEqual<>(DataManager.KEY_TIMESTAMP,
                                previousTimestamp)
                );

                if (cursor != null && cursor.moveToFirst()) {
                    int count = 0;
                    double ax = 0.0, ax2 = 0.0;
                    double ay = 0.0, ay2 = 0.0;
                    double az = 0.0, az2 = 0.0;
                    double ox = 0.0, ox2 = 0.0;
                    double oy = 0.0, oy2 = 0.0;
                    double oz = 0.0, oz2 = 0.0;

                    do {
                        double accX    = cursor.getDouble(0);
                        double accY    = cursor.getDouble(1);
                        double accZ    = cursor.getDouble(2);
                        double azimuth = cursor.getDouble(3);
                        double pitch   = cursor.getDouble(4);
                        double roll    = cursor.getDouble(5);

                        ax  += accX;
                        ax2 += accX * accX;
                        ay  += accY;
                        ay2 += accY * accY;
                        az  += accZ;
                        az2 += accZ * accZ;

                        ox  += azimuth;
                        ox2 += azimuth * azimuth;
                        oy  += pitch;
                        oy2 += pitch * pitch;
                        oz  += roll;
                        oz2 += roll * roll;

                        ++count;
                    } while (cursor.moveToNext());

                    ax /= count; ay /= count; az /= count;
                    ox /= count; oy /= count; oz /= count;
                    
                    ax2 = ax2 / count - ax * ax; ay2 = ay2 / count - ay * ay; az2 = az2 / count - az * az;
                    ox2 = ox2 / count - ox * ox; oy2 = oy2 / count - oy * oy; oz2 = oz2 / count - oz * oz;
                    
                    double av = (ax2 + ay2 + az2) / 3;
                    double ov = (ox2 + oy2 + oz2) / 3;
                    if ((ACCELERATION_SCALE * av + ORIENTATION_SCALE * ov) > THRESHOLD) {
                        // The watch has moved, so the patient is probably wearing it
                        mIsPatientWearingWatch.set(true);
                        DeviceInterface.sendEvent(context, Messages.Event.WATCH_OKAY);
                        return;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to retrieve acceleration data: ", e);
            }

            // Notify the server. The event sent depend on the time of the day
            Calendar morningEndTime = getCalendarTime(mStartParameter, MORNING_RANGE);

            mIsPatientWearingWatch.set(false);

            if (Timestamp.getCalendarFromTimestamp(currentTimestamp).compareTo(morningEndTime) < 0)
                DeviceInterface.sendEvent(context, Messages.Event.NO_WATCH_ON_MORNING);
            else
                DeviceInterface.sendEvent(context, Messages.Event.NO_WATCH);
        }

        @Override
        protected void onLowBatteryEvent(int capacity) {
            DeviceInterface.sendEvent(context, Messages.Event.LOW_BATTERY,
                    new Entry(KEY_CAPACITY, capacity));
        }

        @Override
        protected void onBatteryOkayEvent() {
            DeviceInterface.sendEvent(context, Messages.Event.BATTERY_OKAY);
        }
    }
}
