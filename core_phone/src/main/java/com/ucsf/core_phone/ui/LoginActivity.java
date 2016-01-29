package com.ucsf.core_phone.ui;


import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.ucsf.core.data.DeviceLocation;
import com.ucsf.core.data.Entry;
import com.ucsf.core.data.Settings;
import com.ucsf.core_phone.R;
import com.ucsf.core_phone.services.ServerRequestListener;
import com.ucsf.core_phone.services.ServerUploaderService;
import com.ucsf.core_phone.ui.widgets.AppScreen;
import com.ucsf.core_phone.ui.widgets.CustomDialog;
import com.ucsf.core_phone.ui.widgets.TextFields;

/**
 * Activity to login into the app.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public abstract class LoginActivity extends AppScreen {
    private static final String         TAG = "ucsf:LoginActivity";
    private        final boolean        mIsMandatory;
    private        final boolean        mUseUserIdAsDefault;
    private              EditText       mIdField;
    private              EditText       mPasswordField;
    private              DeviceLocation mDevice;

    protected LoginActivity(boolean isMandatory, boolean useUserIdAsDefault,
                            DeviceLocation device)
    {
        mIsMandatory        = isMandatory;
        mUseUserIdAsDefault = useUserIdAsDefault;
        mDevice             = device;
    }

    /** Returns the class of the parent activity, i.e. the activity that created this one. */
    public abstract Class<? extends Activity> getParentActivityClass();

    /** Returns the activity class that will be opened if this activity is properly completed. */
    public abstract Class<? extends Activity> getOnSuccessActivityClass();

    /**
     * Executes some action before going to the OnSuccessActivity and returns a list of entries to
     * pass as arguments to the child activity.
     */
    protected abstract Entry[] onLogin(Bundle extras);

    /**
     * Returns the {@link ServerUploaderService service} class responsible to send data to
     * the server.
     */
    protected abstract ServerUploaderService.Provider getServerUploaderServiceProvider();

    /**
     * Returns the {@link NewProfileActivity activity} class responsible of creating a new
     * user profile.
     */
    protected abstract Class<? extends NewProfileActivity> getNewProfileActivityClass();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent intent = super.onCreate(savedInstanceState, Disposition.Centered, Theme.Caregiver);

        // Check if come back from the new patient activity
        if (intent.getBooleanExtra("LOGIN", false)) {
            final Bundle data = intent.getBundleExtra("DATA");
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Entry[] entries = onLogin(data);
                    goBackToParentActivity(getOnSuccessActivityClass(), entries);
                }
            }, 200);
        }

        setTitle(R.string.screen_login);

        TextFields fields = new TextFields(this);
        mIdField = fields.addTextField(R.string.label_caregiver_id,
                InputType.TYPE_CLASS_TEXT);
        mPasswordField = fields.addTextField(R.string.label_password,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        addView(fields);

        if (Settings.getCurrentUserId(this).isEmpty())
            addTextLink(R.string.label_no_account, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    requestNewAccount();
                }
            });
        else if (mUseUserIdAsDefault) {
            mIdField.setText(Settings.getCurrentUserId(this));
            mIdField.setEnabled(false);
        }

        addTextLink(R.string.label_forgotten_password, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestForgottenPassword();
            }
        });

        if (!mIsMandatory) {
            addFooterButton(R.string.action_cancel, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    goBackToParentActivity(getParentActivityClass());
                }
            });
        }

        addFooterButton(R.string.action_done, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String id = mIdField.getText().toString();
                String password = mPasswordField.getText().toString();

                if (id.isEmpty() || password.isEmpty()) {
                    new CustomDialog.Builder(LoginActivity.this)
                            .setTitle(R.string.screen_login)
                            .setMessage(R.string.error_wrong_id_or_password)
                            .addFooterButton(R.string.action_done, null)
                            .show();
                } else
                    requestAccountConfirmation(id, password);
            }
        });
    }

    /**
     * Sends a request to the server to know if the given pair caregiver/password is valid and if
     * the given caregiver is allowed to monitor the given patient.
     */
    protected void requestAccountConfirmation(final String caregiverId, final String patientId,
                                              final String password) {
        final CustomDialog waitDialog = new CustomDialog.Builder(this)
                .setTitle(R.string.screen_login)
                .setCancelable(false)
                .setMessage(R.string.instruction_wait_send_data)
                .create();
        Log.d(TAG, String.format("Requesting authorization for caregiver '%s' with password '%s'...",
                caregiverId, password));
        getServerUploaderServiceProvider().requestAccountConfirmation(caregiverId, patientId,
                password, new ServerRequestListener() {
                    @Override
                    public void onDeliveryStart() {
                        waitDialog.show();
                    }

                    @Override
                    public void onDeliveryFailure(String error, Throwable e) {
                        onServerUploadError(waitDialog,
                                "Failed to request account confirmation: " + error, e);
                    }

                    @Override
                    public void onTimeout() {
                        onServerTimeout(waitDialog);
                    }

                    @Override
                    public void onResponseReceived(Bundle data) {
                        String status = data.getString("status");
                        if (status.equals("valid")) {
                            Entry[] entries = onLogin(data);
                            goBackToParentActivity(getOnSuccessActivityClass(), entries);
                        } else {
                            String error = data.getString("extras");
                            if (error == null || error.isEmpty())
                                error = getString(R.string.error_wrong_id_or_password);

                            onServerResponse(waitDialog, error);
                        }
                    }
                }, mDevice);
    }

    /**
     * Sends a request to the server to know if the given pair caregiver/password is valid.
     */
    protected void requestAccountConfirmation(final String caregiverId, final String password) {
        requestAccountConfirmation(caregiverId, null, password);
    }

    /**
     * Opens the application activity responsible of creating a new user profile.
     */
    private void requestNewAccount() {
        goBackToParentActivity(getNewProfileActivityClass(),
                new Entry("PARENT", getClass().getName()),
                new Entry("DEVICE", mDevice.toString())
        );
    }

    /**
     * Sends a request to the server to retrieve a forgotten password. The password will be send
     * by email to the caregiver.
     */
    private void requestForgottenPassword() {
        TextFields fields = new TextFields(this);
        fields.setTextColor(getResources().getColor(R.color.white));
        final EditText idField = fields.addTextField(R.string.label_caregiver_id,
                InputType.TYPE_CLASS_TEXT);
        if (mUseUserIdAsDefault) {
            idField.setText(Settings.getCurrentUserId(this));
            idField.setEnabled(Settings.getCurrentUserId(this).isEmpty());
        }

        final CustomDialog waitDialog = new CustomDialog.Builder(this)
                .setTitle(R.string.screen_login)
                .setCancelable(false)
                .setMessage(R.string.instruction_wait_send_data)
                .create();

        final ServerRequestListener listener =
                new ServerRequestListener() {
                    @Override
                    public void onDeliveryStart() {
                        waitDialog.show();
                    }

                    @Override
                    public void onDeliveryFailure(String error, Throwable e) {
                        onServerUploadError(waitDialog,
                                "Failed to request caregiver password: " + error, e);
                    }

                    @Override
                    public void onTimeout() {
                        onServerTimeout(waitDialog);
                    }

                    @Override
                    public void onResponseReceived(Bundle data) {
                        String message;
                        if ("valid".equals(data.getString("status")))
                            message = getString(R.string.dialog_password_sent);
                        else
                            message = data.getString("extras");
                        onServerResponse(waitDialog, message);
                    }
                };

        new CustomDialog.Builder(this)
                .setTitle(R.string.screen_login)
                .setView(fields)
                .addFooterButton(R.string.action_cancel, null)
                .addFooterButton(R.string.action_done, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String id = idField.getText().toString();
                        getServerUploaderServiceProvider().requestCaregiverPassword(id,
                                listener, mDevice);
                    }
                })
                .show();
    }

    /**
     * Adds a hyperlink text to the activity.
     */
    private void addTextLink(int message, View.OnClickListener listener) {
        TextView label = new TextView(this);
        label.setTextColor(getResources().getColor(R.color.light_grey));
        label.setText(message);
        label.setGravity(Gravity.CENTER);
        label.setTypeface(null, Typeface.ITALIC);
        label.setOnClickListener(listener);

        addView(label);

        int margin = (int) getResources().getDimension(R.dimen.small_components_margin);
        ViewGroup.MarginLayoutParams params =
                (ViewGroup.MarginLayoutParams) label.getLayoutParams();
        params.setMargins(margin, margin, margin, margin);
    }

    /**
     * Method handling server communication errors.
     */
    private void onServerUploadError(final CustomDialog previousDialog, final String error,
                                     final Throwable e)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, error, e);
                if (!isFinishing()) {
                    previousDialog.dismiss();
                    new CustomDialog.Builder(LoginActivity.this)
                            .setTitle(R.string.screen_login)
                            .setMessage(R.string.error_server_upload_failed)
                            .addFooterButton(R.string.action_done, null)
                            .show();
                }
            }
        });
    }

    private void onServerTimeout(final CustomDialog previousDialog) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing()) {
                    previousDialog.dismiss();
                    new CustomDialog.Builder(LoginActivity.this)
                            .setTitle(R.string.screen_login)
                            .setMessage(R.string.error_no_server_response)
                            .addFooterButton(R.string.action_done, null)
                            .show();
                }
            }
        });
    }

    private void onServerResponse(final CustomDialog previousDialog, final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing()) {
                    previousDialog.dismiss();
                    new CustomDialog.Builder(LoginActivity.this)
                            .setTitle(R.string.screen_login)
                            .setMessage(message)
                            .addFooterButton(R.string.action_done, null)
                            .show();
                }
            }
        });
    }
}
