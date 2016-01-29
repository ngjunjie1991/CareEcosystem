package com.ucsf.ui.admin;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;

import com.ucsf.R;
import com.ucsf.core_phone.ui.widgets.CustomDialog;
import com.ucsf.data.PatientProfile;
import com.ucsf.data.Settings;
import com.ucsf.ui.widgets.AppScreen;

/**
 * Activity to change the patient ID.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public class PatientUsernameActivity extends AppScreen {
    private EditText mTextField;
    private PatientProfile mPatientProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, Disposition.Centered);

        setTitle(R.string.screen_admin_patient_info);

        mPatientProfile = new PatientProfile(Settings.getCurrentPatientProfile(this));

        mTextField = addInputMessagePrompt(R.string.instruction_patient_username,
                InputType.TYPE_CLASS_TEXT);
        mTextField.setText(mPatientProfile.username);

        // Set the footer buttons
        addFooterButton(R.string.action_back, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goBackToParentActivity(PatientIdActivity.class);
            }
        });

        addFooterButton(R.string.action_next, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPatientProfile.username = mTextField.getText().toString();

                // Check that the patient id is valid
                if (!mPatientProfile.isValid(PatientProfile.Stage.PatientUsername)) {
                    new CustomDialog.Builder(PatientUsernameActivity.this)
                            .setTitle(R.string.screen_admin_patient_info)
                            .addFooterButton(R.string.action_done, null)
                            .setMessage(R.string.alert_invalid_patient_username)
                            .show();
                } else {
                    Settings.updatePatientProfile(PatientUsernameActivity.this, mPatientProfile);
                    openChildActivity(RoomCountActivity.class);
                }
            }
        });
    }

}
