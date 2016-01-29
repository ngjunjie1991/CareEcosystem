package com.ucsf.core_phone.services;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.google.android.gms.iid.InstanceIDListenerService;
import com.ucsf.core.data.Settings;
import com.ucsf.core.services.ResponseListener;

/**
 * Service responsible of maintaining the validity of GCM tokens. GCM protocol is used to
 * communicate with the server, more precisely to receive message from the server.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public class GcmInstance extends InstanceIDListenerService {
    private static final String  TAG                              = "ucsf:GcmInstance";
    private static final String  PROJECT_NUMBER                   = "1073883312841";
    private static final String  KEY_REGISTRATION_ID              = "REGISTRATION_ID";
    private static final String  KEY_REGISTRATION_VERSION         = "REGISTRATION_VERSION";
    private static final long    DEFAULT_RETRY_DELAY              = 500;
    private static final int     PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static       String  REGISTRATION_ID                  = null;
    private static       Integer LAST_REGISTRATION_VERSION        = null;
    private static       long    RETRY_DELAY                      = DEFAULT_RETRY_DELAY;

    public interface TokenRequestListener {
        void onTokenReceived(String token);
    }

    /**
     * Returns the application GCM token. If the token is not valid, return an empty string.
     */
    public static String getToken(final ServerUploaderService.Provider provider) {
        if (REGISTRATION_ID == null || REGISTRATION_ID.isEmpty()) {
            loadRegistrationId(provider);
            return "";
        }
        return REGISTRATION_ID;
    }

    /**
     * Requests the application GCM token.
     */
    public static void getToken(final ServerUploaderService.Provider provider,
                                  final TokenRequestListener listener)
    {
        if (REGISTRATION_ID == null || REGISTRATION_ID.isEmpty()) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    loadRegistrationId(provider, listener);
                    return null;
                }
            }.execute();
        } else
            listener.onTokenReceived(REGISTRATION_ID);
    }

    /**
     * Retrieve the application GCM token. If not found or if the application version has changed,
     * generates a new one. This method is asynchronous.
     */
    public static void loadRegistrationId(
            final ServerUploaderService.Provider provider)
    {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                loadRegistrationId(provider, new TokenRequestListener() {
                    @Override
                    public void onTokenReceived(String token) {
                        Log.i(TAG, "New GCM token received!");
                    }
                });
                return null;
            }
        }.execute();
    }

    /**
     * Retrieve the application GCM token. If not found or if the application version has changed,
     * generates a new one. Notify the given listener when the new token is generated.
     */
    private static synchronized void loadRegistrationId(
            final ServerUploaderService.Provider provider,
            final TokenRequestListener listener)
    {
        // Get the version of the application for the last update of the registration id
        if (LAST_REGISTRATION_VERSION == null) {
            try {
                LAST_REGISTRATION_VERSION = (Integer) Settings.loadParameter(provider.context,
                        KEY_REGISTRATION_VERSION, 0);
            } catch (Exception e) {
                Log.e(TAG, "Failed to retrieve last registration version: ", e);
                LAST_REGISTRATION_VERSION = 0;
            }
        }

        // Get the current application version
        int appVersion;
        try {
            PackageInfo packageInfo = provider.context.getPackageManager()
                    .getPackageInfo(provider.context.getPackageName(), 0);
            appVersion = packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not get package name: ", e);
            appVersion = -1;
        }

        // Check if the registration id is still valid
        boolean needNewId;
        if (LAST_REGISTRATION_VERSION != appVersion) {
            LAST_REGISTRATION_VERSION = appVersion;
            needNewId = true;
            try {
                Settings.saveParameter(provider.context, KEY_REGISTRATION_VERSION, LAST_REGISTRATION_VERSION);
            } catch (Exception e) {
                Log.e(TAG, "Failed to save registration version: ", e);
            }
        } else {
            try {
                REGISTRATION_ID = (String) Settings.loadParameter(provider.context, KEY_REGISTRATION_ID, "");
                needNewId       = REGISTRATION_ID.isEmpty();
            } catch (Exception e) {
                Log.e(TAG, "Failed to retrieve registration id: ", e);
                needNewId = true;
            }
        }

        // If the registration id is not valid, get a new one
        if (needNewId) {
            InstanceID iid = InstanceID.getInstance(provider.context);
            try {
                // Get the token
                final String token = iid.getToken(PROJECT_NUMBER, GoogleCloudMessaging.INSTANCE_ID_SCOPE);

                // If the token is different of the previous one, send it to the server
                if (!token.equals(REGISTRATION_ID)) {
                    provider.sendRegistrationId(
                            provider.getDeviceLocation(),
                            REGISTRATION_ID,
                            token,
                            new ResponseListener() {
                                @Override
                                public void onSuccess() {
                                    // Save the new token
                                    REGISTRATION_ID = token;
                                    try {
                                        Settings.saveParameter(provider.context,
                                                KEY_REGISTRATION_ID, REGISTRATION_ID);
                                    } catch (Exception e) {
                                        Log.e(TAG, "Failed to save registration id: ", e);
                                    }

                                    RETRY_DELAY = DEFAULT_RETRY_DELAY;

                                    listener.onTokenReceived(token);
                                }

                                @Override
                                public void onFailure(String error, Throwable e) {
                                    Log.e(TAG, "Failed to upload registration id: " + error, e);

                                    new Handler(provider.context.getMainLooper()).postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            new AsyncTask<Void, Void, Void>() {
                                                @Override
                                                protected Void doInBackground(Void... params) {
                                                    loadRegistrationId(provider, listener);
                                                    return null;
                                                }
                                            }.execute();
                                        }
                                    }, RETRY_DELAY);
                                    RETRY_DELAY *= 2;
                                }
                            });
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to update registration id: ", e);

                new Handler(provider.context.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        new AsyncTask<Void, Void, Void>() {
                            @Override
                            protected Void doInBackground(Void... params) {
                                loadRegistrationId(provider, listener);
                                return null;
                            }
                        }.execute();
                    }
                }, RETRY_DELAY);
                RETRY_DELAY *= 2;
            }
        } else
            listener.onTokenReceived(REGISTRATION_ID);
    }

    /**
     * Checks the device to make sure it has the Google Play Service APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    public static boolean checkPlayServices(Activity activity,
                                            final ServerUploaderService.Provider provider) {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, activity,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else
                Log.e(TAG, "This device is not supported.");
            return false;
        }

        // Make sure that the registration id is loaded
        loadRegistrationId(provider);

        return true;
    }

}
