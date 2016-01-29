package com.ucsf.ui.admin;

import android.content.Context;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.ucsf.R;
import com.ucsf.core.services.ResponseListener;
import com.ucsf.core.services.Services;
import com.ucsf.core_phone.services.ServerListener;
import com.ucsf.core_phone.services.ServerMessage;
import com.ucsf.core_phone.ui.Theme;
import com.ucsf.core_phone.ui.widgets.CustomDialog;
import com.ucsf.data.PatientProfile;
import com.ucsf.data.Settings;
import com.ucsf.services.ServerListenerService;
import com.ucsf.services.ServerUploaderService;
import com.ucsf.services.StartupService;
import com.ucsf.ui.tester.TesterMenuActivity;
import com.ucsf.ui.widgets.AppScreen;


/**
 * Activity sending config to the server and waiting for a confirmation.
 * The confirmation process follows four steps:
 * - The phone sends its internal database (the ground trust) to the server.
 * - In case of success, the patient's profile is also sent.
 * - Finally the phone waits for a server confirmation.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public class ServerConfirmationActivity extends AppScreen implements ServerListener {
    private static final String TAG = "ucsf:ServerConfirmation";
    private static final String KEY_STATUS = "status";
    private static final String KEY_EXTRAS = "extras";
    private static final String KEY_VALID_STATUS = "valid";
    private static final long SERVER_TIMEOUT = 600000; // 10 minutes

    private TextView mInstructionsLabel;
    private Button mButton;
    private ProgressBar mProgressBar;

    private PatientProfile mPatientProfile;
    private Handler mTimeoutHandler = null;
    private Runnable mTimeoutCallback;
    private PowerManager.WakeLock mWakeLock;

    @Override
    public void onStart() {
        super.onStart();
        startConfirmationProcess();
        lockScreen();
    }

    @Override
    public void onStop() {
        stopTimeoutCallback();
        ServerListenerService.getProvider(this).unregisterListener(this);
        unlockScreen();
        super.onStop();
    }

    private void lockScreen() {
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, TAG);
        mWakeLock.acquire();
    }

    private void unlockScreen() {
        mWakeLock.release();
    }

    private void startConfirmationProcess() {
        ServerListenerService.Provider provider = ServerListenerService.getProvider(this);
        provider.registerListener(ServerMessage.PatientProfileValidation, this);

        // Make sure that the ServerListener service is running
        if (!provider.isServiceRunning())
            provider.start(new ResponseListener() {
                @Override
                public void onSuccess() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!isFinishing()) {
                                sendGroundTrustData();
                            }
                        }
                    });
                }

                @Override
                public void onFailure(String error, Throwable e) {
                    Log.e(TAG, "Failed to start ServerListener service: " + error, e);
                }
            });
        else
            sendGroundTrustData();
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

        mPatientProfile = new PatientProfile(Settings.getCurrentPatientProfile(this));
        mPatientProfile.registered = false;

        mInstructionsLabel = (TextView) view.findViewById(R.id.instructions);
        mProgressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
        mButton = (Button) view.findViewById(R.id.button);
        mButton.setText(R.string.action_retry);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendGroundTrustData();
            }
        });

        mButton.setVisibility(View.INVISIBLE);
        mInstructionsLabel.setText(R.string.instruction_receiving_watch_data);
        mProgressBar.setVisibility(View.VISIBLE);

        // Set the footer buttons
        addFooterButton(R.string.action_back, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goBackToParentActivity(BeaconTestActivity.class);
            }
        });

        addFooterButton(R.string.action_next, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check that all the rooms have been checked
                if (!mPatientProfile.isValid(PatientProfile.Stage.Registration)) {
                    new CustomDialog.Builder(ServerConfirmationActivity.this)
                            .setTitle(R.string.screen_indoor_settings)
                            .addFooterButton(R.string.action_done, null)
                            .setMessage(R.string.toast_wait_server)
                            .show();
                } else {
                    Settings.updatePatientProfile(ServerConfirmationActivity.this, mPatientProfile);

                    // Start the data acquisition
                    try {
                        Services.enableServices(ServerConfirmationActivity.this, true);
                        StartupService.startServices(ServerConfirmationActivity.this);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to start services: ", e);
                    }

                    if (getViewTheme() == Theme.Caregiver)
                        goBackToParentActivity(AdminMenuActivity.class);
                    else
                        goBackToParentActivity(TesterMenuActivity.class);
                }
            }
        });
    }

    /**
     * First step: send the ground trust data to the server.
     */
    private void sendGroundTrustData() {
        mInstructionsLabel.setText(R.string.instruction_pushing_data);
        mButton.setVisibility(View.INVISIBLE);
        mProgressBar.setVisibility(View.VISIBLE);

        ServerUploaderService.getProvider(this).commit(new ResponseListener() {
            @Override
            public void onSuccess() {
                new Handler(getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // The ground trust data have been uploaded, we can now upload the patient's profile
                        mButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                sendPatientProfile();
                            }
                        });
                        sendPatientProfile();
                    }
                }, 2000); // Let some time to the server for processing the data (it's not a timeout!)
            }

            @Override
            public void onFailure(final String error, final Throwable e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.e(TAG, "Failed to send database content to the server: " + error, e);
                        if (!isFinishing()) {
                            mInstructionsLabel.setText(R.string.instruction_server_push_failed);
                            mProgressBar.setVisibility(View.INVISIBLE);
                            mButton.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }
        });
    }

    /**
     * Second step: push the patient's profile to the server.
     */
    private void sendPatientProfile() {
        final PatientProfile profile = Settings.getCurrentPatientProfile(this);

        mButton.setVisibility(View.INVISIBLE);
        mInstructionsLabel.setText(R.string.instruction_send_config);
        mProgressBar.setVisibility(View.VISIBLE);

        ResponseListener handler = new ResponseListener() {
            @Override
            public void onSuccess() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, String.format("Patient profile '%s' successfully sent to the server.",
                                profile.patientId));

                        if (!isFinishing()) {
                            mInstructionsLabel.setText(R.string.instruction_wait_server);
                            mProgressBar.setVisibility(View.VISIBLE);
                            mButton.setVisibility(View.INVISIBLE);
                            mButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    retry();
                                }
                            });

                            startTimeoutCallback(new Runnable() {
                                @Override
                                public void run() {
                                    mInstructionsLabel.setText(R.string.instruction_no_server_response);
                                    mProgressBar.setVisibility(View.INVISIBLE);
                                    mButton.setVisibility(View.VISIBLE);
                                    mTimeoutHandler = null;
                                }
                            });
                        }
                    }
                });
            }

            private void retry() {
                ServerUploaderService.getProvider(ServerConfirmationActivity.this)
                        .sendProfile(profile, this);
            }

            @Override
            public void onFailure(final String error, final Throwable e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.e(TAG, String.format("Failed to send patient profile '%s' to the server: %s",
                                profile.patientId, error), e);

                        if (!isFinishing()) {
                            mInstructionsLabel.setText(R.string.instruction_check_connection);
                            mProgressBar.setVisibility(View.INVISIBLE);
                            mButton.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }
        };

        ServerUploaderService.getProvider(this).sendProfile(profile, handler);
    }

    private void startTimeoutCallback(Runnable callback) {
        mTimeoutCallback = callback;
        mTimeoutHandler = new Handler(getMainLooper());
        mTimeoutHandler.postDelayed(mTimeoutCallback, SERVER_TIMEOUT);
    }

    private void stopTimeoutCallback() {
        if (mTimeoutHandler != null) {
            mTimeoutHandler.removeCallbacks(mTimeoutCallback);
            mTimeoutHandler = null;
        }
    }

    @Override
    public void onServerMessage(ServerMessage message, final Bundle data) {
        stopTimeoutCallback();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing()) {
                    if (data.get(KEY_STATUS).equals(KEY_VALID_STATUS)) {
                        mInstructionsLabel.setText(R.string.instruction_server_response_received);
                        mPatientProfile.registered = true;
                        mButton.setVisibility(View.INVISIBLE);
                    } else {
                        String error = data.getString(KEY_EXTRAS);
                        mInstructionsLabel.setText(String.format(
                                getString(R.string.instruction_invalid_patient_profile), error));
                        mButton.setVisibility(View.VISIBLE);
                    }

                    mProgressBar.setVisibility(View.INVISIBLE);
                }
            }
        });
    }
}
