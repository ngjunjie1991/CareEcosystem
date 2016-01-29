package com.ucsf.ui.tester;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.ucsf.R;
import com.ucsf.core_phone.ui.Theme;
import com.ucsf.data.PatientProfile;
import com.ucsf.data.Settings;
import com.ucsf.services.GroundTrust;
import com.ucsf.ui.widgets.AppScreen;

/**
 * Activity allowing to register some timestamps associated with a room and an user name
 * in order to create ground trust data.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public class GroundTrustActivity extends AppScreen
        implements GroundTrust.Listener, View.OnClickListener
{
    private PatientProfile     mPatientProfile;
    private Drawable           mArrowDown;
    private Drawable           mArrowUp;
    private ExpandableListView mList;
    private Adapter            mAdapter;
    private boolean            mEnabled = false;

    private GroundTrustHandler[] mGTHandlers = new GroundTrustHandler[]{
            new GroundTrustHandler(GroundTrust.Type.CurrentRoom),
            new GroundTrustHandler(GroundTrust.Type.IsInHouse),
            new GroundTrustHandler(GroundTrust.Type.IsWearingWatch)
    };

    @Override
    protected void onStart() {
        super.onStart();
        for (GroundTrustHandler handler : mGTHandlers) {
            if (!handler.gt.isInitialized())
                handler.gt.init(getViewContext(), this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        for (GroundTrustHandler handler : mGTHandlers) {
            if (!handler.gt.isInitialized())
                handler.gt.init(getViewContext(), this);
        }
    }

    @Override
    protected void onStop() {
        for (GroundTrustHandler handler : mGTHandlers)
            handler.gt.release();
        super.onStop();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, Disposition.LastExpanded, Theme.Admin);

        mPatientProfile = Settings.getCurrentPatientProfile(this);
        mArrowDown = getResources().getDrawable(android.R.drawable.arrow_down_float);
        mArrowUp = getResources().getDrawable(android.R.drawable.arrow_up_float);

        setTitle(R.string.screen_ground_trust);

        addInstruction(R.string.instruction_ground_trust);

        mAdapter = new Adapter();
        mList = addExpandableList(mAdapter);
        mList.setGroupIndicator(null);

        addFooterButton(R.string.action_back, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (GroundTrustHandler handler : mGTHandlers)
                    handler.gt.release();
                goBackToParentActivity(TesterMenuActivity.class);
            }
        });
    }

    @Override
    public void onMonitoringStarted() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(GroundTrustActivity.this,
                        getString(R.string.toast_watch_connected), Toast.LENGTH_SHORT).show();
                mEnabled = true;
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onMonitoringFailedToStart(String error) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(GroundTrustActivity.this,
                        getString(R.string.toast_watch_connection_failed), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onClick(View v) {
        Adapter.ChildViewHolder viewHolder = (Adapter.ChildViewHolder) v.getTag();
        if (viewHolder.handler.currentSelection == -1) {
            viewHolder.handler.gt.startAcquisition(viewHolder.text);
            viewHolder.handler.currentSelection = viewHolder.index;
        } else if (viewHolder.handler.currentSelection == viewHolder.index) {
            viewHolder.handler.gt.stopAcquisition();
            viewHolder.handler.currentSelection = -1;
        } else {
            viewHolder.handler.gt.stopAcquisition();
            viewHolder.handler.gt.startAcquisition(viewHolder.text);
            viewHolder.handler.currentSelection = viewHolder.index;
        }
        mAdapter.notifyDataSetChanged();
    }

    private class GroundTrustHandler {
        final GroundTrust.Type type;
        final GroundTrust gt;
        int currentSelection = -1;

        public GroundTrustHandler(GroundTrust.Type type) {
            this.type = type;
            this.gt   = new GroundTrust(type);
        }
    }

    private class Adapter extends BaseExpandableListAdapter {
        @Override
        public int getGroupCount() {
            return mGTHandlers.length;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            switch (mGTHandlers[groupPosition].type) {
                case CurrentRoom:
                    return mPatientProfile.rooms.length;
                case IsInHouse:
                case IsWearingWatch:
                    return 2;
            }
            return 0;
        }

        @Override
        public Object getGroup(int groupPosition) {
            return mGTHandlers[groupPosition];
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            switch (mGTHandlers[groupPosition].type) {
                case CurrentRoom:
                    return mPatientProfile.rooms[childPosition].getRoomName();
                case IsInHouse:
                case IsWearingWatch:
                    return getString(childPosition == 0 ? R.string.label_yes : R.string.label_no);
            }
            return null;
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getGroupView(final int groupPosition, final boolean isExpanded,
                                 View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater)
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            // Set the rowView and its holder depending on a previous version
            View rowView;
            GroupViewHolder viewHolder;
            if (convertView == null) {
                rowView = inflater.inflate(R.layout.ground_trust_cell, parent, false);
                viewHolder = new GroupViewHolder();
                viewHolder.label = (TextView) rowView.findViewById(R.id.label);
                viewHolder.expandButton = (ImageView) rowView.findViewById(R.id.expand_button);

                rowView.setTag(viewHolder);
            } else {
                rowView = convertView;
                viewHolder = (GroupViewHolder) rowView.getTag();
            }

            switch (mGTHandlers[groupPosition].type) {
                case CurrentRoom:
                    viewHolder.label.setText(R.string.label_current_room);
                    break;
                case IsInHouse:
                    viewHolder.label.setText(R.string.label_in_house);
                    break;
                case IsWearingWatch:
                    viewHolder.label.setText(R.string.label_wear_watch);
                    break;
            }

            // Update the expandable arrow
            viewHolder.expandButton.setImageDrawable(isExpanded ? mArrowUp : mArrowDown);
            rowView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isExpanded)
                        mList.collapseGroup(groupPosition);
                    else
                        mList.expandGroup(groupPosition);
                }
            });
            rowView.setEnabled(mEnabled);

            return rowView;
        }

        @Override
        public View getChildView(int groupPosition, final int childPosition, boolean isLastChild,
                                 View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater)
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            // Set the rowView and its holder depending on a previous version
            View rowView;
            final ChildViewHolder viewHolder;
            if (convertView == null) {
                rowView = inflater.inflate(R.layout.ground_trust_option_cell, parent, false);
                viewHolder = new ChildViewHolder();
                viewHolder.checkBox = (CheckBox) rowView.findViewById(R.id.checkBox);
                viewHolder.checkBox.setOnClickListener(GroundTrustActivity.this);
                viewHolder.checkBox.setTag(viewHolder);
                rowView.setTag(viewHolder);
            } else {
                rowView = convertView;
                viewHolder = (ChildViewHolder) rowView.getTag();
            }

            GroundTrustHandler handler = mGTHandlers[groupPosition];
            String label = (String) getChild(groupPosition, childPosition);
            viewHolder.checkBox.setChecked(handler.currentSelection == childPosition);
            viewHolder.checkBox.setText(label);
            viewHolder.handler = handler;
            viewHolder.index   = childPosition;
            viewHolder.text    = label;
            rowView.setEnabled(mEnabled);

            return rowView;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

        private class GroupViewHolder {
            TextView  label;
            ImageView expandButton;
        }

        private class ChildViewHolder {
            CheckBox           checkBox;
            GroundTrustHandler handler;
            int                index;
            String             text;
        }
    }
}
