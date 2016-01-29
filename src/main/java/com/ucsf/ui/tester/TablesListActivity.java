package com.ucsf.ui.tester;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.ucsf.R;
import com.ucsf.core.data.DataManager;
import com.ucsf.core.data.Entry;
import com.ucsf.core_phone.ui.Theme;
import com.ucsf.services.StartupService;
import com.ucsf.ui.widgets.AppScreen;

/**
 * Activity displaying the list of the internal tables.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public class TablesListActivity extends AppScreen {
    private DataManager.Table[] mTables;
    private Drawable mPhoneDrawable;
    private Drawable mWatchDrawable;
    private Drawable mSettingsDrawable;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, Disposition.Centered, Theme.Admin);

        setTitle(R.string.screen_tables_list);

        mTables = DataManager.getTables();

        mPhoneDrawable = getResources().getDrawable(R.drawable.phone);
        mWatchDrawable = getResources().getDrawable(R.drawable.watch);
        mSettingsDrawable = getResources().getDrawable(R.drawable.settings);

        StartupService.loadTables(this);
        ListView tables = addList(new Adapter(this, R.layout.table_cell, mTables));
        tables.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final DataManager.Table table = mTables[position];

                openChildActivity(TableContentActivity.class, new Entry("table", table.tag));
            }
        });

        addFooterButton(R.string.action_back, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goBackToParentActivity(TesterMenuActivity.class);
            }
        });
    }

    private class Adapter extends ArrayAdapter<DataManager.Table> {
        public Adapter(Context context, int resource, DataManager.Table[] objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater)
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            // Set the rowView and its holder depending on a previous version
            View rowView;
            ViewHolder viewHolder;
            if (convertView == null) {
                rowView = inflater.inflate(R.layout.table_cell, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.nameLabel = (TextView) rowView.findViewById(R.id.label);
                viewHolder.deviceIcon = (ImageView) rowView.findViewById(R.id.deviceIcon);
                rowView.setTag(viewHolder);
            } else {
                rowView = convertView;
                viewHolder = (ViewHolder) rowView.getTag();
            }

            final DataManager.Table table = mTables[position];
            viewHolder.nameLabel.setText(table.tag);
            switch (table.location) {
                case PatientPhone:
                    viewHolder.deviceIcon.setImageDrawable(mPhoneDrawable);
                    break;
                case PatientWatch:
                    viewHolder.deviceIcon.setImageDrawable(mWatchDrawable);
                    break;
                case Unknown:
                    viewHolder.deviceIcon.setImageDrawable(mSettingsDrawable);
                    break;
            }

            return rowView;
        }

        /**
         * Class to store component's references.
         */
        private class ViewHolder {
            TextView nameLabel;
            ImageView deviceIcon;
        }
    }
}