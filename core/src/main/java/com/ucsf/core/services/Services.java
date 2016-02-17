package com.ucsf.core.services;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.ucsf.core.R;
import com.ucsf.core.data.DataManager;
import com.ucsf.core.data.Entry;
import com.ucsf.core.data.Settings;
import com.ucsf.core.data.PersistentParameter;
import com.ucsf.core.data.SharedTables;
import com.ucsf.core.data.Timestamp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static java.lang.System.exit;

/**
 * Definitions of methods and class to homogeneize calls to services.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public abstract class Services {
    private static final String                       TAG              = "ucsf:Services";
    private static final Map<ServiceId, Provider>     mProviders       = new HashMap<>();
    private static final Pattern                      mLogPattern;
    private static       boolean                      mLogsInitialized = false;
    private static final PersistentParameter<Boolean> mServicesEnabled =
            new PersistentParameter<>("SERVICES_ENABLED", false);

    static {
        Pattern pattern = null;
        try {
            pattern = Pattern.compile("[DIWE]/ucsf:.*");
        } catch (Exception e) {
            Log.wtf(TAG, "Failed to parse log pattern: ", e);
            exit(-1);
        }
        mLogPattern = pattern;
    }

    /**
     * Returns the service provider for the given unique id. Because the underlying map is not
     * consistent through different entry points, this method can return null even if the service
     * exists.
     */
    public static Provider getProvider(ServiceId id) {
        return mProviders.get(id);
    }

    /**
     * Returns the registered service providers. Because the underlying map is not consistent
     * through different entry points, this method can return only a subset of all the running
     * services.
     */
    public static Collection<Provider> getProviders() {
        return mProviders.values();
    }

    /**
     * Starts the given services.
     */
    public static void startServices(final Settings settings, Provider... providers) {
        final String servicesStartedFormat = settings.context.getString(R.string.toast_service_started);
        final String servicesFailureFormat = settings.context.getString(R.string.toast_service_failure);

        saveLogs(settings);
        for (final Provider provider : providers) {
            provider.start(new ResponseListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(settings.context, String.format(servicesStartedFormat,
                            provider.getServiceName()), Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(String error, Throwable e) {
                    Toast.makeText(settings.context, String.format(servicesFailureFormat,
                            provider.getServiceName()), Toast.LENGTH_LONG).show();

                    Log.e(TAG, String.format("Failed to start service '%s': %s",
                            provider.getServiceName(), error), e);
                }
            });
        }
    }

    /**
     * Indicates if services are enables, i.e. if they are launched on startup.
     */
    public static boolean areServicesEnabled(Context context) {
        return mServicesEnabled.get(context);
    }

    /**
     * Enables or disables services.
     */
    public static void enableServices(Context context, boolean enabled) {
        mServicesEnabled.set(context, enabled);
    }


    /**
     * Stops all the running services.
     */
    public static void stopServices() throws Exception {
        for (Provider provider : mProviders.values())
            provider.stop();
    }

    /**
     * Dumps warnings and errors logs to a temporary file and save such previous files in the
     * database.
     */
    private static void saveLogs(Settings settings) {
        if (mLogsInitialized)
            return;

        mLogsInitialized = true;

        /**
         * The logs are too big to be handled using our pipeline so we disabled them. It's still
         * possible to use them if necessary by uncommenting the following block.
         * /

        // Get the logs directory and create it if needed
        File logDirectory = settings.context.getDir("logs", Context.MODE_PRIVATE);

        // Store the previous logs to the database
        try (DataManager instance = DataManager.get(settings.context)) {
            for (File logFile : logDirectory.listFiles()) {
                try {
                    // Read the file
                    StringBuilder sb = new StringBuilder();
                    try (BufferedReader br = new BufferedReader(new FileReader(logFile))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            if (mLogPattern.matcher(line).find()) {
                                sb.append(line);
                                sb.append(System.lineSeparator());
                            }
                        }
                    }

                    settings.getLogsTable(instance).add(
                            new Entry(DataManager.KEY_TIMESTAMP, Timestamp.getTimestamp()),
                            new Entry(DataManager.KEY_PATIENT_ID, Settings.getCurrentUserId(settings.context)),
                            new Entry(SharedTables.Logs.KEY_LOG, sb.toString())
                    );

                    if (!logFile.delete())
                        Log.e(TAG, String.format("Failed to delete log file '%s': ", logFile.getName()));
                } catch (Exception e) {
                    Log.e(TAG, String.format("Failed to read log file '%s': ", logFile.getName()), e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to save log files to the database: ", e);
        }

        // Dump the log to a file
        try {
            File filename = new File(String.format("%s/%s", logDirectory.getPath(), Timestamp.getTimestamp()));
            if (!filename.createNewFile())
                Log.e(TAG, "Failed to create log file!");
            else {
                String[] cmd = new String[]{"logcat", "-f", filename.getAbsolutePath(), "*:W"};
                Runtime.getRuntime().exec(cmd);
            }
            mLogsInitialized = true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to dump logs: ", e);
        }
         //*/
    }

    /**
     * Service provider interface. A service provider is responsible of managing the life cycle of
     * an event, i.e. starting and stoping it. It also provides the services parameters.
     */
    public static abstract class Provider {
        public  final ServiceId                    id;
        public  final Context                      context;
        private final Map<String, ServiceProperty> mProperties     = new HashMap<>();
        private final List<ServiceProperty>        mPropertiesList = new ArrayList<>();
        private final ServiceParameter<Boolean>    mIsEnabled;

        /**
         *  Wrapper around a {@link PersistentParameter} to not have to pass the context all
         * the time.
         */
        public class PersistentParameter<T extends Serializable>
                extends com.ucsf.core.data.PersistentParameter<T>
        {
            public PersistentParameter(String tag, @NonNull T defaultValue) {
                super(tag, defaultValue);
            }

            public PersistentParameter(String tag, @NonNull DefaultValue<T> defaultValue) {
                super(tag, defaultValue);
            }

            /** Returns the parameter value. */
            public T get() {
                return super.get(context);
            }

            /** Sets the parameter value. */
            public void set(T value) {
                super.set(context, value);
            }
        }

        protected Provider(Context context, ServiceId id) {
            mProviders.put(id, this);
            this.context = context.getApplicationContext();
            this.id = id;
            this.mIsEnabled = addParameter("ENABLED", R.string.parameter_enabled, true);
        }

        /**
         * Starts the service managed by this service. This method doesn't check if the service
         * is already running or if it can run at all, therefore don't call this method directly,
         * use {@link Provider#start(ResponseListener listener)} instead.
         */
        protected abstract void startService(ResponseListener listener);

        /**
         * Stops the service managed by this service. This method doesn't check if the service is
         * running, therefore don't call this method directly, use {@link Provider#stop()} instead.
         */
        protected abstract void stopService();

        /**
         * Indicates if the service is currently running.
         */
        public abstract boolean isServiceRunning();

        /**
         * Add a new {@link ServiceParameter service parameter} to the service.
         */
        protected <T extends Serializable> ServiceParameter<T> addParameter(String tag,
                                                                            int descriptionId,
                                                                            T value)
        {
            return addParameter(tag, context.getString(descriptionId), value);
        }

        /**
         * Add a new {@link ServiceParameter service parameter} to the service.
         */
        protected <T extends Serializable> ServiceParameter<T> addParameter(String tag,
                                                                            String description,
                                                                            T value)
        {
            ServiceParameter<T> parameter = (ServiceParameter<T>) mProperties.get(tag);
            if (parameter != null)
                return parameter;
            parameter = new ServiceParameter<>(context, id, tag, description, value);
            mProperties.put(tag, parameter);
            mPropertiesList.add(parameter);
            return parameter;
        }

        /**
         * Add a new {@link ServiceParameter service parameter} to the service which is a copy of
         * the given one.
         */
        protected <T extends Serializable> ServiceParameter<T> addParameter(
                ServiceParameter<T> parameter) {
            return addParameter(parameter.tag, parameter.description, parameter.defaultValue());
        }

        /**
         * Add a new {@link ServiceCallback service callback} to the service.
         */
        protected ServiceCallback addCallback(String tag, int description, String annotation) {
            ServiceCallback callback = (ServiceCallback) mProperties.get(tag);
            if (callback != null)
                return callback;
            callback = new ServiceCallback(context, id, tag, description, annotation);
            mProperties.put(tag, callback);
            mPropertiesList.add(callback);
            return callback;
        }

        /**
         * Modify the default value of a {@link ServiceParameter service parameter} (to call in
         * the constructor of a daughter class).
         */
        protected <T extends Serializable> void setParameterDefaultValue(String tag, T value) {
            ServiceProperty<?> property = mProperties.get(tag);
            if (property == null) {
                Log.e(TAG, String.format("Unknown parameter '%s'!", tag));
                return;
            }
            try {
                com.ucsf.core.services.ServiceParameter<T> serviceParameter =
                        (com.ucsf.core.services.ServiceParameter<T>) property;
                serviceParameter.setDefaultValue(value);
            } catch (Exception e) {
                Log.e(TAG, "Wrong parameter type!");
            }
        }

        /**
         * Returns the registered {@link ServiceProperty service properties}. Properties are
         * registered when they are instantiated through call of
         * {@link Provider#addParameter(String, int, Serializable) addParameter()} or
         * {@link Provider#addCallback(String, int, String) addCallback()}.
         */
        public List<ServiceProperty> getProperties() {
            return mPropertiesList;
        }

        /**
         * Returns the registered {@link ServiceProperty service property} with the given tag.
         * Properties are registered when they are instantiated through call of
         * {@link Provider#addParameter(String, int, Serializable) addParameter()} or
         * {@link Provider#addCallback(String, int, String) addCallback()}.
         */
        public ServiceProperty getProperty(String tag) {
            return mProperties.get(tag);
        }

        /**
         * Returns the registered {@link ServiceParameter service parameter} with the given tag. 
         * Parameters are registered when they are instantiated through call of
         * {@link Provider#addParameter(String, int, Serializable) addParameter()}.
         */
        public <T extends Serializable> ServiceParameter<T> getParameter(String tag) {
            ServiceProperty property = mProperties.get(tag);
            return (property != null && property instanceof ServiceParameter) ? (ServiceParameter<T>) property : null;
        }

        /**
         * Returns the registered {@link ServiceCallback service callback} with the given tag.
         * Callbacks are registered when they are instantiated through call of
         * {@link Provider#addCallback(String, int, String)} addCallback()}.
         */
        public ServiceCallback getCallback(String tag) {
            ServiceProperty property = mProperties.get(tag);
            return (property != null && property instanceof ServiceCallback) ? (ServiceCallback) property : null;
        }

        /**
         *  Returns the service name.
         */
        public String getServiceName() {
            return id.getName(context);
        }

        /**
         * Returns the service description.
         */
        public String getServiceDescription() {
            return id.getDescription(context);
        }

        /**
         * Indicates if the service is currently enabled.
         */
        public boolean isServiceEnabled() {
            return mIsEnabled.get();
        }

        /**
         * Starts the service managed by this provider. The service will not be started if it is
         * already running, not enabled or if services are not enabled. Calls
         * {@link Provider#startService(ResponseListener)}.
         */
        public final void start(ResponseListener listener) {
            //// TODO: 16/2/2016 Undo comments, currently blocked to bypass startup checks

            if (Settings.getCurrentUserId(context).isEmpty())
                listener.onFailure("Settings are not valid!", null);
            else if (isServiceRunning())
                Log.w(TAG, String.format("Service '%s' already running!", getServiceName()));
            else if (!isServiceEnabled())
                listener.onFailure(String.format("Service '%s' not enabled!",
                        getServiceName()), null);
            else
                startService(listener);
        }

        /**
         * Starts the service managed by this provider. The service will not be started if it is
         * already running, not enabled or if services are not enabled. Calls
         * {@link Provider#startService(ResponseListener)}.
         */
        public final void start() {
            start(new ResponseListener() {
                @Override
                public void onSuccess() {
                    Log.i(TAG, String.format("Service '%s' successfully started.",
                            getServiceName()));
                }

                @Override
                public void onFailure(String error, Throwable e) {
                    Log.e(TAG, String.format("Failed to start service '%s': %s",
                            getServiceName(), error), e);
                }
            });
        }

        /**
         * Stops the service managed by this provider. Do nothing if the service is not running.
         * Calls {@link Provider#stopService()}.
         */
        public final void stop() {
            if (isServiceRunning()) {
                stopService();
                Log.d(TAG, String.format("Service '%s' stopped.", getServiceName()));
            }
        }

    }

}
