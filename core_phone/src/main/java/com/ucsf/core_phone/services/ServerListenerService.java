package com.ucsf.core_phone.services;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;
import com.ucsf.core.services.ResponseListener;
import com.ucsf.core.services.ServiceId;
import com.ucsf.core.services.Services;

import java.util.HashMap;
import java.util.Map;

/**
 * Service responsible of receiving messages from the server.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public abstract class ServerListenerService extends GcmListenerService {
    private static final String                    TAG        = "ucsf:ServerListener";
    private static final Map<ServerListener, Long> mListeners = new HashMap<>();

    @Override
    public void onMessageReceived(String from, Bundle data) {
        String        messageTag = data.getString(ServerMessage.MESSAGE_ID);
        ServerMessage message    = ServerMessage.fromTag(messageTag);
        if (message != ServerMessage.Invalid) {
            Log.d(TAG, "Server message received: " + messageTag);
            broadcastMessage(message, data);
        } else {
            Log.e(TAG, String.format("Invalid server message '%s'!", messageTag));
        }
    }

    /**
     * Broadcasts the given message to the registered listeners.
     */
    private void broadcastMessage(ServerMessage message, Bundle data) {
        switch (message) {
            case Invalid:
                Log.e(TAG, "Invalid server message!");
                break;
            default:
                synchronized (mListeners) {
                    for (Map.Entry<ServerListener, Long> p : mListeners.entrySet()) {
                        if ((message.flag & p.getValue()) == message.flag)
                            p.getKey().onServerMessage(message, data);
                    }
                }
                break;
        }
    }

    /**
     * Server listener service provider class.
     */
    public static abstract class Provider extends Services.Provider {
        public Provider(Context context, ServiceId service) {
            super(context, service);
        }

        @Override
        protected void startService(ResponseListener listener) {
            GcmInstance.loadRegistrationId(getServerUploaderProvider());
        }

        @Override
        public void stopService() {
        }

        @Override
        public boolean isServiceRunning() {
            return !GcmInstance.getToken(getServerUploaderProvider()).isEmpty();
        }

        /** Returns the application uploader service provider. */
        public abstract ServerUploaderService.Provider getServerUploaderProvider();

        /**
         * Registers the given listener to be notified when the given message type is received.
         */
        public void registerListener(ServerMessage type, ServerListener listener) {
            synchronized (mListeners) {
                Long flag = mListeners.get(listener);
                if (flag == null) flag = 0L; // Make sure that the flag is not null
                mListeners.put(listener, flag | type.flag);
            }
        }

        /**
         * Unregisters the given listener associated with the given message type.
         */
        public void unregisterListener(ServerMessage type, ServerListener listener) {
            synchronized (mListeners) {
                Long flag = mListeners.get(listener);
                if (flag != null) {
                    flag &= ~type.flag;
                    if (flag == 0)
                        unregisterListener(listener);
                    else
                        mListeners.put(listener, flag);
                }
            }
        }

        /**
         * Unregisters the given listener for all its attached message types.
         */
        public void unregisterListener(final ServerListener listener) {
            // Remove the listener in an asynchronous task to avoid concurrent exception when
            // iterating through the listeners list
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    synchronized (mListeners) {
                        mListeners.remove(listener);
                    }
                    return null;
                }
            }.execute();
        }

    }
}
