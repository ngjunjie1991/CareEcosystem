package com.ucsf.services;

import android.content.Context;

import com.ucsf.core.services.ServiceId;

/**
 * Patient phone implementation of the server listener service.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public class ServerListenerService extends com.ucsf.core_phone.services.ServerListenerService {
    private static Provider mInstance = null;

    /**
     * Returns the service provider.
     */
    public static Provider getProvider(Context context) {
        if (mInstance == null)
            mInstance = new Provider(context);
        return mInstance;
    }

    /**
     * Server listener service provider class.
     */
    public static class Provider extends com.ucsf.core_phone.services.ServerListenerService.Provider {
        public Provider(Context context) {
            super(context, ServiceId.PP_ServerListenerService);
        }

        @Override
        public ServerUploaderService.Provider getServerUploaderProvider() {
            return ServerUploaderService.getProvider(context);
        }
    }
}
