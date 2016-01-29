package com.ucsf.core.services;


import android.content.Context;
import android.util.Log;

import java.lang.reflect.Method;

/**
 * Service callback structure. Use {@link Annotations annotations} to reference methods to
 * execute.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public class ServiceCallback extends ServiceProperty<String> {
    private static final String TAG = "ServiceCallback";

    protected ServiceCallback(Context context, ServiceId id, String tag, String description,
                              String annotation) {
        super(context, id, tag, description, annotation);
    }

    protected ServiceCallback(Context context, ServiceId id, String tag, int descriptionId,
                              String annotation) {
        super(context, id, tag, context.getString(descriptionId), annotation);
    }

    /** Execute the method linked to this callback.*/
    public void execute() {
        try {
            Services.Provider provider = Services.getProvider(service);
            Method method = Annotations.getMethod(provider.getClass(), get());
            if (provider.isServiceEnabled())
                method.invoke(provider);
        } catch (Exception e) {
            Log.e(TAG, String.format("Failed to execute method annotated '%s' for service '%s': ",
                    get(), service.getName(context)), e);
        }
    }

}