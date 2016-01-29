package com.ucsf.core.services;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;

import com.ucsf.core.R;

/**
 * Service responsible of monitoring events that you may want to notify, such as a low battery.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public abstract class UserMonitoringService extends RecurringService {
    private static final String TAG          = "ucsf:MonitoringService";
    public  static final String KEY_CAPACITY = "CAPACITY";

    @Override
    protected void execute(Context context) throws Exception {
        ((Provider) getProvider()).check();
    }

    /**
     * User monitoring service provider class.
     */
    public static abstract class Provider extends RecurringService.Provider {
        private final ServiceParameter<Float>      mBatteryLevelThreshold;
        private final PersistentParameter<Boolean> mIsBatteryCritical;

        public Provider(Context context, Class<? extends RecurringService> serviceClass,
                        ServiceId service)
        {
            super(context, serviceClass, service, AlarmManager.INTERVAL_HALF_HOUR);

            mIsBatteryCritical     = new PersistentParameter<>("BATTERY_CRITICAL", false);
            mBatteryLevelThreshold = addParameter("BATTERY_THRESH",
                    R.string.parameter_battery_threshold, 20.0f);
        }

        /**
         * Runs all the verifications you need to execute.
         */
        public void check() {
            try {
                checkBattery();
            } catch (Exception e) {
                Log.e(TAG, "Failed to check the battery status: ", e);
            }
        }

        /**
         * Checks the battery status and send the appropriate events.
         */
        private void checkBattery() throws Exception {
            IntentFilter filter        = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent       batteryStatus = context.registerReceiver(null, filter);

            int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL;

            int level   = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale   = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int percent = (100 * level) / scale;

            if (mIsBatteryCritical.get()) {
                if (isCharging || percent > mBatteryLevelThreshold.get()) { // The battery is now okay
                    mIsBatteryCritical.set(false);
                    onBatteryOkayEvent();
                } else // The battery is still critically low
                    onLowBatteryEvent(percent);
            } else {
                if (!isCharging && percent < mBatteryLevelThreshold.get()) { // The battery is not charging and critically low
                    mIsBatteryCritical.set(true);
                    onLowBatteryEvent(percent);
                } else
                    onBatteryOkayEvent();
            }
        }

        /**
         * Notifies that the battery is currently low.
         */
        protected abstract void onLowBatteryEvent(int capacity);

        /**
         * Notifies that the battery is now okay.
         */
        protected abstract void onBatteryOkayEvent();
    }
}
