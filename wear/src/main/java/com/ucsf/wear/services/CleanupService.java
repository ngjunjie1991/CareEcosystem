package com.ucsf.wear.services;

import android.content.Context;

import com.ucsf.core.services.ServiceId;

/**
 * Watch implementation of the {@link com.ucsf.core.services.CleanupService cleanup service}.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public class CleanupService extends com.ucsf.core.services.CleanupService {
    private static Provider mInstance = null;

    /** Returns the cleanup service provider. */
    public static Provider getProvider(Context context) {
        if (mInstance == null)
            mInstance = new Provider(context);
        return mInstance;
    }

    @Override
    protected void execute(Context context) throws Exception {
        StartupService.loadTables(context);
        getProvider().cleanData();
    }

    @Override
    public com.ucsf.wear.services.CleanupService.Provider getProvider() {
        return getProvider(getContext());
    }

    public static class Provider extends com.ucsf.core.services.CleanupService.Provider {
        private Provider(Context context) {
            super(context, com.ucsf.wear.services.CleanupService.class, ServiceId.PW_CleanupService);
        }
    }
}
