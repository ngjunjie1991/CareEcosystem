package com.ucsf.core_phone.ui;

import android.content.Context;

import com.ucsf.core_phone.R;

/**
 * Enumeration holding some ui properties, such as background and foreground colors.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public enum Theme {
    Caregiver(1, R.color.caregiver_background, R.color.caregiver_foreground),
    Patient  (2, R.color.patient_background  , R.color.patient_foreground),
    Admin    (3, R.color.admin_background    , R.color.admin_foreground),
    Default  (0, R.color.default_background  , R.color.default_foreground);

    private final int mId;
    private final int mBackgroundColor;
    private final int mForegroundColor;

    Theme(int id, int backgroundColorId, int foregroundColorId) {
        this.mId = id;
        this.mBackgroundColor = backgroundColorId;
        this.mForegroundColor = foregroundColorId;
    }

    /** Returns the {@link Theme} corresponding to the given identifier. */
    public static Theme valueOf(int id) {
        switch (id) {
            case 1:
                return Caregiver;
            case 2:
                return Patient;
            case 3:
                return Admin;
            default:
                return Default;
        }
    }

    /** Returns the theme background color. */
    public int getBackgroundColor(Context context) {
        return context.getResources().getColor(mBackgroundColor);
    }

    /** Returns the theme foreground color. */
    public int getForegroundColor(Context context) {
        return context.getResources().getColor(mForegroundColor);
    }

    /** Returns the theme unique identifier. */
    public int getId() {
        return mId;
    }
}
