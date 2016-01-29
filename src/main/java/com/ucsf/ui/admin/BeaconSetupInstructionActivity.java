package com.ucsf.ui.admin;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.ucsf.R;
import com.ucsf.core.data.Entry;
import com.ucsf.core_phone.ui.widgets.CustomDialog;
import com.ucsf.data.PatientProfile;
import com.ucsf.data.Settings;
import com.ucsf.ui.widgets.AppScreen;

/**
 * Activity displaying the instructions to setup the beacons.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public class BeaconSetupInstructionActivity extends AppScreen {

    private PatientProfile mPatientProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, Disposition.Centered);

        setTitle(R.string.screen_indoor_settings);

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.interactive_message_prompt, getCentralLayout(), false);
        addView(view);

        ((ImageView) findViewById(R.id.background)).getDrawable()
                .setColorFilter(getViewTheme().getBackgroundColor(this), PorterDuff.Mode.SRC_ATOP);

        final int room = getIntent().getIntExtra("room", 0);
        mPatientProfile = Settings.getCurrentPatientProfile(this);

        TextView instructionsLabel = (TextView) view.findViewById(R.id.instructions);
        Button button = (Button) view.findViewById(R.id.button);

        if (room == mPatientProfile.rooms.length) {
            instructionsLabel.setText(R.string.instruction_test_introduction);
            button.setText(R.string.action_new_acquisition);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new CustomDialog.Builder(BeaconSetupInstructionActivity.this)
                            .setTitle(R.string.action_new_acquisition)
                            .setMessage(R.string.alert_new_acquisition)
                            .addFooterButton(R.string.action_no, null)
                            .addFooterButton(R.string.action_yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    openChildActivity(BeaconSetupInstructionActivity.class, new Entry("room", 0));
                                }
                            })
                            .show();
                }
            });
            button.setVisibility(mPatientProfile.validationStep ==
                    room * PatientProfile.VALIDATION_STEP_COUNT ? View.VISIBLE : View.GONE);
        } else {
            instructionsLabel.setText(String.format("%s\n\n%s",
                    getString(room == 0 ? R.string.instruction_first_beacon_installation :
                            R.string.instruction_other_beacon_installation),
                    getString(R.string.instruction_beacon_number_remainder)));
            button.setVisibility(View.GONE);
        }

        // Set footer buttons
        addFooterButton(R.string.action_back, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (room == 0)
                    goBackToParentActivity(TallestCeilingActivity.class);
                else
                    goBackToParentActivity(BeaconSetupActivity.class,
                            new Entry("room", room - 1));
            }
        });

        addFooterButton(R.string.action_next, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (room == mPatientProfile.rooms.length) {
                    openChildActivity(BeaconTestActivity.class);
                } else
                    openChildActivity(BeaconSetupActivity.class,
                            new Entry("room", room));
            }
        });
    }

}
