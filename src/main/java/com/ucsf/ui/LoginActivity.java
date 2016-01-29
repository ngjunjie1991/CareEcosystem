package com.ucsf.ui;

import android.app.Activity;
import android.os.Bundle;

import com.ucsf.core.data.DeviceLocation;
import com.ucsf.core.data.Entry;
import com.ucsf.core.data.Settings;
import com.ucsf.core.services.Services;
import com.ucsf.services.ServerUploaderService;
import com.ucsf.services.StartupService;
import com.ucsf.ui.admin.AdminMenuActivity;
import com.ucsf.ui.tester.TesterMenuActivity;

/**
 * Patient phone implementation of the {@link com.ucsf.core_phone.ui.LoginActivity login activity}.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public class LoginActivity extends com.ucsf.core_phone.ui.LoginActivity {
    private boolean mIsAdmin = false;

    public LoginActivity() {
        super(false, false, DeviceLocation.PatientPhone);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        StartupService.startServices(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    public Class<? extends Activity> getParentActivityClass() {
        return StartScreenActivity.class;
    }

    @Override
    public Class<? extends Activity> getOnSuccessActivityClass() {
        return mIsAdmin ? TesterMenuActivity.class : AdminMenuActivity.class;
    }

    @Override
    public boolean isStatusValid() {
        return Services.areServicesEnabled(this);
    }

    @Override
    protected Entry[] onLogin(Bundle extras) {
        mIsAdmin = Integer.valueOf(extras.getString("is_admin")) != 0;
        return new Entry[]{new Entry("caregiver", extras.getString("caregiver"))};
    }

    @Override
    protected ServerUploaderService.Provider getServerUploaderServiceProvider() {
        return ServerUploaderService.getProvider(this);
    }

    @Override
    protected Class<? extends NewProfileActivity> getNewProfileActivityClass() {
        return NewProfileActivity.class;
    }

    @Override
    protected void requestAccountConfirmation(final String caregiverId, final String password) {
        String patientId = Settings.getCurrentUserId(this);
        if (patientId.isEmpty())
            super.requestAccountConfirmation(caregiverId, password);
        else
            super.requestAccountConfirmation(caregiverId, patientId, password);
    }

}
