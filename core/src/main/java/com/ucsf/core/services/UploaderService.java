package com.ucsf.core.services;

import android.content.Context;

import com.ucsf.core.data.DataManager;
import com.ucsf.core.data.DeviceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Abstract service responsible of sending local data to the remote server/phone.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public abstract class UploaderService extends RecurringService {
    private static final String                 TAG              = "ucsf:UploaderService";
    private static final Set<DataManager.Table> mMonitoredTables = new HashSet<>();

    /**
     * Creates and adds a table to monitor, i.e. from which the content will be sent to the remote
     * server/phone.
     */
    public static DataManager.Table addTable(DataManager               instance,
                                             String                    tag,
                                             DeviceLocation            location,
                                             DataManager.TableField... fields)
            throws Exception
    {
        DataManager.Table table = instance.createTable(tag, location, fields);
        mMonitoredTables.add(table);
        return table;
    }

    /**
     * Gets the monitored tables. Because the underlying map is not consistent
     * through different entry points, this method can return only a subset of all the monitored
     * tables. Application startup service should provide a way to load all the monitored tables
     * if needed.
     */
    public static Collection<DataManager.Table> getMonitoredTables() {
        synchronized (mMonitoredTables) { // Creates a copy to avoid concurrent access
            Collection<DataManager.Table> tables = new ArrayList<>();
            tables.addAll(mMonitoredTables);
            return tables;
        }
    }

    @Override
    public void execute(Context context) throws Exception {
        ((Provider) getProvider()).commit();
    }

    /** Uploader service provider class */
    public abstract static class Provider extends RecurringService.Provider {
        private final AtomicBoolean             mIsPushingData = new AtomicBoolean(false);
        private long                            mCurrentCommit;
        private final PersistentParameter<Long> mLastCommit;

        public Provider(Context context, Class<? extends UploaderService> serviceClass,
                        ServiceId service, long interval)
        {
            super(context, serviceClass, service, interval);

            mLastCommit = new PersistentParameter<>("LAST_COMMIT",
                    new com.ucsf.core.data.PersistentParameter.DefaultValue<Long>() {
                        @Override
                        public Long get(Context context) {
                            return System.currentTimeMillis();
                        }
                    });
        }

        /**
         * Returns the last commit time.
         */
        public long getLastCommit() {
            return mLastCommit.get();
        }

        /**
         * Method to call before a commit. Returns the current commit time.
         */
        public long onStartCommit() throws Exception {
            if (mIsPushingData.getAndSet(true))
                throw new Exception("A commit is already running!");
            return mCurrentCommit = System.currentTimeMillis();
        }

        /**
         * Methods to call after a commit in order to finalize it.
         */
        public void onFinishCommit(boolean success) {
            if (success)
                mLastCommit.set(mCurrentCommit);
            mIsPushingData.set(false);
        }

        /**
         * Push the uncommitted data of the monitored tables to the remote device.
         */
        public abstract void commit();
    }

}
