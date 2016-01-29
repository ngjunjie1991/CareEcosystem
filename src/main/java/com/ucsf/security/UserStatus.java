package com.ucsf.security;

import com.ucsf.core_phone.ui.Theme;

/**
 * Specifies the user status. Depending on the user status, different options will be available.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public enum UserStatus {
    Admin  (Theme.Caregiver),
    Patient(Theme.Patient),
    Tester (Theme.Admin),
    None   (Theme.Default);

    private final Theme mTheme;

    UserStatus(Theme theme) {
        mTheme = theme;
    }
}
