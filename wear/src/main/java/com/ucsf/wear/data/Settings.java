package com.ucsf.wear.data;

import android.content.Context;

import com.ucsf.core.data.DataManager;
import com.ucsf.core.data.SharedTables;
import com.ucsf.core.services.Services;

/**
 * Watch implementation of the {@link com.ucsf.core.data.Settings settings} class.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public class Settings extends com.ucsf.core.data.Settings {
    private static Settings mInstance = null;

    protected Settings(Context context) {
        super(context);
    }

    /**
     * Returns the settings instance object.
     */
    public static Settings getInstance(Context context) {
        if (mInstance == null)
            mInstance = new Settings(context.getApplicationContext());
        return mInstance;
    }

    @Override
    public DataManager.Table getLogsTable(DataManager instance) throws Exception {
        return SharedTables.Logs.getTable(instance);
    }
}
