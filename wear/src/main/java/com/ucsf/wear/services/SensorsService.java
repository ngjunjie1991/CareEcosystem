package com.ucsf.wear.services;

import android.content.Context;

import com.ucsf.core.data.DataManager;
import com.ucsf.core.data.SharedTables;
import com.ucsf.core.services.ServiceId;

/**
 * Watch implementation of the {@link com.ucsf.core.services.SensorsService sensors service}.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public class SensorsService extends com.ucsf.core.services.SensorsService {
    private static Provider mInstance;

    /**
     * Returns the service provider.
     */
    public static Provider getProvider(Context context) {
        if (mInstance == null)
            mInstance = new Provider(context);
        return mInstance;
    }

    @Override
    public Provider getProvider() {
        return getProvider(this);
    }

    @Override
    protected DataManager.Table getSensorsTable(DataManager instance) throws Exception {
        return SharedTables.Sensors.getTable(instance);
    }

    public static class Provider extends com.ucsf.core.services.SensorsService.Provider {
        public Provider(Context context) {
            super(context, com.ucsf.wear.services.SensorsService.class, ServiceId.PW_SensorsService, 10000L);
        }
    }
}
