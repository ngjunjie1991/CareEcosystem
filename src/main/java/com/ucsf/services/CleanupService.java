package com.ucsf.services;

import android.content.Context;

import com.ucsf.core.services.ServiceId;

/**
 * Patient phone implementation of the
 * {@link com.ucsf.core.services.CleanupService cleanup service}.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public class CleanupService extends com.ucsf.core.services.CleanupService {
    private static Provider mInstance = null;

    public static Provider getProvider(Context context) {
        if (mInstance == null)
            mInstance = new Provider(context);
        return mInstance;
    }

    @Override
    protected void execute(Context context) throws Exception {
        getProvider().cleanData();
    }

    @Override
    public Provider getProvider() {
        return getProvider(getContext());
    }

    /**
     * Patient phone cleanup service provider class.
     */
    public static class Provider extends com.ucsf.core.services.CleanupService.Provider {
        protected Provider(Context context) {
            super(context, com.ucsf.services.CleanupService.class, ServiceId.PP_CleanupService);
        }
    }

}
