package com.ucsf.core.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

import com.ucsf.core.R;
import com.ucsf.core.data.PersistentParameter;

/**
 * Abstract service called on a regular interval. Between two calls, the application may have
 * been shut down, therefore it's not possible to rely on regular variables. To store values, use
 * instead a {@link PersistentParameter}.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public abstract class RecurringService extends BroadcastReceiver {
    private static final String TAG = "ucsf:RecurringService";

    private Context mContext;

    /** Returns the recurring service provider. **/
    public abstract Provider getProvider();

    /** Returns the context in which the service is running. */
    public Context getContext() {
        return mContext;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "");
        wl.acquire();

        mContext = context;

        try {
            execute(context);
        } catch (Exception e) {
            Log.e(TAG, String.format("Failed to execute service '%s': ",
                    getProvider().getServiceName()), e);
        }

        wl.release();
    }

    /** Method called each time the service is executed. */
    protected abstract void execute(Context context) throws Exception;

    /** Abstract recurring service provider classs. */
    public static class Provider extends Services.Provider {
        private final ServiceParameter<Long> mInterval;
        private final Class<? extends RecurringService> mServiceClass;

        public Provider(Context context, Class<? extends RecurringService> serviceClass,
                        ServiceId service, long interval) {
            super(context, service);
            mInterval     = addParameter("INTERVAL", R.string.parameter_interval, interval);
            mServiceClass = serviceClass;
        }

        /** Returns the interval between two service executions. */
        public long getInterval() {
            return mInterval.get();
        }

        @Override
        public boolean isServiceRunning() {
            return PendingIntent.getBroadcast(context, 0, new Intent(context, mServiceClass),
                    PendingIntent.FLAG_NO_CREATE) != null;
        }

        @Override
        public void startService(ResponseListener listener) {
            try {
                // Register the service
                AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                Intent intent = new Intent(context, mServiceClass);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
                am.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis(),
                        getInterval(),
                        pendingIntent);

                // Notify the listener that the service is now running
                listener.onSuccess();
            } catch (Exception e) {
                listener.onFailure("Failed to create a recurring intent: ", e);
            }
        }

        @Override
        public void stopService() {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, mServiceClass);
            PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
            am.cancel(sender);
        }
    }
}
