package com.ucsf.core_phone.ui.widgets;

import android.content.Context;

import com.ucsf.core_phone.ui.Theme;

/**
 * Interface to access some properties shared by all the app components.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public interface AppComponent {
    /** Returns the {@link Theme theme} used by this interface. */
    Theme getViewTheme();

    /** Returns the interface {@link android.content.Context context}. */
    Context getViewContext();
}
