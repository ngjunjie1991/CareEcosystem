package com.ucsf.core.services;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.ucsf.core.data.Entry;
import com.ucsf.core.data.JSON;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Class describing a service and its properties (i.e. callbacks and parameters).
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public class ServiceDescriptor {
    private static final String TAG                  = "ucsf:ServiceDescriptor";
    private static final String KEY_SERVICE_ID       = "SERVICE";
    private static final String KEY_DESCRIPTORS      = "DESCRIPTORS";
    private static final String KEY_TAG              = "TAG";
    private static final String KEY_DESCRIPTION      = "DESCRIPTION";
    private static final String KEY_PROPERTIES       = "PROPERTIES";
    private static final String KEY_STATUS           = "STATUS";
    private static final String KEY_VALUE            = "VALUE";
    private static final String KEY_DEFAULT_VALUE    = "DEFAULT_VALUE";
    private static final String KEY_IS_DEFAULT_VALUE = "IS_DEFAULT";

    /** Unique {@link ServiceId identifier} of the described service. */
    public final ServiceId             service;
    /** List of properties of the described service. */
    public final List<ServiceProperty> properties;
    /** Current {@link Status status} of the described service. */
    public       Status                status;

    private ServiceDescriptor(ServiceId service, List<ServiceProperty> properties, Status status) {
        this.service = service;
        this.properties = properties;
        this.status = status;
    }

    public ServiceDescriptor(Services.Provider provider) {
        this(provider.id, provider.getProperties(), getStatus(provider));
    }

    public ServiceDescriptor(ServiceDescriptor other) {
        this(other.service, other.properties, other.status);
    }

    /**
     * Returns the given service status.
     * @param provider Service provider
     */
    public static Status getStatus(Services.Provider provider) {
        if (!provider.isServiceEnabled() || !Services.areServicesEnabled(provider.context))
            return Status.Disabled;
        if (provider.isServiceRunning())
            return Status.Running;
        return Status.Malfunctioning;
    }

    /**
     * Saves a list of descriptors in a JSON array
     */
    public static JSONObject saveDescriptors(final Collection<ServiceDescriptor> descriptors) {
        return JSON.create(new Entry(KEY_DESCRIPTORS, JSON.create(descriptors,
                new JSON.Delegate() {
                    @Override
                    public Object wrap(Object value) throws Exception {
                        if (value instanceof ServiceDescriptor) {
                            ServiceDescriptor descriptor = (ServiceDescriptor) value;
                            return JSON.create(
                                    new Entry(KEY_SERVICE_ID, descriptor.service),
                                    new Entry(KEY_PROPERTIES, JSON.create(descriptor.properties, this).toString()),
                                    new Entry(KEY_STATUS, descriptor.status.toString())
                            );
                        }
                        if (value instanceof ServiceParameter) {
                            ServiceParameter parameter = (ServiceParameter) value;
                            return JSON.create(
                                    new Entry(KEY_TAG, parameter.tag),
                                    new Entry(KEY_DESCRIPTION, parameter.description),
                                    new Entry(KEY_VALUE, parameter.get()),
                                    new Entry(KEY_DEFAULT_VALUE, parameter.defaultValue()),
                                    new Entry(KEY_IS_DEFAULT_VALUE, parameter.isDefault())
                            );
                        }
                        if (value instanceof ServiceCallback) {
                            ServiceCallback callback = (ServiceCallback) value;
                            return JSON.create(
                                    new Entry(KEY_TAG, callback.tag),
                                    new Entry(KEY_DESCRIPTION, callback.description),
                                    new Entry(KEY_VALUE, callback.get())
                            );
                        }
                        return super.wrap(value);
                    }
                }).toString()));
    }

    /**
     * Loads a list of descriptors from a JSON array.
     */
    public static List<ServiceDescriptor> loadDescriptors(Context context,
                                                          @NonNull JSONObject jsonObject) {
        List<ServiceDescriptor> descriptors = new LinkedList<>();
        try {
            JSONArray jsonArray = new JSONArray(jsonObject.getString(KEY_DESCRIPTORS));
            for (int i = 0; i < jsonArray.length(); ++i) {
                try {
                    JSONObject descr = (JSONObject) jsonArray.get(i);
                    ServiceId service = ServiceId.valueOf(descr.getString(KEY_SERVICE_ID));
                    Status status = Status.valueOf(descr.getString(KEY_STATUS));
                    JSONArray props = new JSONArray(descr.getString(KEY_PROPERTIES));

                    List<ServiceProperty> properties = new LinkedList<>();
                    for (int j = 0; j < props.length(); ++j) {
                        JSONObject prop = (JSONObject) props.get(j);
                        String tag = prop.getString(KEY_TAG);
                        String description = prop.getString(KEY_DESCRIPTION);
                        Serializable value = (Serializable) prop.get(KEY_VALUE);
                        Serializable defaultValue = (Serializable) prop.opt(KEY_DEFAULT_VALUE);

                        if (defaultValue != null) {
                            properties.add(new ServiceParameter(context, service, tag,
                                    description, value, defaultValue,
                                    prop.optBoolean(KEY_IS_DEFAULT_VALUE)));
                        } else
                            properties.add(new ServiceCallback(context, service, tag,
                                    description, (String) value));
                    }

                    descriptors.add(new ServiceDescriptor(service, properties, status));
                } catch (Exception e) {
                    Log.e(TAG, "Failed to load service descriptor: ", e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load service descriptors: ", e);
        }
        return descriptors;
    }

    /**
     * Possible states of a service.
     */
    public enum Status {
        /** The service is running correctly. */
        Running,
        /** The service is disabled. */
        Disabled,
        /** The service is enabled but not running even if services are activated. */
        Malfunctioning
    }
}
