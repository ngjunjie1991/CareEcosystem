package com.ucsf.core.services;

import android.app.AlarmManager;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.ucsf.core.R;
import com.ucsf.core.data.DataManager;
import com.ucsf.core.data.DataManager.Condition;
import com.ucsf.core.data.SharedTables;
import com.ucsf.core.data.Timestamp;

/**
 * Service responsible of cleaning the database. Entries older than one week and that are committed
 * are deleted. Avoids to use to much memory on the phone.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public abstract class CleanupService extends RecurringService {
    private static final String TAG = "ucsf:CleanupService";
    private static final String KEY_CLEANUP = "a";

    /** Implementation of the recurring service provider for the cleanup service. */
    public static abstract class Provider extends RecurringService.Provider {
        private final ServiceParameter<Long> mCleanupPeriod;

        protected Provider(Context context, Class<? extends CleanupService> serviceClass,
                           ServiceId service) {
            super(context, serviceClass, service, AlarmManager.INTERVAL_DAY);

            mCleanupPeriod = addParameter("CLEANUP_PERIOD", R.string.parameter_cleanup_period,
                    604800000L); // One week

            addCallback("CLEANUP_ACTION", R.string.parameter_cleanup_callback, KEY_CLEANUP);
        }

        /**
         * Returns the interval of time after which one data will be deleted. The default value is
         * one week.
         */
        public long getCleanupTime() {
            return mCleanupPeriod.get();
        }

        /**
         * Erases committed entries older than {@link Provider#getCleanupTime()}.
         */
        public void cleanData() throws Exception {
            String    timestamp  = Timestamp.getTimestamp(-getCleanupTime());
            Condition timeCond   = new Condition.Less<>(DataManager.KEY_TIMESTAMP, timestamp);
            Condition commitCond = new Condition.Equal<>(DataManager.KEY_IS_COMMITTED, 1);

            try (DataManager instance = DataManager.get(context)) {
                for (DataManager.Table table : UploaderService.getMonitoredTables()) {
                    if (table == SharedTables.GroundTrust.getTable(instance))
                        table.erase(commitCond,
                                new Condition.LessEqual<>(SharedTables.GroundTrust.KEY_END, timestamp));
                    else
                        table.erase(timeCond, commitCond);
                }
            } catch (Exception e) {
                throw e;
            }
        }

        /**
         * External access to the method {@link Provider#cleanData()}.
         */
        @Annotations.MappedMethod(KEY_CLEANUP)
        public void cleanup() {
            Toast.makeText(context, R.string.toast_cleanup, Toast.LENGTH_SHORT).show();
            try {
                cleanData();
            } catch (Exception e) {
                Log.e(TAG, "Failed to clean data: ", e);
            }
        }
    }
}
