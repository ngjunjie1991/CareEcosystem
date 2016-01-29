package com.ucsf.services;

import android.content.Context;

import com.ucsf.core.data.DataManager;
import com.ucsf.core.data.DeviceLocation;
import com.ucsf.core.data.SharedTables;
import com.ucsf.core.services.ServiceId;
import com.ucsf.core.services.UploaderService;

/**
 * Patient phone implementation of the {@link com.ucsf.core.services.SensorsService sensors service}.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public class SensorsService extends com.ucsf.core.services.SensorsService {
    private static final String   TAG       = "ucsf:SensorsService";
    private static       Provider mInstance = null;

    private static DataManager.Table mTable;

    /**
     * Returns the database table storing the sensors data.
     */
    public static DataManager.Table getTable(DataManager instance) throws Exception {
        if (mTable == null)
            mTable = UploaderService.addTable(
                    instance,
                    "phone_sensors",
                    DeviceLocation.PatientPhone,
                    SharedTables.Sensors.getTable(instance).fields
            );
        return mTable;
    }

    /**
     * Returns the service provider.
     */
    public static Provider getProvider(Context context) {
        if (mInstance == null)
            mInstance = new Provider(context);
        return mInstance;
    }

    @Override
    protected DataManager.Table getSensorsTable(DataManager instance) throws Exception {
        return getTable(instance);
    }

    @Override
    public Provider getProvider() {
        return getProvider(this);
    }

    /**
     * Sensors service provider class.
     */
    public static class Provider extends com.ucsf.core.services.SensorsService.Provider {
        private Provider(Context context) {
            super(context, SensorsService.class, ServiceId.PP_SensorsService, 5000L);
            setParameterDefaultValue(ACCELEROMETER_TAG, true);
        }
    }
}
