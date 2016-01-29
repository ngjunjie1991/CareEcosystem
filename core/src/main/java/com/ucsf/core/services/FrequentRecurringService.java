package com.ucsf.core.services;

import android.content.Context;
import android.os.PowerManager;
import android.util.Log;

import com.ucsf.core.R;
import com.ucsf.core.data.PersistentParameter;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Abstract service called on a regular interval. Between two calls, the application may have
 * been shut down, therefore it's not possible to rely on regular variables. To store values, use
 * instead a {@link PersistentParameter}.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public abstract class FrequentRecurringService extends BackgroundService implements Runnable {
    private static final String TAG = "ucsf:RecurringService";

    private final ScheduledExecutorService mServiceExecutor;
    private       ScheduledFuture          mServiceHandler;

    public FrequentRecurringService() {
        mServiceExecutor = Executors.newScheduledThreadPool(1);
    }

    @Override
    protected void onStart() throws Exception {
        Provider provider = (Provider) getProvider();
        mServiceHandler   = mServiceExecutor.scheduleAtFixedRate(this, 0, provider.getInterval(),
                TimeUnit.MILLISECONDS);
    }

    @Override
    protected void onStop() {
        if (mServiceHandler != null)
            mServiceHandler.cancel(false);
    }

    public void run() {
        PowerManager          pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "");
        wl.acquire();

        try {
            execute(this);
        } catch (Exception e) {
            Log.e(TAG, String.format("Failed to execute service '%s': ",
                    getProvider().getServiceName()), e);
        }

        wl.release();
    }

    /** Method called each time the service is executed. */
    protected abstract void execute(Context context) throws Exception;

    /** Abstract recurring service provider classs. */
    public static class Provider extends BackgroundService.Provider {
        private final ServiceParameter<Long> mInterval;

        protected Provider(Context context, Class<? extends FrequentRecurringService> serviceClass,
                           ServiceId service, long interval)
        {
            super(context, serviceClass, service);
            mInterval =  addParameter("INTERVAL", R.string.parameter_interval, interval);
        }

        /** Returns the interval between two service executions. */
        public long getInterval() {
            return mInterval.get();
        }

    }
}
