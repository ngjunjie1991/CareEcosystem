package com.ucsf.services;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import com.ucsf.R;
import com.ucsf.core.data.DataManager;
import com.ucsf.core.data.DeviceLocation;
import com.ucsf.core.data.Entry;
import com.ucsf.core.data.Timestamp;
import com.ucsf.core.services.BackgroundService;
import com.ucsf.core.services.ServiceId;
import com.ucsf.core.services.ServiceParameter;
import com.ucsf.core.services.UploaderService;
import com.ucsf.data.PatientProfile;
import com.ucsf.data.Settings;

/**
 * Saves the GPS coordinates of the patient every 15 minutes.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public class GPSLocationService extends BackgroundService implements LocationListener {
    private static final String            TAG                   = "ucsf:GPSLocationService";
    public  static final String            KEY_LATITUDE          = "latitude";
    public  static final String            KEY_LONGITUDE         = "longitude";
    private static       Provider          mInstance;
    private static       DataManager.Table mTable;

    /**
     * Returns the database storing the patient phone GPS coordinates.
     */
    public static DataManager.Table getTable(DataManager instance) throws Exception {
        if (mTable == null)
            mTable = UploaderService.addTable(
                    instance,
                    "gps",
                    DeviceLocation.PatientPhone,
                    new DataManager.TableField(DataManager.KEY_PATIENT_ID, DataManager.Type.Text),
                    new DataManager.TableField(DataManager.KEY_TIMESTAMP, DataManager.Type.Text),
                    new DataManager.TableField(DataManager.KEY_IS_COMMITTED, DataManager.Type.Boolean, 0),
                    new DataManager.TableField(KEY_LATITUDE, DataManager.Type.Real),
                    new DataManager.TableField(KEY_LONGITUDE, DataManager.Type.Real)
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

    /**
     * Format the given location to a string value.
     */
    public static String locationToString(Location location) {
        return String.format("[%s; %s]",
                Location.convert(location.getLatitude(), Location.FORMAT_SECONDS),
                Location.convert(location.getLongitude(), Location.FORMAT_SECONDS));
    }

    /**
     * Returns the GPS provider that this service will use to retrieve the GPS coordinates.
     */
    private static String getGpsProvider(LocationManager locationManager) {
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(true);
        criteria.setPowerRequirement(Criteria.POWER_LOW);

        return locationManager.getBestProvider(criteria, true);
    }

    @Override
    public Provider getProvider() {
        return getProvider(this);
    }

    @Override
    public void onLocationChanged(Location location) {
        final PatientProfile profile = Settings.getCurrentPatientProfile(this);

        // Create a new entry in the database
        try (DataManager instance = DataManager.get(this)) {
            getTable(instance).add(
                    new Entry(DataManager.KEY_PATIENT_ID, profile.patientId),
                    new Entry(DataManager.KEY_TIMESTAMP, Timestamp.getTimestamp()),
                    new Entry(KEY_LATITUDE, location.getLatitude()),
                    new Entry(KEY_LONGITUDE, location.getLongitude())
            );

            Log.d("GPSLocationService", String.format("New GPS entry: [patient: %s; location: %s]",
                    profile.patientId, locationToString(location)));
        } catch (Exception e) {
            Log.e(TAG, "Failed to save GPS location: ", e);
        }

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    protected void onStart() throws Exception {
        LocationManager locationManager =
                (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(getGpsProvider(locationManager),
                getProvider().getInterval(), 10, this);
    }

    @Override
    protected void onStop() {
        LocationManager locationManager =
                (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.removeUpdates(this);
    }

    /**
     * GPS location service provider class.
     */
    public static class Provider extends BackgroundService.Provider {
        private final ServiceParameter<Long> mInterval;

        private Provider(Context context) {
            super(context, GPSLocationService.class, ServiceId.PP_GpsLocationService);
            mInterval = addParameter("INTERVAL", R.string.parameter_interval, 90000L); // 15 minutes
        }

        /**
         * Returns the period between two acquisition. Default is 15 minutes.
         */
        public long getInterval() {
            return mInterval.get();
        }

        /**
         * Returns the last known phone location.
         */
        public Location getLastKnownLocation() {
            LocationManager locationManager =
                    (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            return locationManager.getLastKnownLocation(getGpsProvider(locationManager));
        }
    }

}
