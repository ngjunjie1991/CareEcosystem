package com.ucsf.ui.admin;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.ucsf.R;
import com.ucsf.core.data.Entry;
import com.ucsf.core_phone.ui.widgets.CustomDialog;
import com.ucsf.data.PatientProfile;
import com.ucsf.data.PatientProfile.Room;
import com.ucsf.data.Settings;
import com.ucsf.ui.widgets.AppScreen;

import java.io.Serializable;

/**
 * Activity to enter the parameters of the room.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public class BeaconSetupActivity extends AppScreen {
    private PatientProfile mPatientProfile;
    private boolean        mIsFirstRoom;
    private Room           mCurrentRoom;
    private EditText       mBeaconIdField;
    private EditText       mRoomNameField;
    private EditText       mCeilingHeightField;
    private Spinner        mFloorField;
    private EditText       mXDistanceField;
    private EditText       mYDistanceField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, Disposition.Centered);

        setTitle(R.string.screen_indoor_settings);

        final int room = getIntent().getIntExtra("room", 0);
        int backgroundColor = getViewTheme().getBackgroundColor(this);
        mPatientProfile = new PatientProfile(Settings.getCurrentPatientProfile(this));
        mIsFirstRoom = room == 0;
        mCurrentRoom = mPatientProfile.rooms[room];


        LayoutInflater inflater = (LayoutInflater)
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View centralView = inflater.inflate(R.layout.beacon_setup_layout, getCentralLayout(), false);
        centralView.getBackground().setColorFilter(backgroundColor, PorterDuff.Mode.SRC_ATOP);

        addView(centralView);

        mBeaconIdField = (EditText) centralView.findViewById(R.id.beaconIdEditText);
        mRoomNameField = (EditText) centralView.findViewById(R.id.roomNameEditText);
        mCeilingHeightField = (EditText) centralView.findViewById(R.id.ceilingEditText);
        mFloorField = (Spinner) centralView.findViewById(R.id.floorSpinner);
        mXDistanceField = (EditText) centralView.findViewById(R.id.northDistanceEditText);
        mYDistanceField = (EditText) centralView.findViewById(R.id.eastDistanceEditText);

        mFloorField.setAdapter(new Adapter(this, R.layout.label_cell, new String[]{
                getString(R.string.label_basement),
                getString(R.string.label_1st_floor),
                getString(R.string.label_2nd_floor),
                getString(R.string.label_3rd_floor),
                getString(R.string.label_4th_floor),
        }));

        // Initialize the fields
        initField(mBeaconIdField, mBeaconIdField, mCurrentRoom.getMoteId(), backgroundColor);
        initField(mRoomNameField, mRoomNameField, mCurrentRoom.getRoomName(), backgroundColor);
        initField(mCeilingHeightField, centralView.findViewById(R.id.ceilingLayout),
                mCurrentRoom.getHeight(), backgroundColor);
        initField(mXDistanceField, centralView.findViewById(R.id.northDistanceLayout),
                mCurrentRoom.getXDistanceFromPrevious(), backgroundColor);
        initField(mYDistanceField, centralView.findViewById(R.id.eastDistanceLayout),
                mCurrentRoom.getYDistanceFromPrevious(), backgroundColor);
        mFloorField.setSelection(mCurrentRoom.getFloor() + 1);
        mFloorField.getBackground().setColorFilter(backgroundColor, PorterDuff.Mode.SCREEN);

        if (mIsFirstRoom) {
            findViewById(R.id.northDistanceRow).setVisibility(View.GONE);
            findViewById(R.id.eastDistanceRow).setVisibility(View.GONE);
        }

        // Set the helpers listeners
        centralView.findViewById(R.id.beaconIdButton).setOnClickListener(
                new HelpListener(R.string.label_beacon_id, R.string.help_beacon_id));
        centralView.findViewById(R.id.roomNameButton).setOnClickListener(
                new HelpListener(R.string.label_room_name, R.string.help_room_name));
        centralView.findViewById(R.id.ceilingButton).setOnClickListener(
                new HelpListener(R.string.label_ceiling_height, R.string.help_ceiling_height));
        centralView.findViewById(R.id.floorButton).setOnClickListener(
                new HelpListener(R.string.label_floor, R.string.help_floor));

        SketchHelpListener beaconDistanceListener = new SketchHelpListener(
                R.string.label_beacon_distance,
                R.string.help_beacon_distance,
                R.drawable.home_sketch);
        centralView.findViewById(R.id.northDistanceButton).setOnClickListener(beaconDistanceListener);
        centralView.findViewById(R.id.eastDistanceButton).setOnClickListener(beaconDistanceListener);

        // Set footer buttons
        addFooterButton(R.string.action_back, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goBackToParentActivity(BeaconSetupInstructionActivity.class,
                        new Entry("room", room));
            }
        });

        addFooterButton(R.string.action_next, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!updateCurrentRoom()) {
                    new CustomDialog.Builder(BeaconSetupActivity.this)
                            .setTitle(R.string.screen_indoor_settings)
                            .setMessage(R.string.alert_invalid_room_parameters)
                            .addFooterButton(R.string.action_done, null)
                            .show();
                    return;
                }

                Settings.updatePatientProfile(BeaconSetupActivity.this, mPatientProfile);

                openChildActivity(BeaconSetupInstructionActivity.class,
                        new Entry("room", room + 1));
            }
        });
    }

    private void initField(EditText field, View background, Serializable value,
                           int backgroundColor) {
        field.setText(value.toString());
        background.getBackground().setColorFilter(backgroundColor, PorterDuff.Mode.SCREEN);
    }

    /**
     * Update the current room with the content of the fields and returns if the room is valid.
     */
    private boolean updateCurrentRoom() {
        String beaconId = mBeaconIdField.getText().toString();
        try {
            mCurrentRoom.setMoteId(beaconId);
        } catch (NumberFormatException e) {
            return false;
        }

        String roomName = mRoomNameField.getText().toString();
        if (roomName.isEmpty())
            return false;
        mCurrentRoom.setRoomName(roomName);

        String ceilingHeight = mCeilingHeightField.getText().toString();
        try {
            mCurrentRoom.setHeight(Double.valueOf(ceilingHeight));
        } catch (NumberFormatException e) {
            return false;
        }

        mCurrentRoom.setFloor(mFloorField.getSelectedItemPosition() - 1);

        if (!mIsFirstRoom) {
            String xDistance = mXDistanceField.getText().toString();
            try {
                mCurrentRoom.setXDistanceFromPrevious(Double.valueOf(xDistance));
            } catch (NumberFormatException e) {
                return false;
            }

            String yDistance = mYDistanceField.getText().toString();
            try {
                mCurrentRoom.setYDistanceFromPrevious(Double.valueOf(yDistance));
            } catch (NumberFormatException e) {
                return false;
            }
        } else {
            mCurrentRoom.setXDistanceFromPrevious(0);
            mCurrentRoom.setYDistanceFromPrevious(0);
        }

        return PatientProfile.isRoomValid(mCurrentRoom, mPatientProfile.rooms) ==
                PatientProfile.RoomStatus.Valid;
    }

    private class HelpListener implements View.OnClickListener {
        protected final String mTitle;
        protected final String mText;

        public HelpListener(int titleId, int textId) {
            mTitle = String.format(getString(R.string.label_information), getString(titleId));
            mText = getString(textId);
        }

        @Override
        public void onClick(View view) {
            new CustomDialog.Builder(BeaconSetupActivity.this)
                    .setTitle(mTitle)
                    .setMessage(mText)
                    .addFooterButton(R.string.action_done, null)
                    .show();
        }
    }

    private class SketchHelpListener extends HelpListener {
        private final Drawable mSketch;

        public SketchHelpListener(int titleId, int textId, int sketch) {
            super(titleId, textId);
            mSketch = getResources().getDrawable(sketch);
        }

        @Override
        public void onClick(View view) {
            LayoutInflater inflater = (LayoutInflater)
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View internalView =
                    inflater.inflate(R.layout.sketch_layout, getCentralLayout(), false);
            ((TextView) internalView.findViewById(R.id.label)).setText(mText);
            ((ImageView) internalView.findViewById(R.id.sketch)).setImageDrawable(mSketch);

            new CustomDialog.Builder(BeaconSetupActivity.this)
                    .setTitle(mTitle)
                    .setView(internalView)
                    .addFooterButton(R.string.action_done, null)
                    .show();
        }
    }

    private class Adapter extends ArrayAdapter<String> {
        public Adapter(Context context, int resource, String[] objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            view.setBackground(null);
            ((TextView) view).setTextColor(getResources().getColor(R.color.edit_text_color));
            return view;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            view.setBackgroundColor(getViewTheme().getBackgroundColor(BeaconSetupActivity.this));
            return view;
        }
    }
}
