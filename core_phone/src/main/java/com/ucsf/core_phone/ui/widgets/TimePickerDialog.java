package com.ucsf.core_phone.ui.widgets;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;

import com.ucsf.core_phone.R;

import java.lang.reflect.Method;

/**
 * Dialog to pick a period of time.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public class TimePickerDialog extends CustomDialog {
    private final NumberPicker[] mNumberPickers;
    private Runnable mOnDoneListener;

    private TimePickerDialog(AppComponent parent) {
        super(parent);

        LayoutInflater inflater = (LayoutInflater) getViewContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.time_picker_layout, null);
        mNumberPickers = new NumberPicker[]{
                (NumberPicker) view.findViewById(R.id.daysPicker),
                (NumberPicker) view.findViewById(R.id.hoursPicker),
                (NumberPicker) view.findViewById(R.id.minutesPicker),
                (NumberPicker) view.findViewById(R.id.secondsPicker)
        };

        initNumberPicker(mNumberPickers[0], 0, 99, "%d d");
        initNumberPicker(mNumberPickers[1], 0, 23, "%02d h");
        initNumberPicker(mNumberPickers[2], 0, 59, "%02d m");
        initNumberPicker(mNumberPickers[3], 0, 59, "%02d s");

        new CustomDialog.Builder(parent)
                .setView(view)
                .setTitle(R.string.label_pick_time)
                .addFooterButton(R.string.action_cancel, null)
                .addFooterButton(R.string.action_done, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mOnDoneListener != null)
                            mOnDoneListener.run();
                    }
                })
                .init(this);
    }

    public TimePickerDialog(AppComponent parent, long value) {
        this(parent);
        setValue(value);
    }

    /**
     * Returns the dialog time in milliseconds.
     */
    public long getValue() {
        return (((mNumberPickers[0].getValue() * 24 +
                mNumberPickers[1].getValue()) * 60 +
                mNumberPickers[2].getValue()) * 60 +
                mNumberPickers[3].getValue()) * 1000;
    }

    /**
     * Sets the dialog times (in milliseconds).
     */
    private void setValue(long value) {
        value /= 1000;
        mNumberPickers[3].setValue((int) (value % 60));
        value /= 60;
        mNumberPickers[2].setValue((int) (value % 60));
        value /= 60;
        mNumberPickers[1].setValue((int) (value % 24));
        value /= 24;
        mNumberPickers[0].setValue((int) (value));

        // Force update to properly format the first entry (known android bug)
        for (NumberPicker picker : mNumberPickers) {
            try {
                Method method = picker.getClass().getDeclaredMethod("changeValueByOne", boolean.class); // Comes from the Android API so should pass the obfuscation
                method.setAccessible(true);
                method.invoke(picker, true);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Sets the listener called when the dialog time changes.
     */
    public void setOnDoneListener(Runnable listener) {
        mOnDoneListener = listener;
    }

    /**
     * Initialize the widget responsible of picking a number.
     */
    private void initNumberPicker(NumberPicker picker, int min, int max, final String format) {
        picker.setMinValue(min);
        picker.setMaxValue(max);
        picker.setFormatter(new NumberPicker.Formatter() {
            @Override
            public String format(int value) {
                return String.format(format, value);
            }
        });
    }
}
