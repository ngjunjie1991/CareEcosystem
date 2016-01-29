package com.ucsf.ui.tester;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.ucsf.R;
import com.ucsf.core.data.DataManager;
import com.ucsf.core.data.DataManager.Condition;
import com.ucsf.core.data.Entry;
import com.ucsf.core.data.RSSI;
import com.ucsf.core.data.SharedTables;
import com.ucsf.core.data.Timestamp;
import com.ucsf.core_phone.ui.Theme;
import com.ucsf.core_phone.ui.widgets.CustomDialog;
import com.ucsf.core_phone.ui.widgets.TimeLabel;
import com.ucsf.data.PatientProfile;
import com.ucsf.data.PatientProfile.Room;
import com.ucsf.data.Settings;
import com.ucsf.ui.widgets.AppScreen;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Activity displaying a table content.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public class TableContentActivity extends AppScreen {
    private static final String TAG = "ucsf:TableContent";

    private final Map<FieldDescriptor, TextView> mHeaders = new HashMap<>();
    private       FieldDescriptor[]              mFields;
    private       Condition[]                    mConditions;
    private       DataManager                    mInstance;
    private       DataManager.Table              mTable;
    private       Adapter                        mAdapter = null;
    private       Drawable                       mDeleteIcon;

    @Override
    public void onDestroy() {
        if (mAdapter != null)
            mAdapter.changeCursor(null); // Invalidate the opened cursor
        if (mInstance != null) {
            try {
                mInstance.close();
            } catch (Exception e) {
                Log.e(TAG, "Failed to close database: ", e);
            }
        }
        super.onDestroy();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, Disposition.LastExpanded, Theme.Admin);
        lockOrientation(false);

        // Retrieve the table
        Intent intent = getIntent();
        String tableName = intent.getStringExtra("table");
        setTitle(String.format(getString(R.string.screen_table_content), tableName));

        addFooterButton(R.string.action_back, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goBackToParentActivity(TablesListActivity.class);
            }
        });

        try {
            mInstance = DataManager.get(this);
        } catch (Exception e) {
            Log.e(TAG, "Failed to open database: ", e);
            return;
        }

        mTable = DataManager.getTable(tableName);
        if (mTable == null) {
            addMessage(String.format(getString(R.string.alert_invalid_table_name), tableName));
            return;
        }

        mDeleteIcon = getResources().getDrawable(R.drawable.delete);
        mDeleteIcon.setColorFilter(getViewTheme().getBackgroundColor(this), PorterDuff.Mode.SRC_ATOP);

        // Retrieve the fields
        mFields = (FieldDescriptor[]) intent.getSerializableExtra("fields");
        if (mFields == null) {
            mFields = new FieldDescriptor[mTable.fields.length];
            for (int i = 0; i < mFields.length; ++i)
                mFields[i] = new FieldDescriptor(mTable.fields[i]);
        }

        // Create the content table
        mAdapter = new Adapter(this);
        ListView list = addList(mAdapter);

        // Create the headers and the cursor conditions
        List<Condition> conditions = new LinkedList<>();
        LinearLayout headerView = new LinearLayout(this);
        LinearLayout.LayoutParams params = null;
        headerView.setBackgroundColor(getViewTheme().getBackgroundColor(TableContentActivity.this));
        if (canDeleteEntries())
            headerView.setPadding(80, 0, 0, 0);
        int margin = (int) getResources().getDimension(R.dimen.small_components_margin);

        for (FieldDescriptor field : mFields) {
            if (!field.isEnabled)
                continue;

            TextView header = new TextView(this);
            header.setMinWidth(40);
            header.setSingleLine(true);
            header.setTypeface(null, Typeface.BOLD);
            header.setText(field.tag.equals(DataManager.KEY_IS_COMMITTED) ? "" : field.tag);
            header.setTextColor(getResources().getColor(R.color.text_color));
            header.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    (getResources().getDimension(R.dimen.small_text_size) +
                            getResources().getDimension(R.dimen.text_size)) / 2);

            headerView.addView(header);
            mHeaders.put(field, header);

            params = (LinearLayout.LayoutParams) header.getLayoutParams();
            params.setMargins(margin, margin, margin, margin);

            conditions.addAll(field.conditions);
        }

        // The last header occupies all the remaining space
        if (params != null)
            params.weight = 1;

        list.addHeaderView(headerView);

        // Set the table cursor
        try {
            mConditions = conditions.toArray(new DataManager.Condition[conditions.size()]);
            updateCursor();
        } catch (Exception e) {
            Log.e(TAG, "Failed to read database: ", e);
        }

        // Add the options footer button
        addFooterButton(R.string.action_options, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openChildActivity(TableOptionsActivity.class,
                        new Entry("table", mTable.tag), new Entry("fields", mFields));
            }
        });
    }

    private void updateCursor() throws Exception {
        DataManager.Cursor cursor = mTable.fetch(mConditions);
        mAdapter.changeCursor(cursor.delegateCursor());
    }

    private boolean canDeleteEntries() {
        try (DataManager instance = DataManager.get(this)) {
            return mTable == SharedTables.GroundTrust.getTable(instance);
        } catch (Exception e) {
            Log.wtf(TAG, "Failed to retrieve GroundTrust table: ", e);
            System.exit(-1);
            return false;
        }
    }

    public static class FieldDescriptor extends DataManager.TableField {
        public final List<DataManager.Condition> conditions = new LinkedList<>();
        public boolean isEnabled = true;

        public FieldDescriptor(DataManager.TableField field) {
            super(field.tag, field.type, field.defaultValue);
        }
    }

    private class Adapter extends CursorAdapter implements View.OnClickListener {
        public Adapter(Context context) {
            super(context, null, FLAG_REGISTER_CONTENT_OBSERVER);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            LinearLayout layout = new LinearLayout(context);
            View[] views;
            int offset = 0;

            if (canDeleteEntries()) {
                views = new View[mFields.length + 1];

                ImageButton deleteButton = new ImageButton(TableContentActivity.this);
                deleteButton.setImageDrawable(mDeleteIcon);
                deleteButton.setBackground(null);
                deleteButton.setAdjustViewBounds(false);
                deleteButton.setOnClickListener(this);

                views[0] = deleteButton;
                layout.addView(deleteButton);

                LinearLayout.LayoutParams params =
                        (LinearLayout.LayoutParams) deleteButton.getLayoutParams();
                params.gravity = Gravity.CENTER;

                ++offset;
            } else
                views = new View[mFields.length];

            int i = 0;
            int margin = (int) getResources().getDimension(R.dimen.small_components_margin);
            for (FieldDescriptor field : mFields) {
                View view;
                switch (field.type) {
                    case Boolean:
                        view = createCheckBox(context);
                        break;
                    default:
                        view = createTextView(context);
                        break;
                }

                views[offset + i++] = view;
                layout.addView(view);

                LinearLayout.LayoutParams params =
                        (LinearLayout.LayoutParams) view.getLayoutParams();
                if (view instanceof CheckBox)
                    params.gravity = Gravity.CENTER;
                else {
                    params.setMargins(margin, margin, margin, margin);
                    params.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT;
                }
            }

            layout.setTag(views);

            return layout;
        }

        @Override
        public void bindView(View rowView, Context context, Cursor cursor) {
            View[] views = (View[]) rowView.getTag();
            int rowId = cursor.getInt(cursor.getColumnIndex(DataManager.KEY_ROW_ID));
            int offset = 0;

            if (canDeleteEntries()) {
                views[0].setTag(R.id.rowId_tag, rowId);
                ++offset;
            }

            for (int i = 0; i < mFields.length; ++i) {
                final View view = views[offset + i];
                final FieldDescriptor field = mFields[i];

                if (field.isEnabled) {
                    view.setVisibility(View.VISIBLE);

                    switch (field.type) {
                        case Text:
                        case UniqueText:
                            updateView((TextView) view, field, rowId, getString(field, cursor));
                            break;
                        case Real:
                            updateView((TextView) view, field, rowId, getDouble(field, cursor));
                            break;
                        case Integer:
                            updateView((TextView) view, field, rowId, getInteger(field, cursor));
                            break;
                        case Boolean:
                            updateView((CheckBox) view, field, rowId, getBoolean(field, cursor));
                            break;
                        case Long:
                            updateView((TextView) view, field, rowId, getLong(field, cursor));
                            break;
                        case Blob:
                            updateView((TextView) view, field, rowId, getSerializable(field, cursor));
                            break;
                    }
                } else
                    view.setVisibility(View.GONE);
            }
        }

        private String getString(FieldDescriptor field, Cursor cursor) {
            return cursor.getString(cursor.getColumnIndex(field.tag));
        }

        private double getDouble(FieldDescriptor field, Cursor cursor) {
            return cursor.getDouble(cursor.getColumnIndex(field.tag));
        }

        private int getInteger(FieldDescriptor field, Cursor cursor) {
            return cursor.getInt(cursor.getColumnIndex(field.tag));
        }

        private boolean getBoolean(FieldDescriptor field, Cursor cursor) {
            return cursor.getInt(cursor.getColumnIndex(field.tag)) != 0;
        }

        private long getLong(FieldDescriptor field, Cursor cursor) {
            return cursor.getLong(cursor.getColumnIndex(field.tag));
        }

        private Serializable getSerializable(FieldDescriptor field, Cursor cursor) {
            try {
                byte[] bytes = cursor.getBlob(cursor.getColumnIndex(field.tag));
                ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
                ObjectInputStream ois = new ObjectInputStream(bis);
                return (Serializable) ois.readObject();
            } catch (Exception e) {
                Log.e(TAG, "Failed to read serializable object: ", e);
            }
            return "";
        }

        private View createTextView(Context context) {
            TextView view = new TextView(context);
            view.setSingleLine(true);
            view.setTextColor(getViewTheme().getForegroundColor(TableContentActivity.this));
            view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimension(R.dimen.small_text_size));
            view.setOnClickListener(this);
            return view;
        }

        private View createCheckBox(Context context) {
            CheckBox view = new CheckBox(context);
            view.setClickable(false);
            return view;
        }

        private void updateView(TextView view, FieldDescriptor field, int rowId, Serializable value) {
            view.setWidth(mHeaders.get(field).getWidth());
            view.setText(formatValue(field, value));
            view.setTag(R.id.value_tag, value);
            view.setTag(R.id.field_tag, field);
            view.setTag(R.id.rowId_tag, rowId);
        }

        private void updateView(CheckBox view, FieldDescriptor field, int rowId, boolean value) {
            view.setChecked(value);
        }

        private String formatValue(FieldDescriptor field, Serializable value) {
            if (value instanceof Room[])
                return Arrays.toString((Room[]) value);

            if (value instanceof String) {
                if (field.tag.equals(DataManager.KEY_TIMESTAMP)) {
                    Calendar calendar = Timestamp.getCalendarFromTimestamp((String) value);
                    return Timestamp.getTimestampFromCalendar(calendar, Timestamp.Format.MMDDYY_HHMMSS);
                }
                return (String) value;
            }

            if (value instanceof Boolean)
                return ((Boolean) value) ? "[x]" : "[ ]";

            if (value instanceof Long) {
                long period = (Long) value;
                if (period > 31536000000L) { // Period longer than one year
                    Calendar calendar = Timestamp.getCalendarFromTime(period);
                    return Timestamp.getTimestampFromCalendar(calendar, Timestamp.Format.MMDDYY_HHMMSS);
                }
                return TimeLabel.formatPeriod(period);
            }

            if (value instanceof Double)
                return String.format("%.3f", (Double) value);

            if (value instanceof RSSI) {
                RSSI rssi = (RSSI) value;

                // Retrieve the patient profile
                Cursor cursor = getCursor();
                String patientId = cursor.getString(cursor.getColumnIndex(DataManager.KEY_PATIENT_ID));
                PatientProfile profile = Settings.getPatientProfile(TableContentActivity.this, patientId);

                if (profile == null) {
                    Log.e(TAG, String.format("Failed to find patient profile '%s'!", patientId));
                    return rssi.toString();
                }

                // Only display in range motes' signals
                StringBuilder ss = new StringBuilder();
                ss.append("[");

                boolean isEmpty = true;
                if (profile.rooms != null) {
                    for (Room room : profile.rooms) {
                        double v = rssi.get(room);
                        if (v != RSSI.DEFAULT_RSSI) {
                            if (!isEmpty)
                                ss.append("\n ");
                            ss.append(room.getRoomName()).append(": ").append(v);
                            isEmpty = false;
                        }
                    }
                }

                if (isEmpty)
                    ss.append(getResources().getString(R.string.alert_no_mote_in_range));
                ss.append("]");
                return ss.toString();
            }

            return value.toString();
        }

        @Override
        public void onClick(final View view) {
            if (view instanceof ImageButton) { // Delete button
                new CustomDialog.Builder(TableContentActivity.this)
                        .setTitle(getResources().getString(R.string.label_remove_entry))
                        .setMessage(String.format(
                                getResources().getString(R.string.alert_remove_entry), mTable.tag))
                        .addFooterButton(R.string.action_no, null)
                        .addFooterButton(R.string.action_yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                try {
                                    int rowId = (Integer) view.getTag(R.id.rowId_tag);
                                    mTable.erase(new Condition.Equal<>(DataManager.KEY_ROW_ID, rowId));
                                    updateCursor();
                                } catch (Exception e) {
                                    Log.e(TAG,
                                            String.format("Failed to remove entry from table '%s': ",
                                                    mTable.tag), e);
                                }
                            }
                        })
                        .show();
            } else { // Field selection
                try {
                    Serializable value = (Serializable) view.getTag(R.id.value_tag);
                    FieldDescriptor field = (FieldDescriptor) view.getTag(R.id.field_tag);
                    new CustomDialog.Builder(TableContentActivity.this)
                            .setTitle(String.format("%s: %s", mTable.tag, field.tag))
                            .setMessage(formatValue(field, value))
                            .addFooterButton(R.string.action_done, null)
                            .show();
                } catch (Exception e) {
                    Log.e(TAG, "Failed to display entry value: ", e);
                }
            }
        }

    }

}
