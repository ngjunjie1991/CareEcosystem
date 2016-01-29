package com.ucsf.ui.tester;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.ucsf.R;
import com.ucsf.core.services.Services;
import com.ucsf.core_phone.ui.Theme;
import com.ucsf.core_phone.ui.widgets.CustomDialog;
import com.ucsf.data.PatientProfile;
import com.ucsf.data.Settings;
import com.ucsf.security.AdminInstance;
import com.ucsf.ui.StartScreenActivity;
import com.ucsf.ui.admin.PatientIdActivity;
import com.ucsf.ui.widgets.AppScreen;

/**
 * Tester main menu.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public class TesterMenuActivity extends AppScreen {
    private static final String TAG = "ucsf:TesterMenuActivity";

    private Button mCameraButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, Disposition.Centered, Theme.Admin);

        setTitle(R.string.screen_tester_main_menu);

        // Settings button
        Button settingsButton = addMenuButton(R.string.action_settings, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openChildActivity(PatientIdActivity.class);
            }
        });
        settingsButton.setVisibility(Services.areServicesEnabled(this) ? View.GONE : View.VISIBLE);

        // Service
        addMenuButton(R.string.action_services, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openChildActivity(ServicesActivity.class);
            }
        });

        // Ground trust
        addMenuButton(R.string.action_ground_trust, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check that the user profile is properly setup
                if (areSettingsValid())
                    openChildActivity(GroundTrustActivity.class);
            }
        });

        // Ground trust
        addMenuButton(R.string.action_data_tables, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openChildActivity(TablesListActivity.class);
            }
        });

        // Camera
        mCameraButton = addMenuButton(AdminInstance.isCameraDisabled(TesterMenuActivity.this) ?
                        R.string.action_enable_camera : R.string.action_disable_camera,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AdminInstance.toggleCamera(TesterMenuActivity.this);
                        mCameraButton.setText(AdminInstance.isCameraDisabled(TesterMenuActivity.this) ?
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

    private boolean areSettingsValid() {
        PatientProfile patientProfile = Settings.getCurrentPatientProfile(this);
        if (patientProfile == null || !patientProfile.isValid()) {
            CustomDialog.Builder dialogBuilder =
                    new CustomDialog.Builder(TesterMenuActivity.this);
            dialogBuilder.addFooterButton(R.string.action_done, null);
            dialogBuilder.setMessage(R.string.alert_settings_invalid);
            dialogBuilder.show();
            return false;
        }
        return true;
    }

}
