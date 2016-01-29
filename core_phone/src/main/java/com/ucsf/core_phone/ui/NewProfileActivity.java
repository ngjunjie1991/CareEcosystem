package com.ucsf.core_phone.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.ucsf.core.data.DeviceLocation;
import com.ucsf.core.data.Entry;
import com.ucsf.core_phone.R;
import com.ucsf.core_phone.data.CaregiverInfo;
import com.ucsf.core_phone.services.ServerRequestListener;
import com.ucsf.core_phone.services.ServerUploaderService;
import com.ucsf.core_phone.ui.widgets.AppScreen;
import com.ucsf.core_phone.ui.widgets.CustomDialog;


/**
 * Activity to create a new caregiver profile. This activity is only called the first time the
 * application is launched.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public abstract class NewProfileActivity extends AppScreen implements ServerRequestListener {
    private static final String TAG              = "ucsf:NewProfileActivity";
    private static final String KEY_STATUS       = "status";
    private static final String KEY_EXTRAS       = "extras";
    private static final String KEY_VALID_STATUS = "valid";

    private DeviceLocation            mDevice;
    private CustomDialog              mWaitDialog;
    private Class<? extends Activity> mParentActivityClass;

    private EditText mIdField;
    private EditText mPasswordField;
    private EditText mPasswordConfirmationField;
    private EditText mUsernameField;
    private EditText mEmailField;
    private EditText mPhoneNumberEditText;
    private CheckBox mAcceptEmailCheckbox;
    private CheckBox mAcceptSMSCheckbox;
    private TextView mEmailLabel;
    private TextView mPhoneNumberLabel;

    @SuppressWarnings("unchecked")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent intent = super.onCreate(savedInstanceState, AppScreen.Disposition.LastExpanded,
                Theme.Caregiver);
        mDevice = DeviceLocation.fromTag(intent.getStringExtra("DEVICE"));
        try {
            mParentActivityClass =
                    (Class<? extends Activity>) Class.forName(intent.getStringExtra("PARENT"));
        } catch (Exception e) {
            Log.e(TAG, "Failed to retrieve parent activity class: ", e);
            System.exit(-1);
        }

        setTitle(R.string.screen_new_profile);

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.new_profile_layout, null);

        mIdField = (EditText) view.findViewById(R.id.caregiverIdEditText);
        mPasswordField = (EditText) view.findViewById(R.id.passwordEditText);
        mPasswordConfirmationField = (EditText) view.findViewById(R.id.passwordConfirmationEditText);
        mUsernameField = (EditText) view.findViewById(R.id.usernameEditText);
        mEmailField = (EditText) view.findViewById(R.id.emailEditText);
        mPhoneNumberEditText = (EditText) view.findViewById(R.id.phoneNumberEditText);
        mAcceptEmailCheckbox = (CheckBox) view.findViewById(R.id.acceptEmailCheckbox);
        mAcceptSMSCheckbox = (CheckBox) view.findViewById(R.id.acceptSMSCheckbox);
        mEmailLabel = (TextView) view.findViewById(R.id.emailLabel);
        mPhoneNumberLabel = (TextView) view.findViewById(R.id.phoneNumberLabel);

        addView(view);

        mAcceptEmailCheckbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int visibility = mAcceptEmailCheckbox.isChecked() ? View.VISIBLE : View.GONE;
                mEmailField.setVisibility(visibility);
                mEmailLabel.setVisibility(visibility);
            }
        });

        mAcceptSMSCheckbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int visibility = mAcceptSMSCheckbox.isChecked() ? View.VISIBLE : View.GONE;
                mPhoneNumberEditText.setVisibility(visibility);
                mPhoneNumberLabel.setVisibility(visibility);
            }
        });

        addFooterButton(R.string.action_cancel, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goBackToParentActivity(mParentActivityClass, new Entry("LOGIN", false));
            }
        });

        addFooterButton(R.string.action_done, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateCaregiverProfile();
            }
        });
    }

    @Override
    public void onDeliveryStart() {
        mWaitDialog = new CustomDialog.Builder(this)
                .setTitle(R.string.screen_new_profile)
                .setCancelable(false)
                .setMessage(R.string.instruction_wait_server_response)
                .show();
    }

    @Override
    public void onDeliveryFailure(String error, Throwable e) {
        Log.e(TAG, "Failed to send caregiver profile to the server: " + error, e);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing()) {
                    mWaitDialog.dismiss();
                    mWaitDialog = new CustomDialog.Builder(NewProfileActivity.this)
                            .setTitle(R.string.screen_new_profile)
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
                    mWaitDialog.dismiss();
                    mWaitDialog = new CustomDialog.Builder(NewProfileActivity.this)
                            .setTitle(R.string.screen_new_profile)
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
                    String status = data.getString(KEY_STATUS);
                    switch (status) {
                        case KEY_VALID_STATUS:
                            goBackToParentActivity(mParentActivityClass,
                                    new Entry("LOGIN", true),
                                    new Entry("DATA", data)
                            );
                            break;
                        default:
                            // Notify the user that something is invalid
                            String error = data.getString(KEY_EXTRAS);
                            Log.e(TAG, "Failed to validate caregiver profile: " + error);
                            mWaitDialog.dismiss();
                            mWaitDialog = new CustomDialog.Builder(NewProfileActivity.this)
                                    .setTitle(R.string.screen_new_profile)
                                    .setMessage(String.format(
                                            getString(R.string.error_invalid_caregiver_profile), error))
                                    .addFooterButton(R.string.action_done, null)
                                    .show();
                            break;
                    }
                }
            }
        });
    }

    protected abstract ServerUploaderService.Provider getServerUploaderServiceProvider();

    private void validateCaregiverProfile() {
        String phoneNumber = mPhoneNumberEditText.getText().toString();
        if (phoneNumber.startsWith("+"))
            phoneNumber = phoneNumber.substring(1);
        else
            phoneNumber = "1" + phoneNumber;

        CaregiverInfo info = new CaregiverInfo(
                mIdField.getText().toString(),
                mUsernameField.getText().toString(),
                mPasswordField.getText().toString(),
                mEmailField.getText().toString(),
                phoneNumber,
                mAcceptEmailCheckbox.isChecked(),
                mAcceptSMSCheckbox.isChecked()
        );
        String password = mPasswordConfirmationField.getText().toString();

        if (info.caregiverId.isEmpty()) { // Check if the id is not empty
            new CustomDialog.Builder(NewProfileActivity.this)
                    .setTitle(R.string.screen_new_profile)
                    .setMessage(R.string.error_no_caregiver_id_provided)
                    .addFooterButton(R.string.action_done, null)
                    .show();
        } else if (info.username.isEmpty()) { // Check if the username is not empty
            new CustomDialog.Builder(NewProfileActivity.this)
                    .setTitle(R.string.screen_new_profile)
                    .setMessage(R.string.error_no_username_provided)
                    .addFooterButton(R.string.action_done, null)
                    .show();
        } else if (info.acceptMail && (info.email.isEmpty() ||
                !Patterns.EMAIL_ADDRESS.matcher(info.email).matches())) { // Check if the email is not empty
            new CustomDialog.Builder(NewProfileActivity.this)
                    .setTitle(R.string.screen_new_profile)
                    .setMessage(R.string.error_no_email_provided)
                    .addFooterButton(R.string.action_done, null)
                    .show();
        } else if (info.acceptSMS && (info.phoneNumber.isEmpty() ||
                !Patterns.PHONE.matcher(info.phoneNumber).matches())) { // Check if the email is not empty
            new CustomDialog.Builder(NewProfileActivity.this)
                    .setTitle(R.string.screen_new_profile)
                    .setMessage(R.string.error_no_phone_number_provided)
                    .addFooterButton(R.string.action_done, null)
                    .show();
        } else if (info.password.isEmpty()) { // Check if the password is not empty
            new CustomDialog.Builder(NewProfileActivity.this)
                    .setTitle(R.string.screen_new_profile)
                    .setMessage(R.string.error_no_password_provided)
                    .addFooterButton(R.string.action_done, null)
                    .show();
        } else if (!info.password.equals(password)) { // Check if the two passwords are the same
            new CustomDialog.Builder(NewProfileActivity.this)
                    .setTitle(R.string.screen_new_profile)
                    .setMessage(R.string.error_invalid_password_confirmation)
                    .addFooterButton(R.string.action_done, null)
                    .show();
        } else { // Send the profile to the server to see if it's valid
            getServerUploaderServiceProvider().sendCaregiverProfile(info, this, mDevice);
        }
    }
}
