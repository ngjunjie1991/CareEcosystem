package com.ucsf.ui.widgets;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.ucsf.core.data.Entry;
import com.ucsf.core.services.Services;
import com.ucsf.core_phone.ui.Theme;

/**
 * Patient phone implementation of the {@link com.ucsf.core_phone.ui.widgets.AppScreen
 * application base screen}.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public class AppScreen extends com.ucsf.core_phone.ui.widgets.AppScreen {
    private static final String TYPE_EXTRA = "AppScreen.Theme";

    private String mCaregiverId = "";

    /** Returns the current cargiver id. */
    protected String getCaregiverId() {
        return mCaregiverId;
    }

    @Override
    public boolean isStatusValid() {
        return Services.areServicesEnabled(this);
    }

    /**
     * Equivalent to {@link AppScreen#onCreate(Bundle, Disposition, Theme)} with the application
     * theme provider inf the extra bundle under the key {@link AppScreen#TYPE_EXTRA}.
     */
    public Intent onCreate(Bundle savedInstanceState, Disposition disposition) {
        return onCreate(savedInstanceState, disposition,
                (Theme) getIntent().getExtras().get(TYPE_EXTRA));
    }

    @Override
    public Intent onCreate(Bundle savedInstanceState, Disposition disposition, Theme theme) {
        mCaregiverId = getIntent().getStringExtra("caregiver");
        return super.onCreate(savedInstanceState, disposition, theme);
    }

    @Override
    protected void openChildActivity(Class<? extends Activity> activity, Entry... entries) {
        Entry[] newEntries = new Entry[entries.length + 2];
        System.arraycopy(entries, 0, newEntries, 0, entries.length);
        newEntries[entries.length] = new Entry(TYPE_EXTRA, getViewTheme());
        newEntries[entries.length + 1] = new Entry("caregiver", mCaregiverId);

        super.openChildActivity(activity, newEntries);
    }

    @Override
    public void goBackToParentActivity(Class<? extends Activity> parentActivity, Entry... entries) {
        Entry[] newEntries = new Entry[entries.length + 2];
        System.arraycopy(entries, 0, newEntries, 0, entries.length);
        newEntries[entries.length] = new Entry(TYPE_EXTRA, getViewTheme());
        newEntries[entries.length + 1] = new Entry("caregiver", mCaregiverId);

        super.goBackToParentActivity(parentActivity, newEntries);
    }
}
