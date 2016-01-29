package com.ucsf.core_phone.ui.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * Label widget showing a period of time.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public class TimeLabel extends TextView {
    private long mValue;

    public TimeLabel(Context context) {
        super(context);
        setValue(123456000);
    }

    public TimeLabel(Context context, AttributeSet attrs) {
        super(context, attrs);
        setValue(123456000);
    }

    public TimeLabel(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setValue(123456000);
    }

    /**
     * Format the given period (in milliseconds) in a readable format (day hours minutes seconds).
     */
    public static String formatPeriod(long period) {
        period /= 1000;
        if (period == 0)
            return "0s";

        int seconds = (int) (period % 60);
        period /= 60;
        int minutes = (int) (period % 60);
        period /= 60;
        int hours = (int) (period % 24);
        period /= 24;
        int days = (int) (period);

        StringBuilder ss = new StringBuilder();
        if (days > 0)
            ss.append(String.format("%dd", days));
        if (hours > 0)
            ss.append(String.format(" %dh", hours));
        if (minutes > 0)
            ss.append(String.format(" %dm", minutes));
        if (seconds > 0)
            ss.append(String.format(" %ds", seconds));

        return ss.toString();
    }

    /**
     * Returns the label time in milliseconds.
     */
    public long getValue() {
        return mValue;
    }

    /**
     * Sets the label time (in milliseconds).
     */
    public void setValue(long period) {
        mValue = period;
        setText(formatPeriod(period));
    }

}
