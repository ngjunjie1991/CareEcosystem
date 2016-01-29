package com.ucsf.core.services;

import android.content.Context;
import android.util.Log;

import com.ucsf.core.data.DataManager;
import com.ucsf.core.data.DeviceLocation;
import com.ucsf.core.data.Entry;

import java.io.Serializable;

/**
 * Service parameter structure. Holds a value, a default value, an unique tag and a description.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public class ServiceParameter<T extends Serializable> extends ServiceProperty<T> {
    private static final String            TAG              = "ucsf:ServiceParameter";
    private static final String            KEY_SERVICE      = "service";
    private static final String            KEY_PARAMETER    = "parameter";
    private static final String            KEY_VALUE        = "value";
    private static final String            KEY_IS_DEFAULT   = "is_default";
    private static       DataManager.Table mParametersTable;
    private              T                 mDefaultValue;
    private              boolean           mIsDefault;
    private              boolean           mLoaded          = false;


    public ServiceParameter(Context context, ServiceId service, String tag, String description,
                            T value, T defaultValue, boolean isDefault)
    {
        super(context, service, tag, description, value);
        this.mDefaultValue = defaultValue;
        this.mIsDefault = isDefault;
    }

    public ServiceParameter(Context context, ServiceId service, String tag, String description,
                            T value) {
        this(context, service, tag, description, value, value, true);
    }

    public ServiceParameter(Context context, ServiceId service, String tag, int descriptionId,
                            T value) {
        this(context, service, tag, context.getString(descriptionId), value);
    }

    private static DataManager.Table getTable(DataManager instance) throws Exception {
        if (mParametersTable == null)
            mParametersTable = instance.createTable(
                    "services_parameters",
                    DeviceLocation.Unknown,
                    new DataManager.TableField(KEY_SERVICE   , DataManager.Type.Text),
                    new DataManager.TableField(KEY_PARAMETER , DataManager.Type.Text),
                    new DataManager.TableField(KEY_VALUE     , DataManager.Type.Blob),
                    new DataManager.TableField(KEY_IS_DEFAULT, DataManager.Type.Boolean, 1)
            );
        return mParametersTable;
    }

    /** Returns the parameter default value. */
    public T defaultValue() {
        return mDefaultValue;
    }

    /** Set the parameter default value. */
    public void setDefaultValue(T value) {
        mDefaultValue = value;
    }

    /** Indicates if the current valus is the default value. */
    public boolean isDefault() {
        return mIsDefault;
    }

    @Override
    public T get() {
        if (mIsDefault)
            return mDefaultValue;
        if (!mLoaded)
            load();
        return super.get();
    }

    @Override
    public void set(Serializable value) {
        super.set(value);
        save();
        mIsDefault = false;
    }

    /** Resets the parameter to its default value. */
    public void reset() {
        mIsDefault = true;
        save();
    }

    /** Loads the parameter from the database. */
    private void load() {
        try (DataManager instance = DataManager.get(context)) {
            DataManager.Cursor cursor = getTable(instance).fetch(
                    new String[]{KEY_VALUE, KEY_IS_DEFAULT},
                    new DataManager.Condition.Equal<>(KEY_SERVICE, service),
                    new DataManager.Condition.Equal<>(KEY_PARAMETER, tag));
            if (cursor != null && cursor.moveToFirst()) {
                if (cursor.getBoolean(KEY_IS_DEFAULT))
                    mIsDefault = true;
                else
                    super.set(cursor.getSerializable(KEY_VALUE));
            }
            mLoaded = true;
        } catch (Exception e) {
            Log.e(TAG, String.format("Failed to load parameter '%s:%s': ",
                    service.getName(context), tag), e);
        }
    }

    /** Saves the parameter to the database. */
    private void save() {
        try (DataManager instance = DataManager.get(context)) {
            getTable(instance).fetchAndAdd(
                    new Entry[]{
                            new Entry(KEY_SERVICE, service),
                            new Entry(KEY_PARAMETER, tag)
                    },
                    new Entry(KEY_VALUE, get(), true),
                    new Entry(KEY_IS_DEFAULT, isDefault() ? 1 : 0)
            );
        } catch (Exception e) {
            Log.e(TAG, String.format("Failed to save parameter '%s:%s': ",
                    service.getName(context), tag), e);
        }
    }

}
