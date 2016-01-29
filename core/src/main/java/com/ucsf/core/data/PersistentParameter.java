package com.ucsf.core.data;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.Serializable;

/**
 * Because this application has many entry points, static variables cannot be used reliably to store
 * data and share them across classes. An PersistentParameter object will synchronize its value
 * with the application database, allowing a consistent state of the underlying parameter.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public class PersistentParameter<T extends Serializable> {
    private static final String TAG = "ucsf:PersistentParam";

    /**
     * Some parameters may require a dynamic default value (for instance the current date). This
     * class is here to provide this functionality.
     */
    public interface DefaultValue<T extends Serializable> {
        /** Returns the parameter default value. */
        T get(Context context);
    }

    public  final String          tag;           /**< Unique identifier of the underlying parameter. */
    private final DefaultValue<T> mDefaultValue; /**< Default value accessor. */

    public PersistentParameter(String tag, final @NonNull T defaultValue) {
        this(tag, new DefaultValue<T>() {
            @Override
            public T get(Context context) {
                return defaultValue;
            }
        });
    }

    public PersistentParameter(String tag, @NonNull DefaultValue<T> defaultValue) {
        this.tag           = tag;
        this.mDefaultValue = defaultValue;
    }

    /**
     * Returns the underlying parameter value. If an error occurs, or if the parameter has never
     * been set, returns the parameter default value instead.
     */
    public synchronized T get(Context context) {
        T defaultValue = mDefaultValue.get(context);
        try {
            return (T) Settings.loadParameter(context, tag, defaultValue);
        } catch (Exception e) {
            Log.e(TAG, String.format("Failed to load settings parameter '%s': ", tag), e);
            return defaultValue;
        }
    }

    /**
     * Set the parameter value.
     */
    public synchronized void set(Context context, @NonNull T value) {
        try {
            Settings.saveParameter(context, tag, value);
        } catch (Exception e) {
            Log.e(TAG, String.format("Failed to save settings parameter '%s': ", tag), e);
        }
    }
}
