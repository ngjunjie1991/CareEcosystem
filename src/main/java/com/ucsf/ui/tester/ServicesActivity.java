package com.ucsf.ui.tester;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.ucsf.R;
import com.ucsf.core.data.DeviceLocation;
import com.ucsf.core.services.Messages;
import com.ucsf.core.services.ResponseListener;
import com.ucsf.core.services.ServiceCallback;
import com.ucsf.core.services.ServiceId;
import com.ucsf.core.services.ServiceParameter;
import com.ucsf.core.services.ServiceProperty;
import com.ucsf.core.services.Services;
import com.ucsf.core_phone.ui.Theme;
import com.ucsf.core_phone.ui.widgets.CustomDialog;
import com.ucsf.core_phone.ui.widgets.TimeLabel;
import com.ucsf.core_phone.ui.widgets.TimePickerDialog;
import com.ucsf.data.PatientProfile;
import com.ucsf.data.Settings;
import com.ucsf.services.DeviceInterface;
import com.ucsf.services.StartupService;
import com.ucsf.ui.widgets.AppScreen;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Activity displaying all the services available, a brief description and their status.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public class ServicesActivity extends AppScreen {
    private static final String TAG           = "ucsf:ServicesActivity";
    private static final long   UPDATE_PERIOD = 10000;

    private final Map<ServiceId, ServiceDescriptor> mServicesMap = new HashMap<>();
    private final List<ServiceDescriptor>           mServices    = new ArrayList<>();

    private Timer               mTimer = new Timer();
    private Drawable            mWarning;
    private Drawable            mEnabled;
    private Drawable            mDisabled;
    private Drawable            mArrowDown;
    private Drawable            mArrowUp;
    private Drawable            mPhoneDrawable;
    private Drawable            mWatchDrawable;
    private String              mServiceRunningFormat;
    private String              mServiceDisabledFormat;
    private String              mServiceErrorFormat;
    private Adapter             mAdapter;
    private Handler             mHandler;
    private ExpandableListView  mList;
    private Button              mServicesButton;

    @Override
    public void onStart() {
        super.onStart();

        DeviceInterface.requestWatchServices(this,
                new com.ucsf.core.services.DeviceInterface.RequestListener() {
                    @Override
                    public void requestProcessed(Messages.Request request, JSONObject data)
                            throws Exception
                    {
                        onServicesReceived(data);
                    }

                    @Override
                    public void requestTimeout(Messages.Request request) throws Exception {
                        Log.w(TAG, "Watch not found. New connection...");
                        if (!isFinishing() && !isDestroyed())
                            DeviceInterface.requestWatchServices(ServicesActivity.this, this);
                    }

                    @Override
                    public void requestCancelled(Messages.Request request) throws Exception {
                        Log.d(TAG, "Watch connection aborted.");
                    }
                });

        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                final Iterator<ServicesActivity.ServiceDescriptor> it = mServices.iterator();
                if (it.hasNext()) {
                    ServicesActivity.ServiceDescriptor descriptor = it.next();
                    descriptor.updateStatus(new ResponseListener() {
                        @Override
                        public void onSuccess() {
                            synchronized (it) {
                                if (it.hasNext())
                                    it.next().updateStatus(this);
                            }
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mAdapter.notifyDataSetChanged();
                                }
                            });
                        }

                        @Override
                        public void onFailure(String error, Throwable e) {
                            Log.e(TAG, error ,e);
                        }
                    });
                }
            }
        }, 0, UPDATE_PERIOD);
    }

    @Override
    public void onStop() {
        mTimer.cancel();
        super.onStop();
    }

    /**
     * Method called when the phone received the watch services descriptors.
     */
    public void onServicesReceived(JSONObject data) {
        final List<com.ucsf.core.services.ServiceDescriptor> descriptors =
                ServiceDescriptor.loadDescriptors(this, data);

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                for (com.ucsf.core.services.ServiceDescriptor descriptor : descriptors)
                    mServicesMap.put(descriptor.service,
                            new ServicesActivity.ServiceDescriptor(descriptor));

                mServices.clear();
                mServices.addAll(mServicesMap.values());

                mAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, Disposition.LastExpanded, Theme.Admin);

        mWarning               = getResources().getDrawable(R.drawable.warning);
        mEnabled               = getResources().getDrawable(R.drawable.enabled);
        mDisabled              = getResources().getDrawable(R.drawable.disabled);
        mArrowDown             = getResources().getDrawable(android.R.drawable.arrow_down_float);
        mArrowUp               = getResources().getDrawable(android.R.drawable.arrow_up_float);
        mPhoneDrawable         = getResources().getDrawable(R.drawable.phone);
        mWatchDrawable         = getResources().getDrawable(R.drawable.watch);
        mServiceRunningFormat  = getResources().getString(R.string.alert_service_running);
        mServiceDisabledFormat = getResources().getString(R.string.alert_service_disabled);
        mServiceErrorFormat    = getResources().getString(R.string.alert_service_error);

        setTitle(R.string.screen_services);

        // Button to toggle services
        mServicesButton = addMenuButton(Services.areServicesEnabled(this) ?
                        R.string.action_stop_services : R.string.action_start_services,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            if (Services.areServicesEnabled(ServicesActivity.this)) { // Stop the services
                                Services.enableServices(ServicesActivity.this, false);
                                DeviceInterface.toggleWatchServices(ServicesActivity.this, false);
                                Services.stopServices();
                                onServicesStopped();
                            } else { // Start the services
                                Services.enableServices(ServicesActivity.this, true);
                                DeviceInterface.toggleWatchServices(ServicesActivity.this, true);
                                StartupService.startServices(ServicesActivity.this);
                                onServicesStarted();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to toggle services: ", e);
                        }
                    }
                });
        PatientProfile patientProfile = Settings.getCurrentPatientProfile(this);
        if (patientProfile == null || !patientProfile.isValid())
            mServicesButton.setVisibility(View.GONE);

        // Service list
        try {
            for (Services.Provider provider : Services.getProviders()) {
                ServiceDescriptor descriptor = new ServiceDescriptor(provider);
                mServicesMap.put(descriptor.service, descriptor);
                mServices.add(descriptor);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to retrieve the list of services: ", e);
        }

        mAdapter = new Adapter();
        mHandler = new Handler();
        mList    = addExpandableList(mAdapter);
        mList.setGroupIndicator(null);

        // Back button
        addFooterButton(R.string.action_back, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goBackToParentActivity(TesterMenuActivity.class);
            }
        });
    }

    /**
     * Method called when the services are stopped. Update the screen header.
     */
    private void onServicesStopped() {
        getHeader().updateStatusIcon(false);
        mServicesButton.setText(R.string.action_start_services);
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Method called when the services are started. Update the screen header.
     */
    private void onServicesStarted() {
        getHeader().updateStatusIcon(true);
        mServicesButton.setText(R.string.action_stop_services);
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Method called when a watch service status update is received.
     */
    private void updateServiceStatus(ServiceId service, ServiceDescriptor.Status status) {
        ServiceDescriptor descriptor;
        synchronized (mServicesMap) {
            descriptor = mServicesMap.get(service);
        }

        if (descriptor == null) {
            Log.e(TAG, String.format("Service for key '%s' doesn't exist!", service));
            return;
        }
        descriptor.status = status;

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    /**
     * Service descriptor class.
     */
    private class ServiceDescriptor extends com.ucsf.core.services.ServiceDescriptor {
        public ServiceDescriptor(com.ucsf.core.services.ServiceDescriptor other) {
            super(other);
        }

        public ServiceDescriptor(Services.Provider provider) {
            super(provider);
        }

        /**
         * Requests a service status, either on the phone or the watch.
         */
        public void updateStatus(final ResponseListener listener) {
            if (service.device == DeviceLocation.PatientWatch) {
                DeviceInterface.requestServiceStatus(ServicesActivity.this, service,
                        new com.ucsf.core.services.DeviceInterface.RequestListener() {
                            @Override
                            public void requestProcessed(Messages.Request request, JSONObject data) throws Exception {
                                updateServiceStatus(service, Status.valueOf(data.getString(Messages.VALUE)));
                                listener.onSuccess();
                            }

                            @Override
                            public void requestTimeout(Messages.Request request) throws Exception {
                                listener.onFailure(String.format("Failed to retrieve service '%s' status!",
                                        service.getName(ServicesActivity.this)), null);
                            }

                            @Override
                            public void requestCancelled(Messages.Request request) throws Exception {
                                listener.onFailure(String.format("Failed to retrieve service '%s' status!",
                                        service.getName(ServicesActivity.this)), null);
                            }
                        });
            } else {
                updateServiceStatus(service, getStatus(Services.getProvider(service)));
                listener.onSuccess();
            }
        }

        /**
         * Requests a service parameter update, either on the phone or the watch.
         */
        public void updateServiceParameter(ServiceParameter parameter, Serializable value) {
            if (service.device == DeviceLocation.PatientWatch) {
                DeviceInterface.requestServiceParameterUpdate(ServicesActivity.this, service,
                        parameter.tag, value);
                parameter.set(value);
            } else {
                Services.Provider provider = Services.getProvider(service);
                boolean isRunning = provider.isServiceRunning();
                boolean isEnabled = provider.isServiceEnabled();
                provider.stop();
                parameter.set(value);
                if (isRunning || (!isEnabled && provider.isServiceEnabled()))
                    provider.start();
                provider.start();
            }
            mAdapter.notifyDataSetChanged();
        }

        /**
         * Requests to reset a service parameter, either on the phone or the watch.
         */
        public void resetServiceParameter(ServiceParameter parameter) {
            if (service.device == DeviceLocation.PatientWatch) {
                DeviceInterface.requestServiceParameterReset(ServicesActivity.this, service,
                        parameter.tag);
                parameter.reset();
            } else {
                Services.Provider provider = Services.getProvider(service);
                boolean isRunning = provider.isServiceRunning();
                boolean isEnabled = provider.isServiceEnabled();
                provider.stop();
                parameter.reset();
                if (isRunning || (!isEnabled && provider.isServiceEnabled()))
                    provider.start();
            }
            mAdapter.notifyDataSetChanged();
        }

        /**
         * Requests a service callback execution, update either on the phone or the watch.
         */
        public void executeServiceCallback(ServiceCallback callback) {
            if (service.device == DeviceLocation.PatientWatch)
                DeviceInterface.requestServiceCallbackExecution(ServicesActivity.this, service,
                        callback.tag);
            else
                callback.execute();
        }

    }

    private class Adapter extends BaseExpandableListAdapter {
        @Override
        public int getGroupCount() {
            return mServices.size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return mServices.get(groupPosition).properties.size() + 1;
        }

        @Override
        public Object getGroup(int groupPosition) {
            return mServices.get(groupPosition);
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            ServicesActivity.ServiceDescriptor descriptor =
                    (ServicesActivity.ServiceDescriptor) getGroup(groupPosition);
            if (childPosition == 0)
                return descriptor.service.getDescription(ServicesActivity.this);
            return descriptor.properties.get(childPosition - 1);
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
        public View getGroupView(final int groupPosition, final boolean isExpanded, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater)
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            // Set the rowView and its holder depending on a previous version
            View rowView;
            GroupViewHolder viewHolder;
            if (convertView == null || !(convertView.getTag() instanceof GroupViewHolder)) {
                rowView = inflater.inflate(R.layout.service_cell, parent, false);
                viewHolder = new GroupViewHolder();
                viewHolder.label = (TextView) rowView.findViewById(R.id.label);
                viewHolder.statusIcon = (ImageButton) rowView.findViewById(R.id.icon);
                viewHolder.expandButton = (ImageView) rowView.findViewById(R.id.expand_button);
                viewHolder.deviceIcon = (ImageView) rowView.findViewById(R.id.deviceIcon);

                rowView.setTag(viewHolder);
            } else {
                rowView = convertView;
                viewHolder = (GroupViewHolder) rowView.getTag();
            }

            final ServicesActivity.ServiceDescriptor descriptor =
                    (ServicesActivity.ServiceDescriptor) getGroup(groupPosition);

            viewHolder.label.setText(descriptor.service.getName(ServicesActivity.this));
            viewHolder.deviceIcon.setImageDrawable(
                    descriptor.service.device != DeviceLocation.PatientWatch ?
                            mPhoneDrawable : mWatchDrawable);

            // Update the status icon
            Drawable statusIcon;
            String statusMessageFormat;
            switch (descriptor.status) {
                case Disabled:
                    statusIcon = mDisabled;
                    statusMessageFormat = mServiceDisabledFormat;
                    break;
                case Running:
                    statusIcon = mEnabled;
                    statusMessageFormat = mServiceRunningFormat;
                    break;
                default:
                    statusIcon = mWarning;
                    statusMessageFormat = mServiceErrorFormat;
                    break;
            }
            final String statusMessage = String.format(statusMessageFormat,
                    descriptor.service.getName(ServicesActivity.this));

            viewHolder.statusIcon.setVisibility(View.VISIBLE);
            viewHolder.statusIcon.setImageDrawable(statusIcon);
            viewHolder.statusIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new CustomDialog.Builder(ServicesActivity.this)
                            .setTitle(R.string.screen_services)
                            .setMessage(statusMessage)
                            .addFooterButton(R.string.action_done, null)
                            .show();
                }
            });

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
            if (convertView == null || !(convertView.getTag() instanceof ChildViewHolder)) {
                rowView = inflater.inflate(R.layout.service_parameter_cell, parent, false);
                viewHolder = new ChildViewHolder();
                viewHolder.description = (TextView) rowView.findViewById(R.id.description);
                viewHolder.description.getBackground().setColorFilter(
                        getViewTheme().getBackgroundColor(ServicesActivity.this),
                        PorterDuff.Mode.SRC_ATOP);
                viewHolder.label = (TextView) rowView.findViewById(R.id.label);
                viewHolder.checkBox = (CheckBox) rowView.findViewById(R.id.check_box);
                viewHolder.timeField = (TimeLabel) rowView.findViewById(R.id.time);
                viewHolder.button = (TextView) rowView.findViewById(R.id.button);
                viewHolder.button.getBackground().setColorFilter(
                        getViewTheme().getBackgroundColor(ServicesActivity.this),
                        PorterDuff.Mode.SRC_ATOP);
                viewHolder.resetButton = (ImageButton) rowView.findViewById(R.id.resetButton);

                rowView.setTag(viewHolder);
            } else {
                rowView = convertView;
                viewHolder = (ChildViewHolder) rowView.getTag();
            }

            final ServicesActivity.ServiceDescriptor descriptor =
                    (ServicesActivity.ServiceDescriptor) getGroup(groupPosition);
            if (childPosition == 0) {
                viewHolder.description.setVisibility(View.VISIBLE);
                viewHolder.description.setText(descriptor.service.getDescription(ServicesActivity.this));

                viewHolder.label.setVisibility(View.GONE);
                viewHolder.checkBox.setVisibility(View.GONE);
                viewHolder.timeField.setVisibility(View.GONE);
                viewHolder.button.setVisibility(View.GONE);
                viewHolder.resetButton.setVisibility(View.GONE);
            } else {
                final ServiceProperty property =
                        (ServiceProperty) getChild(groupPosition, childPosition);

                if (property instanceof ServiceParameter) {
                    final ServiceParameter parameter = (ServiceParameter) property;
                    final Serializable value = parameter.get();
                    final boolean isDefault = parameter.isDefault();

                    viewHolder.description.setVisibility(View.GONE);
                    if (value instanceof Boolean) {
                        viewHolder.label.setVisibility(View.VISIBLE);
                        viewHolder.label.setText(parameter.description);
                        viewHolder.timeField.setVisibility(View.GONE);
                        viewHolder.checkBox.setVisibility(View.VISIBLE);
                        viewHolder.checkBox.setChecked((Boolean) value);
                        viewHolder.checkBox.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                descriptor.updateServiceParameter(parameter, viewHolder.checkBox.isChecked());
                            }
                        });
                        viewHolder.button.setVisibility(View.GONE);
                        viewHolder.resetButton.setVisibility(isDefault ? View.INVISIBLE : View.VISIBLE);
                        viewHolder.resetButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                descriptor.resetServiceParameter(parameter);
                            }
                        });
                    } else if (value instanceof Long || value instanceof Integer) {
                        final long lvalue = value instanceof Long ? (Long) value : (Integer) value;
                        viewHolder.label.setVisibility(View.VISIBLE);
                        viewHolder.label.setText(parameter.description);
                        viewHolder.checkBox.setVisibility(View.GONE);
                        viewHolder.timeField.setVisibility(View.VISIBLE);
                        viewHolder.timeField.setValue(lvalue);
                        viewHolder.timeField.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                final TimePickerDialog timePicker = new TimePickerDialog(
                                        ServicesActivity.this, lvalue);
                                timePicker.setOnDoneListener(new Runnable() {
                                    @Override
                                    public void run() {
                                        descriptor.updateServiceParameter(parameter, timePicker.getValue());
                                    }
                                });
                                timePicker.show();
                            }
                        });
                        viewHolder.button.setVisibility(View.GONE);
                        viewHolder.resetButton.setVisibility(isDefault ? View.INVISIBLE : View.VISIBLE);
                        viewHolder.resetButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                descriptor.resetServiceParameter(parameter);
                            }
                        });
                    }
                } else if (property instanceof ServiceCallback) {
                    final ServiceCallback callback = (ServiceCallback) property;

                    viewHolder.description.setVisibility(View.GONE);
                    viewHolder.label.setVisibility(View.GONE);
                    viewHolder.timeField.setVisibility(View.GONE);
                    viewHolder.checkBox.setVisibility(View.GONE);
                    viewHolder.button.setVisibility(View.VISIBLE);
                    viewHolder.button.setText(callback.description);
                    viewHolder.button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            descriptor.executeServiceCallback(callback);
                        }
                    });
                    viewHolder.resetButton.setVisibility(View.GONE);
                }
            }

            return rowView;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return false;
        }

        private class GroupViewHolder {
            TextView    label;
            ImageButton statusIcon;
            ImageView   expandButton;
            ImageView   deviceIcon;
        }

        private class ChildViewHolder {
            TextView    description;
            TextView    label;
            CheckBox    checkBox;
            TimeLabel   timeField;
            TextView    button;
            ImageButton resetButton;
        }
    }
}
