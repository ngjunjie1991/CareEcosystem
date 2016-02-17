package com.ucsf.core.services;

import android.content.Context;

import com.ucsf.core.R;
import com.ucsf.core.data.DeviceLocation;

/**
 * Enumeration of all the available services.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public enum ServiceId {
    PW_StartupService(DeviceLocation.PatientWatch, R.string.service_startup, R.string.service_startup_descriptions),
    PP_StartupService(DeviceLocation.PatientPhone, R.string.service_startup, R.string.service_startup_descriptions),
    PP_CleanupService(DeviceLocation.PatientPhone, R.string.service_cleanup, R.string.service_cleanup_description),
    PW_CleanupService(DeviceLocation.PatientWatch, R.string.service_cleanup, R.string.service_cleanup_description),
    PP_SensorsService(DeviceLocation.PatientPhone, R.string.service_sensors, R.string.service_sensors_description),
    PW_SensorsService(DeviceLocation.PatientWatch, R.string.service_sensors, R.string.service_sensors_description),
    PW_RangingService(DeviceLocation.PatientWatch, R.string.service_beacon_monitoring, R.string.service_beacon_monitoring_description),
    PW_SensorTagService(DeviceLocation.PatientWatch, R.string.service_sensortag_monitoring, R.string.service_sensortag_monitoring_description),
    PW_PhoneUploaderService(DeviceLocation.PatientWatch, R.string.service_phone_data, R.string.service_phone_data_description),
    PW_PatientWatcherService(DeviceLocation.PatientWatch, R.string.service_patient_watcher, R.string.service_patient_watcher_description),
    PP_PatientWatcherService(DeviceLocation.PatientPhone, R.string.service_patient_watcher, R.string.service_patient_watcher_description),
    PP_ServerListenerService(DeviceLocation.PatientPhone, R.string.service_server_listener, R.string.service_server_listener_description),
    PP_GpsLocationService(DeviceLocation.PatientPhone, R.string.service_gps, R.string.service_gps_description),
    PP_ServerUploaderService(DeviceLocation.PatientPhone, R.string.service_server, R.string.service_server_description);

    /** Indicates in which device the service is running. */
    public final DeviceLocation device;

    private final int mNameId;
    private final int mDescriptionId;

    ServiceId(DeviceLocation device, int nameId, int descriptionId) {
        this.device = device;
        this.mNameId = nameId;
        this.mDescriptionId = descriptionId;
    }

    /** Returns the service name. */
    public String getName(Context context) {
        return context.getString(mNameId);
    }

    /** Returns the service description. */
    public String getDescription(Context context) {
        return context.getString(mDescriptionId);
    }
}