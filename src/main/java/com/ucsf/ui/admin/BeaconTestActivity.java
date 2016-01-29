package com.ucsf.ui.admin;

import android.content.Context;
import android.graphics.PorterDuff;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.util.Range;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.ucsf.R;
import com.ucsf.core.data.DataManager;
import com.ucsf.core.data.Entry;
import com.ucsf.core.data.RSSI;
import com.ucsf.core.data.SharedTables;
import com.ucsf.core.data.Timestamp;
import com.ucsf.core.services.Messages;
import com.ucsf.core.services.ServiceId;
import com.ucsf.core_phone.ui.widgets.CustomDialog;
import com.ucsf.data.PatientProfile;
import com.ucsf.data.PatientProfile.Room;
import com.ucsf.data.Settings;
import com.ucsf.services.DeviceInterface;
import com.ucsf.services.GroundTrust;
import com.ucsf.ui.widgets.AppScreen;

import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.crypto.NullCipher;

/**
 * Activity to test each beacons. The admin is prompt to walk for a certain among of time
 * in each room.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public class BeaconTestActivity extends AppScreen implements GroundTrust.Listener,
        com.ucsf.core.services.DeviceInterface.RequestListener
{
    private static final String TAG = "ucsf:BeaconTestActivity";
    private static final long TASK_DURATION = 180000; // 2 minutes
    private static final double RSSI_THRESHOLD = -200;
    private final GroundTrust mGroundTrust = new GroundTrust(GroundTrust.Type.CurrentRoom);
    private PatientProfile mPatientProfile;
    private int mCurrentRoomIdx;
    private Room mCurrentRoom;
    private int mCurrentIter;
    private int mMaxProgress;
    private PowerManager.WakeLock mWakeLock;
    private Range<String> mAcquisitionRange;
    private LocationListener mGpsListener;

    private Handler mHandler;
    private TextView mInstructionsLabel;
    private Button mButton;
    private ProgressBar mProgressBar;
    private ProgressBar mGlobalProgressBar;
    private TextView mProgressLabel;
    private View mProgressLayout;
    private String mStartRoomTestFormat;
    private String mTransitionInstructionFormat;
    private String mDataAcquisitionInstructionsFormat;
    // This listener is called when the user finishes a room test and clicks on the button 'Done'
    // It is responsible to start the next test or to notify the user that tests are now finished.
    private final View.OnClickListener mOnDoneListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ++mCurrentRoomIdx;

            // If all the rooms have been checked, start again or notify the user
            if (mCurrentRoomIdx == mPatientProfile.rooms.length) {
                if (++mCurrentIter == PatientProfile.VALIDATION_STEP_COUNT) {
                    mPatientProfile.setupEndTimestamp = Timestamp.getTimestamp();
                    updateView();
                } else {
                    mCurrentRoomIdx = 0;
                    mCurrentRoom = mPatientProfile.rooms[mCurrentRoomIdx];
                    displayTransitionMessage();
                }
            } else { // Continue
                mCurrentRoom = mPatientProfile.rooms[mCurrentRoomIdx];
                displayTransitionMessage();
            }
        }
    };
    private String mDataCollectedInstructionsFormat;

    @Override
    protected void onStart() {
        super.onStart();
        DeviceInterface.sendPatientInfo(this);
        DeviceInterface.toggleWatchServices(this, true);
        if (mPatientProfile.validationStep != mMaxProgress)
            mGroundTrust.init(getViewContext(), this);
        updateGPSLocation();
        lockScreen();
    }

    @Override
    protected void onStop() {
        unlockScreen();
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateGPSLocation();
        lockScreen();
    }

    private void lockScreen() {
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, TAG);
        mWakeLock.acquire();
    }

    private void unlockScreen() {
        mWakeLock.release();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, Disposition.Centered);

        setTitle(R.string.screen_indoor_settings);

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.progress_message_prompt, getCentralLayout(), false);
        addView(view);

        ((ImageView) findViewById(R.id.background)).getDrawable()
                .setColorFilter(getViewTheme().getBackgroundColor(this), PorterDuff.Mode.SRC_ATOP);

        mHandler = new Handler();
        mPatientProfile = new PatientProfile(Settings.getCurrentPatientProfile(this));
        mCurrentRoomIdx = mPatientProfile.validationStep % mPatientProfile.rooms.length;
        mCurrentIter = mPatientProfile.validationStep / mPatientProfile.rooms.length;
        mCurrentRoom = mPatientProfile.rooms[mCurrentRoomIdx];

        mInstructionsLabel = (TextView) view.findViewById(R.id.instructions);
        mButton = (Button) view.findViewById(R.id.button);
        mProgressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
        mGlobalProgressBar = (ProgressBar) view.findViewById(R.id.globalProgressBar);
        mProgressLabel = (TextView) view.findViewById(R.id.progressLabel);
        mProgressLayout = findViewById(R.id.globalProgressLayout);

        mMaxProgress = PatientProfile.VALIDATION_STEP_COUNT * mPatientProfile.rooms.length;
        mGlobalProgressBar.setMax(mMaxProgress);

        mStartRoomTestFormat = getString(R.string.action_start_room_test);
        mTransitionInstructionFormat = getString(R.string.instruction_test_transition);
        mDataAcquisitionInstructionsFormat = getString(R.string.instruction_test_acquisition);
        mDataCollectedInstructionsFormat = getString(R.string.instruction_test_done);

        updateView();

        // Set the footer buttons
        addFooterButton(R.string.action_back, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mGroundTrust.release();
                goBackToParentActivity(BeaconSetupInstructionActivity.class,
                        new Entry("room", mPatientProfile.rooms.length));
            }
        });

        addFooterButton(R.string.action_next, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check that all the rooms have been checked
                if (!mPatientProfile.isHomeAcquired) {
                    new CustomDialog.Builder(BeaconTestActivity.this)
                            .setTitle(R.string.screen_indoor_settings)
                            .addFooterButton(R.string.action_done, null)
                            .setMessage(R.string.alert_wait_gps)
                            .show();
                } else if (!mPatientProfile.isValid(PatientProfile.Stage.MoteValidation)) {
                    new CustomDialog.Builder(BeaconTestActivity.this)
                            .setTitle(R.string.screen_indoor_settings)
                            .addFooterButton(R.string.action_done, null)
                            .setMessage(R.string.alert_unchecked_rooms)
                            .show();
                } else {
                    if (mPatientProfile.setupEndTimestamp.isEmpty())
                        mPatientProfile.setupEndTimestamp = Timestamp.getTimestamp();
                    Settings.updatePatientProfile(BeaconTestActivity.this, mPatientProfile);
                    mGroundTrust.release();
                    openChildActivity(ServerConfirmationActivity.class);
                }
            }
        });
    }

    private void updateView() {
        if (mPatientProfile.validationStep == mMaxProgress) { // The initialization is done
            mButton.setVisibility(View.INVISIBLE);
            mInstructionsLabel.setText(getString(R.string.instruction_test_all_done));
            mProgressLayout.setVisibility(View.INVISIBLE);
            mProgressBar.setVisibility(View.INVISIBLE);
        } else {
            mGlobalProgressBar.setProgress(mPatientProfile.validationStep);
            mProgressLabel.setText(String.format("%d/%d", mPatientProfile.validationStep, mMaxProgress));
            mProgressBar.setVisibility(View.VISIBLE);
            mButton.setVisibility(View.INVISIBLE);
            mInstructionsLabel.setText(R.string.instruction_test_connection);
            mPatientProfile.setupEndTimestamp = "";
        }
    }

    /**
     * Start the GPS location in background to get the home location. Do it once.
     */
    private void updateGPSLocation() {
        if (mPatientProfile.isHomeAcquired || mGpsListener != null)
            return;

        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mGpsListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                // Update the patient profile
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                if (latitude == 0 && longitude == 0) // The sensor can return an invalid coordinate
                    return;

                Log.d(TAG, String.format("New home location: [%f; %f]", latitude, longitude));

                mPatientProfile.homeLatitude = latitude;
                mPatientProfile.homeLongitude = longitude;
                mPatientProfile.isHomeAcquired = true;

                // Remove this gps listener
                mGpsListener = null;
                manager.removeUpdates(this);
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {
            }

            @Override
            public void onProviderEnabled(String s) {
            }

            @Override
            public void onProviderDisabled(String s) {
            }
        };
        manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mGpsListener);
    }

    /**
     * Displays the instruction for the next room.
     */
    private void displayTransitionMessage() {
        mProgressBar.setVisibility(View.GONE);
        mInstructionsLabel.setText(String.format(mTransitionInstructionFormat,
                mCurrentRoom.getRoomName()));

        mButton.setVisibility(View.VISIBLE);
        mButton.setText(String.format(mStartRoomTestFormat, mCurrentRoom.getRoomName()));
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRoomAcquisition();
            }
        });
    }

    /**
     * Displays a message during acquisition and starts the room acquisition.
     */
    private synchronized void startRoomAcquisition() {
        // Update the ui
        mProgressBar.setVisibility(View.VISIBLE);
        mInstructionsLabel.setText(String.format(mDataAcquisitionInstructionsFormat,
                mCurrentRoom.getRoomName()));
        mButton.setVisibility(View.INVISIBLE);

        // Start data acquisition
        if (mPatientProfile.validationStep == 0) // First scan
            mPatientProfile.setupStartTimestamp = Timestamp.getTimestamp();

        mGroundTrust.startAcquisition(mCurrentRoom.getRoomName());
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Stop the acquistion and request the estimote data from the watch
                mAcquisitionRange = mGroundTrust.stopAcquisition();
                requestWatchData();

                // Play a sound to notify the user
                try {
                    Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                    r.play();
                } catch (Exception e) {
                    Log.e(TAG, "Failed to play a notification sound: ", e);
                }
            }
        }, TASK_DURATION);
    }

    private void requestWatchData() {
        mButton.setVisibility(View.INVISIBLE);
        mInstructionsLabel.setText(R.string.instruction_receiving_watch_data);
        mProgressBar.setVisibility(View.VISIBLE);

        DeviceInterface.requestServiceCallbackExecution(this, ServiceId.PW_PhoneUploaderService,
                "PUSH_DATA", this);
    }

    @Override
    public void onMonitoringStarted() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mProgressLayout.setVisibility(View.VISIBLE);
                displayTransitionMessage();
            }
        });
    }

    @Override
    public void onMonitoringFailedToStart(String error) {
        Log.e(TAG, "Failed to start ground trust acquisition: " + error);

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mProgressBar.setVisibility(View.GONE);
                mInstructionsLabel.setText(R.string.instruction_test_error);
                mButton.setVisibility(View.VISIBLE);
                mButton.setText(R.string.action_retry);
                mButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        updateView();
                        DeviceInterface.sendPatientInfo(BeaconTestActivity.this);
                        DeviceInterface.toggleWatchServices(BeaconTestActivity.this, true);
                        mGroundTrust.init(getViewContext(), BeaconTestActivity.this);
                    }
                });
            }
        });
    }

    @Override
    public void requestProcessed(Messages.Request request, JSONObject data) throws Exception {
        // The data have been uploaded to the phone, we can now push them to the server
        new Handler(getMainLooper()).postDelayed( new Runnable() {
            @Override
            public void run() {
                if (!isFinishing()) {
                    // Verify the validity of the data
                    List<Double> values = new LinkedList<>();
                    double median = RSSI.DEFAULT_RSSI;
                    try (DataManager instance = DataManager.get(BeaconTestActivity.this)) {
                        DataManager.Cursor cursor = SharedTables.Estimote.getTable(instance).fetch(
                                new String[]{SharedTables.Estimote.KEY_RSSI},
                                new DataManager.Condition.GreaterEqual<>(DataManager.KEY_TIMESTAMP,
                                        mAcquisitionRange.getLower()),
                                new DataManager.Condition.LessEqual<>(DataManager.KEY_TIMESTAMP,
                                        mAcquisitionRange.getUpper()));
                        if (cursor != null && cursor.moveToFirst()) {
                            do {
                                RSSI rssi = new RSSI((HashMap<String, Double>)
                                        cursor.getSerializable(SharedTables.Estimote.KEY_RSSI));
                                values.add(rssi.get(mCurrentRoom));
                            } while (cursor.moveToNext());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to validate room data: ", e);
                    }

                    if (!values.isEmpty()) {
                        Collections.sort(values);
                        median = values.get(values.size() / 2);
                    }

                    Log.d(TAG, String.format("Median room RSSI value: %f (%d entries)", median, values.size()));
                    if (median < RSSI_THRESHOLD) { // The beacon is not functioning properly
                        // Remove the entries
                        try (DataManager instance = DataManager.get(BeaconTestActivity.this)) {
                            SharedTables.GroundTrust.getTable(instance).erase(
                                    new DataManager.Condition.Equal<>("start",
                                            mAcquisitionRange.getLower()));
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to invalidate room data: ", e);
                        }

                        mProgressBar.setVisibility(View.GONE);
                        mInstructionsLabel.setText(R.string.instruction_invalid_room_data);
                        mButton.setText(R.string.action_retry);
                        mButton.setVisibility(View.VISIBLE);
                        mButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                startRoomAcquisition();
                            }
                        });
                    } else { // Go to the next room
                        mProgressBar.setVisibility(View.GONE);
                        mInstructionsLabel.setText(String.format(
                                mDataCollectedInstructionsFormat, mCurrentRoom.getRoomName()));
                        mButton.setVisibility(View.VISIBLE);
                        mButton.setText(R.string.action_done);
                        mButton.setOnClickListener(mOnDoneListener);
                        mGlobalProgressBar.setProgress(++mPatientProfile.validationStep);
                        mProgressLabel.setText(String.format("%d/%d", mPatientProfile.validationStep,
                                mMaxProgress));

                        // Save the progression
                        Settings.updatePatientProfile(BeaconTestActivity.this, mPatientProfile);
                    }
                }
            }
        }, 10000L); // Makes sure that the data are uploaded
    }

    @Override
    public void requestTimeout(Messages.Request request) throws Exception {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mInstructionsLabel.setText(R.string.instruction_no_watch_response);
                mProgressBar.setVisibility(View.INVISIBLE);
                mButton.setText(R.string.action_retry);
                mButton.setVisibility(View.VISIBLE);
                mButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        requestWatchData();
                    }
                });
            }
        });
    }

    @Override
    public void requestCancelled(Messages.Request request) throws Exception {
        requestTimeout(request);
    }
}
