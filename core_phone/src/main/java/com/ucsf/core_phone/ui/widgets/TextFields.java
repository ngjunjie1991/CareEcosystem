package com.ucsf.core_phone.ui.widgets;

import android.content.Context;
import android.graphics.PorterDuff;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TextView;

import com.ucsf.core_phone.R;
import com.ucsf.core_phone.ui.Theme;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * TableLayout containing several text fields. Used to keep text fields aligned.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public class TextFields extends TableLayout implements AppComponent {
    private final AppComponent mParent;
    private final List<TextView> mLabels = new LinkedList<>();
    private final int mBackgroundColor;
    private int mTextColor;

    public TextFields(AppComponent parent) {
        super(parent.getViewContext());
        mParent = parent;

        setColumnStretchable(1, true);
        setGravity(Gravity.CENTER);

        mBackgroundColor = getViewTheme().getBackgroundColor(getContext());
        mTextColor = getViewTheme().getForegroundColor(getContext());
    }

    /**
     * Set the text color for the labels.
     */
    public void setTextColor(int color) {
        mTextColor = color;
        for (TextView label : mLabels)
            label.setTextColor(color);
    }

    /**
     * Add a new field. Returns the index of the newly created edit text.
     */
    private EditText addTextField(String text, int type) {
        LayoutInflater inflater = (LayoutInflater)
                getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View row = inflater.inflate(R.layout.text_field, this, false);
        addView(row, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

        TextView label = (TextView) row.findViewById(R.id.label);
        label.setText(text);
        label.setTextColor(mTextColor);
        label.setId(UUID.randomUUID().hashCode());

        EditText field = (EditText) row.findViewById(R.id.edit_text);
        field.setInputType(type);
        field.getBackground().setColorFilter(mBackgroundColor, PorterDuff.Mode.SCREEN);
        field.setTextColor(getResources().getColor(R.color.edit_text_color));
        field.setId(UUID.randomUUID().hashCode());

        mLabels.add(label);

        return field;
    }

    /**
     * Add a new field. Returns the index of the newly created edit text.
     */
    public EditText addTextField(int textId, int type) {
        return addTextField(getResources().getString(textId), type);
    }

    @Override
    public Theme getViewTheme() {
        return mParent.getViewTheme();
    }

    @Override
    public Context getViewContext() {
        return getContext();
    }
}
