package com.ucsf.core.services;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

/**
 * Abstract definition of a service running in the background. Inherits from {@link Service}.
 * Such services continue to run even if the application is closed by the user.
 * If you need a service running in the background, but doing some processing at regular intervals,
 * use {@link FrequentRecurringService} instead.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public abstract class BackgroundService extends Service {
    private static final String TAG = "ucsf:BackgroundService";

    /** Class giving access to the background service when
     * {@link Context#bindService(Intent, ServiceConnection, int)}
     */
    public class Binder extends android.os.Binder {
        public BackgroundService getService() {
            return BackgroundService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            onStart();
            getProvider().mServiceInstance = this;
        } catch (Exception e) {
            Log.e(TAG, String.format("Failed to start background service '%s': ",
                    getProvider().getServiceName()), e);
            getProvider().killService();
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new Binder();
    }

    /** Returns the service provider. */
    public abstract Provider getProvider();

    /** Method called when the service is started. */
    protected abstract void onStart() throws Exception;

    /** Method called when the service is stoped. */
    protected abstract void onStop();

    /** Abstract background service provider (seet {@link Services.Provider}). */
    public static abstract class Provider extends Services.Provider {
        private final Class<? extends BackgroundService> mServiceClass;
        private       BackgroundService                  mServiceInstance = null;

        protected Provider(Context context, Class<? extends BackgroundService> serviceClass,
                           ServiceId id) {
            super(context, id);
            mServiceClass = serviceClass;
        }

        @Override
        protected void startService(ResponseListener listener) {
            ComponentName res = context.startService(new Intent(context, mServiceClass));
            if (res == null) {
                Log.e(TAG, String.format("Failed to start background service '%s'!",
                        getServiceName()));
            }
        }

        @Override
        protected void stopService() {
            if (mServiceInstance != null) {
                mServiceInstance.onStop();
                mServiceInstance = null;
                killService();
            }
        }

        @Override
        public boolean isServiceRunning() {
            return mServiceInstance != null;
        }

        /** Stop the service without calling {@link BackgroundService#onStop()}. */
        private void killService() {
            context.stopService(new Intent(context, mServiceClass));
        }
    }

}
