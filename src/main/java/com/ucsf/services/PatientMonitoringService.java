package com.ucsf.services;

import android.content.Context;
import android.location.Location;

import com.ucsf.core.data.DeviceLocation;
import com.ucsf.core.data.Entry;
import com.ucsf.core.data.Sender;
import com.ucsf.core.services.Messages;
import com.ucsf.core.services.ServiceId;
import com.ucsf.core.services.UserMonitoringService;
import com.ucsf.data.PatientProfile;
import com.ucsf.data.Settings;

import org.json.JSONObject;

/**
 * Patient phone implementation of the {@link UserMonitoringService monitoring service}. Check
 * also if the patient is at home and if the watch is in range.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public class PatientMonitoringService extends UserMonitoringService {
    private static final String   TAG       = "ucsf:PatientWatcher";
    private static       Provider mInstance = null;

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
        return getProvider(getContext());
    }

    /**
     * Patient monitoring service provider class.
     */
    public static class Provider extends UserMonitoringService.Provider {
        private final PersistentParameter<Boolean> isWatchInRange;
        private final PersistentParameter<Boolean> isPatientAtHome;

        public Provider(Context context) {
            super(context, PatientMonitoringService.class, ServiceId.PP_PatientWatcherService);

            isWatchInRange  = new PersistentParameter<>("IS_WATCH_IN_RANGE", true);
            isPatientAtHome = new PersistentParameter<>("IS_PATIENT_INSIDE", true);
        }

        @Override
        public void check() {
            super.check();
            checkWatchConnection();
            checkPatientIndoor();
        }

        /**
         * Checks if the patient watch is in range.
         */
        private void checkWatchConnection() {
            DeviceInterface.pingWatch(context,
                    new com.ucsf.core.services.DeviceInterface.RequestListener() {
                        @Override
                        public void requestProcessed(Messages.Request request, JSONObject data)
                                throws Exception
                        {
                            isWatchInRange.set(true);
                            ServerUploaderService.getProvider(context).sendEvent(
                                    new Sender(com.ucsf.data.Settings.getCurrentUserId(context),
                                            DeviceLocation.PatientPhone),
                                    Messages.Event.WATCH_CONNECTION_RETRIEVED);
                        }

                        @Override
                        public void requestTimeout(Messages.Request request) throws Exception {
                            isWatchInRange.set(false);
                            ServerUploaderService.getProvider(context).sendEvent(
                                    new Sender(com.ucsf.data.Settings.getCurrentUserId(context),
                                            DeviceLocation.PatientPhone),
                                    Messages.Event.WATCH_CONNECTION_LOST);
                        }

                        @Override
                        public void requestCancelled(Messages.Request request) throws Exception {}
                    });
        }

        /**
         * Checks if the patient is at home, i.e. if its GPS coordinates are not more that 100
         * meters away from his home location.
         */
        private void checkPatientIndoor() {
            PatientProfile profile  = Settings.getCurrentPatientProfile(context);
            Location       location = GPSLocationService.getProvider(context).getLastKnownLocation();
            float          distance = profile.getHomeDistance(location);
            if (distance > 100) {
                isPatientAtHome.set(false);
                DeviceInterface.sendEvent(context, Messages.Event.PATIENT_OUTSIDE);
                ServerUploaderService.getProvider(context).sendEvent(
                        new Sender(profile.patientId, DeviceLocation.PatientPhone),
                        Messages.Event.PATIENT_OUTSIDE
                );
            } else {
                isPatientAtHome.set(true);
                DeviceInterface.sendEvent(context, Messages.Event.PATIENT_INSIDE);
                ServerUploaderService.getProvider(context).sendEvent(
                        new Sender(profile.patientId, DeviceLocation.PatientPhone),
                        Messages.Event.PATIENT_INSIDE
                );
            }

        }

        @Override
        protected void onLowBatteryEvent(int capacity) {
            ServerUploaderService.getProvider(context).sendEvent(
                    new Sender(com.ucsf.core.data.Settings.getCurrentUserId(context),
                            DeviceLocation.PatientPhone),
                    Messages.Event.LOW_BATTERY,
                    new Entry(KEY_CAPACITY, capacity)
            );
        }

        @Override
        protected void onBatteryOkayEvent() {
            ServerUploaderService.getProvider(context).sendEvent(
                    new Sender(com.ucsf.core.data.Settings.getCurrentUserId(context),
                            DeviceLocation.PatientPhone),
                    Messages.Event.BATTERY_OKAY
            );
        }
    }


}
