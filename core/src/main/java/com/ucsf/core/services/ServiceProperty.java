package com.ucsf.core.services;


import android.content.Context;
import android.util.Log;

import java.io.Serializable;

/**
 * Abstract service property, i.e. either a parameter or a callback. Holds an unique tag, a value
 * and a description. In the case of a callback the value is the {@link Annotations annotation}
 * referencing the method to execute.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public abstract class ServiceProperty<T extends Serializable> {
    private static final String TAG = "ucsf:ServiceProperty";

    /** Context used for I/O in the database. */
    public final Context   context;

    /** Unique identifier of the service to which this propery belongs. */
    public final ServiceId service;

    /** Unique identifier of the property. */
    public final String    tag;

    /** Description of the property. */
    public final String    description;

    /** Value of the property. */
    private      T         mValue;

    public ServiceProperty(Context context, ServiceId service, String tag, String description,
                           T value) {
        this.context = context;
        this.service = service;
        this.tag = tag;
        this.description = description;
        this.mValue = value;
    }

    /** Returns the property value. */
    public T get() {
        return mValue;
    }

    /** Set the property value. Allows some flexibility in the type of object you can pass. */
    public void set(Serializable value) {
        try {
            if (value instanceof Number) {
                if (mValue instanceof Long)
                    mValue = (T) mValue.getClass().cast(((Number) value).longValue());
                else if (mValue instanceof Integer)
                    mValue = (T) mValue.getClass().cast(((Number) value).intValue());
                else if (mValue instanceof Float)
                    mValue = (T) mValue.getClass().cast(((Number) value).floatValue());
                else if (mValue instanceof Double)
                    mValue = (T) mValue.getClass().cast(((Number) value).doubleValue());
                else if (mValue instanceof String)
                    mValue = (T) mValue.getClass().cast(value.toString());
                else
                    mValue = (T) mValue.getClass().cast(value);
            } else if (value instanceof String) {
                if (mValue instanceof Long)
                    mValue = (T) mValue.getClass().cast(Long.valueOf((String) value));
                else if (mValue instanceof Integer)
                    mValue = (T) mValue.getClass().cast(Integer.valueOf((String) value));
                else if (mValue instanceof Float)
                    mValue = (T) mValue.getClass().cast(Float.valueOf((String) value));
                else if (mValue instanceof Double)
                    mValue = (T) mValue.getClass().cast(Double.valueOf((String) value));
                else
                    mValue = (T) mValue.getClass().cast(value);
            } else
                mValue = (T) mValue.getClass().cast(value);
        } catch (Exception e) {
            Log.e(TAG, String.format("Invalid cast from '%s' to '%s'!",
                    value.getClass().getName(), mValue.getClass().getName()));
        }
    }
}
