package com.ucsf.security;

import android.app.Activity;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

/**
 * Class responsible of enabling/disabling the camera.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public class AdminInstance extends DeviceAdminReceiver {
    public static final int REQUEST_ENABLE_ADMIN_MODE = 4321;

    /**
     * Indicates if the admin mode is enabled. Camera can be disabled only in admin mode.
     */
    public static boolean isInAdminMode(Context context) {
        DevicePolicyManager dm =
                (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName deviceAdmin = new ComponentName(context, AdminInstance.class);
        return dm.isAdminActive(deviceAdmin);
    }

    /**
     * Message the usr to accept admin mode. The calling activity must handle the resultCode
     * REQUEST_ENABLE_ADMIN_MODE in onActivityResult(int requestCode, int resultCode, Intent data).
     */
    public static void enableAdminMode(Activity activity) {
        ComponentName deviceAdmin = new ComponentName(activity, AdminInstance.class);

        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, deviceAdmin);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Explanation");
        activity.startActivityForResult(intent, REQUEST_ENABLE_ADMIN_MODE);
    }

    /**
     * Toggle the camera. The admin mode must be active.
     */
    public static void toggleCamera(Context context) {
        DevicePolicyManager dm =
                (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName deviceAdmin = new ComponentName(context, AdminInstance.class);
        dm.setCameraDisabled(deviceAdmin, !dm.getCameraDisabled(deviceAdmin));
    }

    /**
     * Indicates if the camera is currently disabled.
     */
    public static boolean isCameraDisabled(Context context) {
        DevicePolicyManager dm =
                (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName deviceAdmin = new ComponentName(context, AdminInstance.class);
        return dm.getCameraDisabled(deviceAdmin);
    }

}
