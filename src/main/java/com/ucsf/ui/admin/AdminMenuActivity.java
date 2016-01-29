package com.ucsf.ui.admin;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.ucsf.R;
import com.ucsf.core.services.Services;
import com.ucsf.core_phone.ui.Theme;
import com.ucsf.core_phone.ui.widgets.CustomDialog;
import com.ucsf.data.PatientProfile;
import com.ucsf.data.Settings;
import com.ucsf.security.AdminInstance;
import com.ucsf.services.DeviceInterface;
import com.ucsf.services.StartupService;
import com.ucsf.ui.StartScreenActivity;
import com.ucsf.ui.widgets.AppScreen;

/**
 * Main menu for admin.
 * Allows to edit Settings, show past days patient data and start/stop services.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public class AdminMenuActivity extends AppScreen {
    private static final String TAG = "ucsf:AdminMenuActivity";

    private Button mToggleServicesButton;
    private Button mSettingsButton;
    private Button mCameraButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        onCreate(savedInstanceState, Disposition.Centered, Theme.Caregiver);

        setTitle(R.string.screen_admin_main_menu);

        // Settings button
        mSettingsButton = addMenuButton(R.string.action_settings, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openChildActivity(PatientIdActivity.class);
            }
        });
        mSettingsButton.setVisibility(Services.areServicesEnabled(this) ? View.GONE : View.VISIBLE);

        // Toggle services button
        mToggleServicesButton = addMenuButton(Services.areServicesEnabled(this) ?
                        R.string.action_stop_services : R.string.action_start_services,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        toggleServices();
                    }
                });

        // Camera button
        mCameraButton = addMenuButton(AdminInstance.isCameraDisabled(AdminMenuActivity.this) ?
                        R.string.action_enable_camera : R.string.action_disable_camera,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AdminInstance.toggleCamera(AdminMenuActivity.this);
                        mCameraButton.setText(AdminInstance.isCameraDisabled(AdminMenuActivity.this) ?
                                R.string.action_enable_camera : R.string.action_disable_camera);
                    }
                }
        );

        addFooterButton(R.string.action_logout, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goBackToParentActivity(StartScreenActivity.class);
            }
        });
    }

    /**
     * Method called when the user activates the services.
     */
    private void onServicesStarted() {
        getHeader().updateStatusIcon(true);
        mToggleServicesButton.setText(R.string.action_stop_services);
        mSettingsButton.setVisibility(View.GONE);
    }

    /**
     * Method called when the user stops the services.
     */
    private void onServicesStopped() {
        getHeader().updateStatusIcon(false);
        mToggleServicesButton.setText(R.string.action_start_services);
        mSettingsButton.setVisibility(View.VISIBLE);
    }

    private void toggleServices() {
        try {
            if (Services.areServicesEnabled(this)) { // Stop services
                Services.enableServices(this, false);
                DeviceInterface.toggleWatchServices(this, false);
                Services.stopServices();
                onServicesStopped();
            } else {
                // Get the patient profile and make sure it is valid
                PatientProfile patientProfile =
                        Settings.getCurrentPatientProfile(this);
                if (patientProfile != null && patientProfile.isValid()) {
                    // The patient profile is valid, we can then starts the services
                    Services.enableServices(this, true);
                    DeviceInterface.toggleWatchServices(this, true);
                    StartupService.startServices(this);
                    onServicesStarted();
                } else { // Notify the admin that some information are missing
                    CustomDialog.Builder dialogBuilder =
                            new CustomDialog.Builder(this);
                    dialogBuilder.addFooterButton(R.string.action_done, null);
                    dialogBuilder.setMessage(R.string.alert_settings_invalid);
                    dialogBuilder.show();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to toggle services: ", e);
        }
    }

}
