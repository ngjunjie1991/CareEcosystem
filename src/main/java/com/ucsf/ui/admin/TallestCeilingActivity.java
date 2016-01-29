package com.ucsf.ui.admin;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;

import com.ucsf.R;
import com.ucsf.core.data.Entry;
import com.ucsf.core_phone.ui.widgets.CustomDialog;
import com.ucsf.data.PatientProfile;
import com.ucsf.data.Settings;
import com.ucsf.ui.widgets.AppScreen;

/**
 * Activity for the user to set the tallest ceiling height of the house.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public class TallestCeilingActivity extends AppScreen {

    private PatientProfile mPatientProfile;
    private EditText mCeilingHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, Disposition.Centered);

        setTitle(R.string.screen_indoor_settings);

        mPatientProfile = new PatientProfile(Settings.getCurrentPatientProfile(this));
        mCeilingHeight = addInputMessagePrompt(R.string.instruction_tallest_ceiling,
                R.string.label_unit, InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if (mPatientProfile.tallestCeilingHeight > 0)
            mCeilingHeight.setText(String.valueOf(mPatientProfile.tallestCeilingHeight));

        // Set footer buttons
        addFooterButton(R.string.action_back, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goBackToParentActivity(RoomCountActivity.class);
            }
        });

        addFooterButton(R.string.action_next, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mPatientProfile.tallestCeilingHeight =
                            Double.valueOf(mCeilingHeight.getText().toString());

                    if (mPatientProfile.isValid(PatientProfile.Stage.TallestCeiling)) {
                        Settings.updatePatientProfile(TallestCeilingActivity.this, mPatientProfile);

                        openChildActivity(BeaconSetupInstructionActivity.class,
                                new Entry("room", 0));

                        return;
                    }
                } catch (Exception ignored) {
                }

                // Notify the user that he entered an invalid height
                new CustomDialog.Builder(TallestCeilingActivity.this)
                        .setTitle(R.string.screen_indoor_settings)
                        .addFooterButton(R.string.action_done, null)
                        .setMessage(R.string.alert_invalid_height)
                        .show();
            }
        });
    }

}
