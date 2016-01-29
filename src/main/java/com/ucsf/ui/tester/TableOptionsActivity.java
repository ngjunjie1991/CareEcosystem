package com.ucsf.ui.tester;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;

import com.ucsf.R;
import com.ucsf.core.data.DataManager;
import com.ucsf.core.data.Entry;
import com.ucsf.core.data.Timestamp;
import com.ucsf.core_phone.ui.Theme;
import com.ucsf.core_phone.ui.widgets.CustomDialog;
import com.ucsf.core_phone.ui.widgets.TextFields;
import com.ucsf.ui.tester.TableContentActivity.FieldDescriptor;
import com.ucsf.ui.widgets.AppScreen;

import java.util.Calendar;

/**
 * Screen showing some options concerning how data are displayed.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public class TableOptionsActivity extends AppScreen {
    private String            mTable;
    private FieldDescriptor[] mFields;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, Disposition.LastExpanded, Theme.Admin);

        Intent intent = getIntent();
        mTable = intent.getStringExtra("table");
        mFields = (FieldDescriptor[]) intent.getSerializableExtra("fields");

        setTitle(String.format(getString(R.string.screen_table_options), mTable));

        addInstruction(R.string.instruction_table_fields);

        addList(new Adapter(this, R.layout.table_field_cell, mFields));

        addFooterButton(R.string.action_back, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goBackToParentActivity(TableContentActivity.class,
                        new Entry("table", mTable), new Entry("fields", mFields));
            }
        });
    }

    private class Adapter extends ArrayAdapter<FieldDescriptor> {
        private final OnCheckedChangeListener mOnEnabledListener = new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean enabled) {
                ViewHolder viewHolder = (ViewHolder) compoundButton.getTag();
                viewHolder.field.isEnabled = enabled;
            }
        };
        private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ViewHolder viewHolder = (ViewHolder) view.getTag();
                switch (viewHolder.field.tag) {
                    case DataManager.KEY_PATIENT_ID:
                        showEditPatientIdConfigDialog(viewHolder.field);
                        break;
                    case DataManager.KEY_TIMESTAMP:
                        showEditTimestampRangeConfigDialog(viewHolder.field);
                        break;
                    case DataManager.KEY_IS_COMMITTED:
                        showCommitConfigDialog(viewHolder.field);
                        break;
                    default:
                        break;
                }
            }
        };

        public Adapter(Context context, int resource, FieldDescriptor[] objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(int position, View convertView, final ViewGroup parent) {
            // Set the rowView and its holder depending on a previous version
            View rowView;
            ViewHolder viewHolder;
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater)
                        getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                rowView = inflater.inflate(R.layout.table_field_cell, parent, false);
                viewHolder = new ViewHolder();

                viewHolder.label = (TextView) rowView.findViewById(R.id.label);
                viewHolder.checkBox = (CheckBox) rowView.findViewById(R.id.check_box);
                viewHolder.checkBox.setTag(viewHolder);
                viewHolder.checkBox.setOnCheckedChangeListener(mOnEnabledListener);

                rowView.setTag(viewHolder);
                rowView.setOnClickListener(mOnClickListener);
            } else {
                rowView = convertView;
                viewHolder = (ViewHolder) rowView.getTag();
            }

            viewHolder.field = mFields[position];
            viewHolder.label.setText(viewHolder.field.tag);
            viewHolder.checkBox.setChecked(viewHolder.field.isEnabled);

            return rowView;
        }

        private void showEditPatientIdConfigDialog(final FieldDescriptor field) {
            TextFields textField = new TextFields(TableOptionsActivity.this);
            final EditText editText = textField.addTextField(R.string.label_patient,
                    InputType.TYPE_CLASS_TEXT);
            textField.setTextColor(getResources().getColor(R.color.text_color));

            if (!field.conditions.isEmpty()) {
                @SuppressWarnings("unchecked")
                DataManager.Condition.Equal<String> cond =
                        (DataManager.Condition.Equal<String>) field.conditions.get(0);
                editText.setText(cond.value);
            }

            new CustomDialog.Builder(TableOptionsActivity.this)
                    .setTitle(String.format("%s: %s", mTable, field.tag))
                    .setView(textField)
                    .addFooterButton(R.string.action_cancel, null)
                    .addFooterButton(R.string.action_done, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            field.conditions.clear();
                            field.conditions.add(new DataManager.Condition.Equal<>(
                                    DataManager.KEY_PATIENT_ID, editText.getText().toString()));
                        }
                    }).show();
        }

        private void showCommitConfigDialog(final FieldDescriptor field) {
            TextFields textField = new TextFields(TableOptionsActivity.this);
            final EditText editText = textField.addTextField(R.string.label_is_committed,
                    InputType.TYPE_CLASS_TEXT);
            textField.setTextColor(getResources().getColor(R.color.text_color));

            if (!field.conditions.isEmpty()) {
                @SuppressWarnings("unchecked")
                DataManager.Condition.Equal<Integer> cond =
                        (DataManager.Condition.Equal<Integer>) field.conditions.get(0);
                if (cond.value != 0)
                    editText.setText(R.string.label_true);
                else
                    editText.setText(R.string.label_false);
            }

            new CustomDialog.Builder(TableOptionsActivity.this)
                    .setTitle(String.format("%s: %s", mTable, field.tag))
                    .setView(textField)
                    .addFooterButton(R.string.action_cancel, null)
                    .addFooterButton(R.string.action_done, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            field.conditions.clear();

                            String value = editText.getText().toString();
                            if (value.equalsIgnoreCase(getString(R.string.label_true))) {
                                field.conditions.add(new DataManager.Condition.Equal<>(
                                        DataManager.KEY_IS_COMMITTED, 1));
                            } else if (value.equalsIgnoreCase(getString(R.string.label_false))) {
                                field.conditions.add(new DataManager.Condition.Equal<>(
                                        DataManager.KEY_IS_COMMITTED, 0));
                            }
                        }
                    }).show();
        }

        private void showEditTimestampRangeConfigDialog(final FieldDescriptor field) {
            TextFields textField = new TextFields(TableOptionsActivity.this);
            final EditText fromEditText = textField.addTextField(R.string.label_from,
                    InputType.TYPE_CLASS_DATETIME);
            final EditText toEditText = textField.addTextField(R.string.label_to,
                    InputType.TYPE_CLASS_DATETIME);
            textField.setTextColor(getResources().getColor(R.color.text_color));

            initTimestampEditText(fromEditText);
            initTimestampEditText(toEditText);

            for (DataManager.Condition condition : field.conditions) {
                if (condition instanceof DataManager.Condition.GreaterEqual) {
                    @SuppressWarnings("unchecked")
                    DataManager.Condition.GreaterEqual<String> cond =
                            (DataManager.Condition.GreaterEqual<String>) condition;

                    Calendar calendar = Timestamp.getCalendarFromTimestamp(cond.value);
                    fromEditText.setText(Timestamp.getTimestampFromCalendar(calendar,
                            Timestamp.Format.MMDDYY_HHMMSS));
                } else if (condition instanceof DataManager.Condition.LessEqual) {
                    @SuppressWarnings("unchecked")
                    DataManager.Condition.LessEqual<String> cond =
                            (DataManager.Condition.LessEqual<String>) condition;

                    Calendar calendar = Timestamp.getCalendarFromTimestamp(cond.value);
                    toEditText.setText(Timestamp.getTimestampFromCalendar(calendar,
                            Timestamp.Format.MMDDYY_HHMMSS));
                } else if (condition instanceof DataManager.Condition.Equal) {
                    @SuppressWarnings("unchecked")
                    DataManager.Condition.Equal<String> cond =
                            (DataManager.Condition.Equal<String>) condition;

                    Calendar calendar = Timestamp.getCalendarFromTimestamp(cond.value);
                    String timestamp = Timestamp.getTimestampFromCalendar(calendar,
                            Timestamp.Format.MMDDYY_HHMMSS);
                    fromEditText.setText(timestamp);
                    toEditText.setText(timestamp);
                } else if (condition instanceof DataManager.Condition.Range) {
                    @SuppressWarnings("unchecked")
                    DataManager.Condition.Range<String> cond =
                            (DataManager.Condition.Range<String>) condition;

                    Calendar calendar = Timestamp.getCalendarFromTimestamp(cond.first);
                    fromEditText.setText(Timestamp.getTimestampFromCalendar(calendar,
                            Timestamp.Format.MMDDYY_HHMMSS));

                    calendar = Timestamp.getCalendarFromTimestamp(cond.last);
                    toEditText.setText(Timestamp.getTimestampFromCalendar(calendar,
                            Timestamp.Format.MMDDYY_HHMMSS));
                }
            }

            new CustomDialog.Builder(TableOptionsActivity.this)
                    .setTitle(String.format("%s: %s", mTable, field.tag))
                    .setView(textField)
                    .addFooterButton(R.string.action_cancel, null)
                    .addFooterButton(R.string.action_done, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            field.conditions.clear();
                            String fromTimestamp = fromEditText.getText().toString();
                            String toTimestamp = toEditText.getText().toString();

                            if (fromTimestamp.isEmpty() && toTimestamp.isEmpty())
                                return;

                            if (fromTimestamp.isEmpty()) {
                                Calendar calendar = Timestamp.getCalendarFromTimestamp(
                                        toTimestamp, Timestamp.Format.MMDDYY_HHMMSS);

                                field.conditions.add(new DataManager.Condition.LessEqual<>(
                                        DataManager.KEY_TIMESTAMP,
                                        Timestamp.getTimestampFromCalendar(calendar)));
                            } else if (toTimestamp.isEmpty()) {
                                Calendar calendar = Timestamp.getCalendarFromTimestamp(
                                        fromTimestamp, Timestamp.Format.MMDDYY_HHMMSS);

                                field.conditions.add(new DataManager.Condition.GreaterEqual<>(
                                        DataManager.KEY_TIMESTAMP,
                                        Timestamp.getTimestampFromCalendar(calendar)));
                            } else if (toTimestamp.equals(fromTimestamp)) {
                                Calendar calendar = Timestamp.getCalendarFromTimestamp(
                                        fromTimestamp, Timestamp.Format.MMDDYY_HHMMSS);

                                field.conditions.add(new DataManager.Condition.Equal<>(
                                        DataManager.KEY_TIMESTAMP,
                                        Timestamp.getTimestampFromCalendar(calendar)));
                            } else {
                                Calendar calendar = Timestamp.getCalendarFromTimestamp(
                                        fromTimestamp, Timestamp.Format.MMDDYY_HHMMSS);
                                fromTimestamp = Timestamp.getTimestampFromCalendar(calendar);

                                calendar = Timestamp.getCalendarFromTimestamp(
                                        toTimestamp, Timestamp.Format.MMDDYY_HHMMSS);
                                toTimestamp = Timestamp.getTimestampFromCalendar(calendar);

                                field.conditions.add(new DataManager.Condition.Range<>(
                                        DataManager.KEY_TIMESTAMP, fromTimestamp, toTimestamp));
                            }
                        }
                    }).show();
        }

        private void initTimestampEditText(final EditText editText) {
            TextWatcher formatter = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable editable) {
                    editText.removeTextChangedListener(this);

                    int i = 0;
                    while (i < editable.length()) {
                        switch (editable.charAt(i)) {
                            case ':':
                            case '/':
                            case '.':
                            case '-':
                                editable.delete(i, i + 1);
                                break;
                            default:
                                ++i;
                                break;
                        }
                    }

                    if (editable.length() > 2)
                        editable.insert(2, "/");
                    if (editable.length() > 5)
                        editable.insert(5, "/");
                    if (editable.length() > 8)
                        editable.insert(8, "-");
                    if (editable.length() > 11)
                        editable.insert(11, ":");
                    if (editable.length() > 14)
                        editable.insert(14, ":");
                    if (editable.length() > 17)
                        editable.delete(17, editable.length());

                    editText.addTextChangedListener(this);
                }
            };

            editText.setHint(R.string.label_timestamp_format);
            editText.addTextChangedListener(formatter);
        }

        class ViewHolder {
            TextView label;
            CheckBox checkBox;
            FieldDescriptor field;
        }
    }

}
