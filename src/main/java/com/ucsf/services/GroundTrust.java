package com.ucsf.services;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Range;

import com.ucsf.core.data.DataManager;
import com.ucsf.core.data.Entry;
import com.ucsf.core.data.SharedTables;
import com.ucsf.core.data.Timestamp;
import com.ucsf.core.services.Messages;
import com.ucsf.core.services.ServiceDescriptor;
import com.ucsf.core.services.ServiceId;
import com.ucsf.data.Settings;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Ground trust wrapper for beacons RSSI.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public class GroundTrust {
    private static final String                 TAG               = "ucsf:GroundTrust";
    private static final Map<Listener, Integer> mListeners        = new HashMap<>();
    private static final Set<Listener>          mPendingListeners = new HashSet<>();

    private final Type     mType;
    private       boolean  mIsRunning = false;
    private       String   mStartTimestamp;
    private       String   mLabel;
    private       Listener mListener;
    private       Context  mContext;

    public GroundTrust(Type type) {
        mType = type;
    }

    /**
     * Starts the ground trust service.
     */
    private static synchronized void startService(final Context context, final Listener listener) {
        synchronized (mListeners) {
            if (mListeners.isEmpty()) {
                DeviceInterface.requestServiceStatus(context,
                        ServiceId.PW_RangingService,
                        new com.ucsf.core.services.DeviceInterface.RequestListener() {
                            @Override
                            public void requestProcessed(Messages.Request request, JSONObject data) {
                                ServiceDescriptor.Status status;
                                try {
                                    status = ServiceDescriptor.Status.valueOf(data.getString(Messages.VALUE));
                                } catch (Exception e) {
                                    Log.e(TAG, "Failed to retrieve ranging service status: ", e);
                                    status = ServiceDescriptor.Status.Disabled;
                                }

                                synchronized (mListeners) {
                                    synchronized (mPendingListeners) {
                                        mPendingListeners.add(listener);
                                        for (Listener l : mPendingListeners) {
                                            if (status == ServiceDescriptor.Status.Running) {
                                                Integer counter = mListeners.get(l);
                                                if (counter == null)
                                                    mListeners.put(l, 1);
                                                else
                                                    mListeners.put(l, counter + 1);
                                                l.onMonitoringStarted();

                                            }else{
                                                l.onMonitoringFailedToStart("Ranging service is not running!");
                                            }
                                        }
                                        mPendingListeners.clear();
                                    }
                                }
                            }

                            @Override
                            public void requestTimeout(Messages.Request request) throws Exception {
                                synchronized (mPendingListeners) {
                                    mPendingListeners.add(listener);
                                    for (Listener l : mPendingListeners)
                                        l.onMonitoringFailedToStart("Failed to retrieve ranging service status!");
                                    mPendingListeners.clear();
                                }
                            }

                            @Override
                            public void requestCancelled(Messages.Request request) throws Exception {
                                synchronized (mPendingListeners) {
                                    mPendingListeners.add(listener);
                                }
                            }
                        });
            } else {
                Integer counter = mListeners.get(listener);
                if (counter == null)
                    mListeners.put(listener, 1);
                else
                    mListeners.put(listener, counter + 1);
                listener.onMonitoringStarted();
            }
        }
    }

    /**
     * Stops the ground trust service.
     */
    private static synchronized void stopService(Listener listener) {
        synchronized (mListeners) {
            Integer counter = mListeners.get(listener);
            if (counter != null) {
                if (counter == 1)
                    mListeners.remove(listener);
                else
                    mListeners.put(listener, counter - 1);
            }
        }
    }

    /**
     * Initializes the ground trust service. Must be called before acquiring data.
     */
    public synchronized void init(Context context, @NonNull Listener listener) {
        startService(mContext = context, mListener = listener);
    }

    /**
     * Releases the ground trust service. Will stop running acquisition.
     */
    public synchronized void release() {
        if (isRunning())
            saveEntry(Timestamp.getTimestamp());
        mIsRunning = false;
        stopService(mListener);
    }

    /**
     * Starts ground trust acquisition for the given label.
     */
    public synchronized void startAcquisition(String label) {
        mLabel = label;
        mIsRunning = true;
        if (isRunning())
            mStartTimestamp = Timestamp.getTimestamp();
    }

    /**
     * Stops the recording.
     */
    public synchronized Range<String> stopAcquisition() {
        if (isRunning()) {
            String endTimestamp = Timestamp.getTimestamp();
            saveEntry(endTimestamp);
            mIsRunning = false;
            return new Range<>(mStartTimestamp, endTimestamp);
        }
        mIsRunning = false;
        return null;
    }

    /**
     * Returns if the ground trust service is initialized.
     */
    public boolean isInitialized() {
        return mListeners.containsKey(mListener);
    }


    /**
     * Returns if the ground trust service is running.
     */
    public boolean isRunning() {
        return mIsRunning && isInitialized();
    }

    /**
     * Writes a new entry to the database.
     */
    private void saveEntry(String endTimestamp) {
        try (DataManager instance = DataManager.get(mContext)) {
            SharedTables.GroundTrust.getTable(instance).add(
                    new Entry(DataManager.KEY_PATIENT_ID        , Settings.getCurrentUserId(mContext)),
                    new Entry(SharedTables.GroundTrust.KEY_TYPE , mType.toString()),
                    new Entry(SharedTables.GroundTrust.KEY_LABEL, mLabel),
                    new Entry(SharedTables.GroundTrust.KEY_START, mStartTimestamp),
                    new Entry(SharedTables.GroundTrust.KEY_END  , endTimestamp)
            );
        } catch (Exception e) {
            Log.e(TAG, "Failed to write entry into the database: ", e);
        }
    }

    /**
     * Enumeration of all the possible type of data that we want to test.
     */
    public enum Type {
        /** Patient location. */
        CurrentRoom   ("CR"),

        /** Is the patient wearing the watch. */
        IsWearingWatch("WW"),

        /** Is the patient at home. */
        IsInHouse     ("IH");

        private final String mTag;

        Type(String tag) {
            mTag = tag;
        }

        @Override
        public String toString() {
            return mTag;
        }
    }

    /**
     * Interface use to notify when the ground trust service is started.
     */
    public interface Listener {
        /** Method called when the ground trust is started. */
        void onMonitoringStarted();

        /** Method called if the ground trust failed to start. */
        void onMonitoringFailedToStart(String error);
    }


}
