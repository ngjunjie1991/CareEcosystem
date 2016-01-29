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
 * Activity to set the number of rooms/motes.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public class RoomCountActivity extends AppScreen {
    private EditText mRoomCount;
    private PatientProfile mPatientProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, Disposition.Centered);

        setTitle(R.string.screen_indoor_settings);

        mPatientProfile = new PatientProfile(Settings.getCurrentPatientProfile(this));
        mRoomCount = addInputMessagePrompt(R.string.instruction_room_count,
                InputType.TYPE_CLASS_NUMBER);
        if (mPatientProfile.rooms != null)
            mRoomCount.setText(String.valueOf(mPatientProfile.rooms.length));

        // Set footer buttons
        addFooterButton(R.string.action_back, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goBackToParentActivity(PatientUsernameActivity.class);
            }
        });

        addFooterButton(R.string.action_next, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mPatientProfile.setRoomCount(Integer.valueOf(mRoomCount.getText().toString()));

                    if (mPatientProfile.isValid(PatientProfile.Stage.RoomCount)) {
                        Settings.updatePatientProfile(RoomCountActivity.this, mPatientProfile);

                        openChildActivity(TallestCeilingActivity.class);

                        return;
                    }
                } catch (Exception ignored) {
                }

                // Notify the user that he entered a wrong number
                new CustomDialog.Builder(RoomCountActivity.this)
                        .setTitle(R.string.screen_indoor_settings)
                        .addFooterButton(R.string.action_done, null)
                        .setMessage(R.string.alert_wrong_room_count)
                        .show();

                // Restore previous settings to avoid deleting room information
                Settings.getCurrentPatientProfile(RoomCountActivity.this).copyTo(mPatientProfile);
            }
        });
    }

}
