package com.ucsf.ui.admin;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.ucsf.R;
import com.ucsf.core.data.DeviceLocation;
import com.ucsf.core.data.Timestamp;
import com.ucsf.core_phone.services.ServerRequestListener;
import com.ucsf.core_phone.ui.Theme;
import com.ucsf.core_phone.ui.widgets.CustomDialog;
import com.ucsf.data.PatientProfile;
import com.ucsf.data.Settings;
import com.ucsf.services.DeviceInterface;
import com.ucsf.services.ServerUploaderService;
import com.ucsf.ui.tester.TesterMenuActivity;
import com.ucsf.ui.widgets.AppScreen;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;

/**
 * Activity to change the patient ID.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public class PatientIdActivity extends AppScreen {
    private static final String TAG = "ucsf:PatientIdActivity";

    private EditText mTextField;

    private static PatientProfile getPatientProfile(Context context, String patientId) {
        // Get the patient profile
        PatientProfile profile = Settings.getPatientProfile(context, patientId);
        if (profile == null) {
            profile = new PatientProfile(patientId);
            PatientProfile currentProfile = Settings.getCurrentPatientProfile(context);
            if (currentProfile != null)
                currentProfile.copyTo(profile);
        }
        return profile;
    }

    private static PatientProfile getPatientProfile(Context context, String patientId,
                                                    @NonNull Bundle data) {
        // Get the patient profile
        PatientProfile profile = new PatientProfile(getPatientProfile(context, patientId));

        // Copy the remote profile if available
        String patientDataDesc = data.getString(PatientProfile.KEY_PROFILE); // This is not automatically parsed by Android...
        if (patientDataDesc != null && !patientDataDesc.isEmpty()) {
            try {
                JSONObject patientData = new JSONObject(patientDataDesc);
                JSONArray rooms = patientData.getJSONArray(PatientProfile.KEY_ROOMS);
                profile.rooms = new PatientProfile.Room[rooms.length()];
                for (int i = 0; i < profile.rooms.length; ++i) {
                    JSONObject roomData = rooms.getJSONObject(i);
                    int roomIdx = roomData.getInt(PatientProfile.KEY_ROOM_IDX);
                    PatientProfile.Room room = profile.rooms[roomIdx] = profile.new Room();

                    room.setRoomName(roomData.getString(PatientProfile.KEY_ROOM_NAME));
                    room.setMoteId(roomData.getString(PatientProfile.KEY_MOTE_ID));
                    room.setHeight(roomData.getDouble(PatientProfile.KEY_CEILING_HEIGHT));
                    room.setFloor(roomData.getInt(PatientProfile.KEY_FLOOR));
                    room.setXDistanceFromPrevious(roomData.getDouble(PatientProfile.KEY_X_DIST_FROM_PREV));
                    room.setYDistanceFromPrevious(roomData.getDouble(PatientProfile.KEY_Y_DIST_FROM_PREV));
                }

                if (profile.rooms.length == 0) { // Something went wrong
                    Log.e(TAG, "Failed to parse patient rooms data!");
                    return getPatientProfile(context, patientId);
                }

                profile.username = patientData.getString(PatientProfile.KEY_USERNAME);
                profile.tallestCeilingHeight = patientData.getInt(PatientProfile.KEY_TALLEST_CEILING);
                profile.homeLatitude = patientData.getDouble(PatientProfile.KEY_HOME_LATITUDE);
                profile.homeLongitude = patientData.getDouble(PatientProfile.KEY_HOME_LONGITUDE);
                profile.setupStartTimestamp = parseTimestamp(patientData.getString(PatientProfile.KEY_START_TIMESTAMP));
                profile.setupEndTimestamp = parseTimestamp(patientData.getString(PatientProfile.KEY_END_TIMESTAMP));
                profile.isHomeAcquired = true;
                profile.validationStep = PatientProfile.VALIDATION_STEP_COUNT * profile.rooms.length;
                profile.registered = false; // Just in case, still need a validation from the server
            } catch (Exception e) {
                Log.e(TAG, "Failed to parse patient data: ", e);
                return getPatientProfile(context, patientId);
            }
        }

        return profile;
    }

    private static String parseTimestamp(final String timestamp) {
        Calendar calendar =
                Timestamp.getCalendarFromTimestamp(timestamp, Timestamp.Format.YY_MM_DDTHH_MM_SS_MS);
        return Timestamp.getTimestampFromCalendar(calendar);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, Disposition.Centered);

        setTitle(R.string.screen_admin_patient_info);

        mTextField = addInputMessagePrompt(R.string.instruction_patient_id,
                InputType.TYPE_CLASS_TEXT);
        mTextField.setText(Settings.getCurrentUserId(this));

        // Set the footer buttons
        addFooterButton(R.string.action_back, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getViewTheme() == Theme.Caregiver)
                    goBackToParentActivity(AdminMenuActivity.class);
                else
                    goBackToParentActivity(TesterMenuActivity.class);
            }
        });

        addFooterButton(R.string.action_next, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String patientId = mTextField.getText().toString();
                PatientProfile patientProfile = getPatientProfile(PatientIdActivity.this, patientId);

                // Check that the patient id is valid
                if (!patientProfile.isValid(PatientProfile.Stage.PatientId)) {
                    new CustomDialog.Builder(PatientIdActivity.this)
                            .setTitle(R.string.screen_admin_patient_info)
                            .addFooterButton(R.string.action_done, null)
                            .setMessage(R.string.alert_invalid_patient_id)
                            .show();
                } else
                    requestPatientValidation(patientId);
            }
        });
    }

    /**
     * Sends a request to the server to know if the patient id is valid.
     */
    private void requestPatientValidation(final String patientId) {
        final CustomDialog waitDialog = new CustomDialog.Builder(this)
                .setTitle(R.string.screen_admin_patient_info)
                .setCancelable(false)
                .setMessage(R.string.instruction_wait_send_data)
                .create();

        ServerUploaderService.getProvider(this).requestPatientAuthorization(
                getCaregiverId(),
                patientId,
                new ServerRequestListener() {
                    @Override
                    public void onDeliveryStart() {
                        waitDialog.show();
                    }

                    @Override
                    public void onDeliveryFailure(final String error, final Throwable e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.e(TAG, "Failed to request patient id validation: " + error, e);
                                if (!isFinishing()) {
                                    waitDialog.dismiss();
                                    new CustomDialog.Builder(PatientIdActivity.this)
                                            .setTitle(R.string.screen_admin_patient_info)
                                            .setMessage(R.string.error_server_upload_failed)
                                            .addFooterButton(R.string.action_done, null)
                                            .show();
                                }
                            }
                        });
                    }

                    @Override
                    public void onTimeout() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (!isFinishing()) {
                                    waitDialog.dismiss();
                                    new CustomDialog.Builder(PatientIdActivity.this)
                                            .setTitle(R.string.screen_admin_patient_info)
                                            .setMessage(R.string.error_no_server_response)
                                            .addFooterButton(R.string.action_done, null)
                                            .show();
                                }
                            }
                        });
                    }

                    @Override
                    public void onResponseReceived(final Bundle data) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (!isFinishing()) {
                                    waitDialog.dismiss();
                                    String status = data.getString("status");
                                    if (status.equals("valid")) {
                                        PatientProfile patientProfile = getPatientProfile(PatientIdActivity.this, patientId, data);

                                        Settings.updatePatientProfile(PatientIdActivity.this, patientProfile);
                                        Settings.setCurrentUserId(PatientIdActivity.this, patientProfile.patientId);

                                        DeviceInterface.sendPatientInfo(PatientIdActivity.this);

                                        openChildActivity(PatientUsernameActivity.class);
                                    } else {
                                        String error = data.getString("extras");
                                        if (error == null || error.isEmpty())
                                            error = getString(R.string.alert_invalid_patient_id);

                                        new CustomDialog.Builder(PatientIdActivity.this)
                                                .setTitle(R.string.screen_admin_patient_info)
                                                .setMessage(error)
                                                .addFooterButton(R.string.action_done, null)
                                                .show();
                                    }
                                }
                            }
                        });
                    }
                }, DeviceLocation.PatientPhone);
    }
}
