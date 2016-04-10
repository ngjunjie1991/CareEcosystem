package com.ucsf.wear.services;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
//import android.util.Pair;

import com.ucsf.core.data.Settings;
import com.ucsf.wear.sensortag.BarometerSensor;
import com.ucsf.wear.sensortag.HumiditySensor;
import com.ucsf.wear.sensortag.IRTSensor;
import com.ucsf.wear.sensortag.LuxometerSensor;
import com.ucsf.wear.sensortag.MotionSensor;
import com.ucsf.wear.sensortag.Sensor;
import com.ucsf.wear.sensortag.SensorTagConfiguration;
import com.ucsf.wear.sensortag.Pair;
import com.ucsf.wear.sensortag.SensorTagReading;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;
import java.util.Collections;
import java.util.ListIterator;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class SensorTagMonitoring {

    private final static String TAG = "ucsf:SensorTagMonitor";
    private static BluetoothAdapter mBluetoothAdapter;
    private static Handler mHandler = new Handler();

    // Stops scanning after 6 seconds.
    private static final long SCAN_PERIOD = 6000;
    // Performs automatic scans every 1 minutes
    private static final long SCAN_INTERVAL = 60000;
    // Set threshold for sensor inactivity to 2 minutes
    private static final long SENSOR_INACTIVITY_THRESHOLD = 120000;
    // Set threshold for sensor creation to 5 seconds
    private static final long SENSOR_CREATION_THRESHOLD = 5000;
    // Set maximum concurrent connections to sensortags
    private static final int MAX_CONNECTED_SENSORTAGS = 3;
    // Hashmap of connected devices
    private static HashMap<String, BluetoothDevice> mBluetoothDeviceMap = new HashMap<>();
    // Hashmap of GATT services for connected devices
    private static HashMap<String, BluetoothGatt> mBluetoothGattMap = new HashMap<>();
    // Hashmap of targeted devices and the sensors that we wish to retrieve readings from
    private static HashMap<String, SensorTagConfiguration> mBluetoothTargetDevicesMap = new HashMap<>();
    // Hashmap of sensors for a given sensortag device
    private static HashMap<String, ArrayList<Sensor>> mSensorsMap = new HashMap<>();
    // Used to store all available SensorTag connections
    private static PriorityQueue<Pair> mBluetoothScanResults = new PriorityQueue<>();
    // Used to keep track of what SensorTags the app is currently connected to
    private static List<String> mCurrentConnectedBluetooth = new ArrayList<>();
    // Used to keep track of the RSSI of connected SensorTags
    private static List<Pair> mCurrentConnectedRssi= new ArrayList<>();
    // Used to keep track of the current mode, i.e. whether to scan and connect to sensortags
    private static boolean isAutomaticMode = false;
    // App context provided during the creation of the instance
    private static Context mContext;
    // Used to track the listeners to which we can update the sensor readings
    private static final Set<SensorTagListener> mListeners = new HashSet<>();
    //List of connected sensortags pending sensor creation
    private static ArrayList<BluetoothDevice> mPendingSensorCreationList = new ArrayList<>();

    private static int mConnectionsAvailable = MAX_CONNECTED_SENSORTAGS;
    private int waitForRssiCallback = 0;

    // Actions.
    public final static String ACTION_GATT_CONNECTED =
            "com.ucsf.core.services.sensortag.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.ucsf.core.services.sensortag.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.ucsf.core.services.sensortag.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_READ =
            "com.ucsf.core.services.sensortag.ACTION_DATA_READ";
    public final static String ACTION_DATA_NOTIFY =
            "com.ucsf.core.services.sensortag.ACTION_DATA_NOTIFY";
    public final static String ACTION_DATA_WRITE =
            "com.ucsf.core.services.sensortag.ACTION_DATA_WRITE";
    public final static String EXTRA_DATA =
            "com.ucsf.core.services.sensortag.EXTRA_DATA";
    public final static String EXTRA_UUID =
            "com.ucsf.core.services.sensortag.EXTRA_UUID";
    public final static String EXTRA_DEVICEADDRESS =
            "com.ucsf.core.services.sensortag..EXTRA_DEVICEADDRESS";

    /**
     * Broad SensorTag connection workflow
     * 1) Scan devices that are in advertising mode
     * 2) Compare RSSI and connect to the nearest devices based on signal strength
     * 3) Connect to SensorTags, up to the number of connections allowed
     * 5) Retrieve available sensors/services on SensorTag
     * 4) Create sensors after stipulated time delay
     * 5) Start receiving readings
     */

    /**
     * Implements callback methods for GATT events that the app cares about.
     * For example, connection change and services discovered.
     */
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            //String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                //intentAction = ACTION_GATT_CONNECTED;
                Log.i(TAG, "Connected to GATT server.");
                Log.i(TAG, "Attempting to start service discovery");

                // upon connection, start discovering sensors available on the SensorTag
                gatt.discoverServices();

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                //intentAction = ACTION_GATT_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");

                String address = gatt.getDevice().getAddress();
                closeDevice(address);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                /* TODO: uncomment to print debug text
                //boolean isGood = true;
                for (int i = 0; i < gatt.getServices().size(); i++) {
                    BluetoothGattService bgs = gatt.getServices().get(i);
                    //Log.i(TAG, "Found service " + bgs.getUuid().toString());
                    //Log.i(TAG, bgs.getCharacteristics().toString());
                    //if (bgs.getCharacteristics().size() == 0)
                    //    isGood = false;
                }*/

                mPendingSensorCreationList.add(gatt.getDevice());
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic,
                                          int status) {
            //TODO: uncomment onCharacteristicWrite
            //Log.w(TAG, "onCharacteristicWrite received: " + status);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "onCharacteristicRead received: " + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            //TODO: uncommment oncharacteristicchanged log
            //Log.i(TAG, "onCharacteristicChanged received: ");
            //save the sensor reading to persistent storage
            updateSensorReading(characteristic.getValue(), gatt.getDevice().getAddress());
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.i(TAG, "onDescriptorRead received: " + descriptor.getUuid().toString());
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.i(TAG, "onDescriptorWrite received: " + descriptor.getUuid().toString());
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Pair temp = new Pair(gatt.getDevice(), rssi);
                mCurrentConnectedRssi.add(temp);
                //Log.d(TAG, "onReadRemoteRssi:" + temp);
            }
            waitForRssiCallback = 0;
        }

    };

    /**
     * Pass through method to start the automatic monitoring
     * Main code found in setAutomaticMode(boolean)
     */
    public synchronized void startMonitoring(Context context) {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
        }
        mContext = context;
        Log.d(TAG, "startMonitoring()");
        setAutomaticMode(true);
    }

    /**
     * Pass through method to stop the automatic monitoring
     * Main code found in setAutomaticMode(boolean)
     */
    public synchronized void stopMonitoring() {
        Log.d(TAG, "stopMonitoring()");
        setAutomaticMode(false);
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceMap.containsKey(address) && mBluetoothGattMap.containsKey(address)) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            BluetoothGatt gatt = mBluetoothGattMap.get(address);
            if (gatt.connect())
                return true;
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.

        mBluetoothGattMap.put(address, device.connectGatt(mContext, false, mGattCallback));

        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceMap.put(address, device);
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public static void disconnect() {
        // Disconnect all relevant Bluetooth GATT instances.
        if (mBluetoothAdapter == null || mBluetoothGattMap.isEmpty()) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        for (BluetoothGatt gatt : mBluetoothGattMap.values()) {
            gatt.disconnect();
        }
    }


    /**
     * Disconnect the relevant Bluetooth GATT instance.
     * Disconnect preserves the GATT service for use again, and results in a callback for DISCONNECTED
     * @param address MAC address of the sensortag
     */
    public static void disconnectDevice(String address) {

        if (mBluetoothAdapter == null || mBluetoothGattMap.isEmpty()) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        BluetoothGatt gatt = mBluetoothGattMap.get(address);
        gatt.disconnect();
        mCurrentConnectedBluetooth.remove(address);
        mConnectionsAvailable++;
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public static void close() {
        // Close all relevant Bluetooth instances.
        if (mBluetoothGattMap.isEmpty()) {
            return;
        }

        ArrayList<String> addresses = new ArrayList<>();
        for (String address : mBluetoothGattMap.keySet()) {
            addresses.add(address);
        }
        for (String address : addresses) {
            closeDevice(address);
        }
    }

    /**
     * Close the relevant Bluetooth instance.
     * This is one level higher than disconnect and releases all resources (including the GATT service)
     * However, it does not trigger a DISCONNECTED callback
     * @param address MAC address of the sensortag
     */
    public static void closeDevice(String address) {

        if (mBluetoothGattMap.isEmpty() || !mBluetoothGattMap.containsKey(address)) {
            return;
        }

        BluetoothGatt gatt = mBluetoothGattMap.get(address);
        Log.d(TAG, "Closing " + gatt.getDevice().getAddress());
        gatt.close();

        //clear out all existence of the remote device upon closure of the connection
        mBluetoothGattMap.remove(address);
        mBluetoothDeviceMap.remove(address);
        mCurrentConnectedBluetooth.remove(address);
        ArrayList<Sensor> sensors = mSensorsMap.get(address);
        if (sensors != null) {
            for (Sensor sensor : sensors) {
                if (sensor != null) {
                    sensor.disable();
                }
            }
        }
        mSensorsMap.remove(address);
    }

    /**
     * Writes a characteristic.
     */
    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic, String address) {
        return mBluetoothGattMap.get(address).writeCharacteristic(characteristic);
        //return true;
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled, String address) {
        if (mBluetoothAdapter == null || !mBluetoothGattMap.containsKey(address)) {
            Log.w(TAG, address + ": setCharacteristicNotification - BluetoothAdapter not initialized or GATT does not exist");
            return;
        }
        mBluetoothGattMap.get(address).setCharacteristicNotification(characteristic, enabled); // Enabled locally.
    }

    /**
     * Writes the Descriptor for the input characteristic.
     */
    public void writeDescriptor(BluetoothGattCharacteristic characteristic, String address) {
        if (mBluetoothAdapter == null || !mBluetoothGattMap.containsKey(address)) {
            Log.w(TAG, address + ": writeDescriptor - BluetoothAdapter not initialized or GATT does not exist");
            return;
        }
        BluetoothGattDescriptor clientConfig = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
        clientConfig.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGattMap.get(address).writeDescriptor(clientConfig);
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices(String address) {
        if (mBluetoothAdapter == null || !mBluetoothGattMap.containsKey(address)) {
            Log.w(TAG, address + ": getSupportedGattServices - BluetoothAdapter not initialized or GATT does not exist");
            return null;
        }

        return mBluetoothGattMap.get(address).getServices();
    }

    /**
     * Retrieves the service corresponding to the input UUID.
     */
    public BluetoothGattService getService(UUID servUuid, String address) {
        if (mBluetoothAdapter == null || !mBluetoothGattMap.containsKey(address)) {
            Log.w(TAG, address + ": getService: BluetoothAdapter not initialized or GATT does not exist");
            return null;
        }
        return mBluetoothGattMap.get(address).getService(servUuid);
    }

    /**
     * Subsequent sections deal with background scanning and connection to pre-defined BLE sensortags
     */

    /**
     * Runnable to be performed at the start of every scan interval
     */
    private Runnable mStartAutomaticRunnable = new Runnable() {
        @Override
        public void run() {
            //first check whether there are inactive sensortags that needs to be disconnected
            checkSensorTagInactivity();

            //next begin to scan for advertising sensortags
            startAutomaticScan();
        }
    };

    /**
     * Runnable to be performed at the end of every scan interval
     */
    private Runnable mStopAutomaticRunnable = new Runnable() {
        @Override
        public void run() {
            stopAutomaticScan();
        }
    };

    /**
     * Use to create the sensors on the SensorTags.
     * A runnable is used so that we can delay the execution based on a pre-set interval
     * Doing so increases the stability of the connection since immediate creation of sensors
     * after discovering services on the sensortags typically lead to poor connections
     */
    private Runnable mCreateSensorsRunnable = new Runnable() {
        @Override
        public void run() {
            for (BluetoothDevice device : mPendingSensorCreationList) {
                createSensors(device);
            }
        }
    };

    /**
     * This method is called by the runnable at the start of every scan interval
     * It prompts the bluetooth adapter to scan for advertising sensortags
     * The scan results are managed in the callback function: mLeScanCallback
     */
    private void startAutomaticScan() {
        Log.d(TAG, "Scheduled Bluetooth scan started.");
        mPendingSensorCreationList = new ArrayList<>();
        //Scan for Bluetooth devices with specified MAC
        mBluetoothScanResults.clear();
        mCurrentConnectedRssi.clear();
        //noinspection deprecation
        mBluetoothAdapter.startLeScan(mLeScanCallback);
        mHandler.postDelayed(mStopAutomaticRunnable, SCAN_PERIOD);
    }

    /**
     * This method is called by the runnable at the end of every scan interval
     * It also schedules the next scan
     */
    private void stopAutomaticScan() {
        Log.d(TAG, "Scheduled Bluetooth scan stopped.");

        //noinspection deprecation
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
        if (isAutomaticMode) {
            //only schedule next scan if automatic mode is enabled
            //schedules the sensor creation to take place at a later time
            mHandler.removeCallbacks(mStartAutomaticRunnable); //remove any delayed calls to prevent duplicates
            mHandler.postDelayed(mStartAutomaticRunnable, SCAN_INTERVAL);
            mHandler.postDelayed(mCreateSensorsRunnable, SENSOR_CREATION_THRESHOLD);
            Log.d(TAG, "Next Bluetooth scan scheduled.");
        }

        /*
        for (Pair deviceFound: mBluetoothScanResults) {
            Log.d(TAG, deviceFound.toString());
        }*/

        //get RSSI values of currently connected devices
        for (String address : mCurrentConnectedBluetooth) {
            BluetoothGatt gatt = mBluetoothGattMap.get(address);
            waitForRssiCallback = 1;
            gatt.readRemoteRssi();
            while (waitForRssiCallback == 1);
        }

        // if there are still connections available, just connect to highest priority in scan results
        while (mConnectionsAvailable > 0 && !mBluetoothScanResults.isEmpty()) {
            BluetoothDevice deviceToConnect = mBluetoothScanResults.poll().getKey();
            if (mBluetoothTargetDevicesMap.containsKey(deviceToConnect.getAddress())) {
                connectDevice(deviceToConnect);
            }
        }

        // if mCurrentConnectedRssi not populated yet, don't need to compare
        if (mCurrentConnectedRssi.isEmpty()) return;

        int last = Math.min(MAX_CONNECTED_SENSORTAGS, mBluetoothScanResults.size());
        Collections.sort(mCurrentConnectedRssi);
        // iterate through mBluetoothScanResults and connect to devices with stronger RSSI than current
        for (int i = 0; i < last; i++) {
            Pair pairToConnect = mBluetoothScanResults.poll();
            BluetoothDevice deviceToConnect = pairToConnect.getKey();
            Iterator<Pair> it  = mCurrentConnectedRssi.iterator();
            Pair currDevice = it.next();
            while (pairToConnect.getValue() < currDevice.getValue()) {
                try {
                    currDevice = it.next();
                }
                catch (NoSuchElementException e) {
                    currDevice = null;
                    break;
                }
            }
            // if new device has stronger RSSI than curr device
            if (currDevice != null) {
                Pair deviceToDisconnect = mCurrentConnectedRssi.get(mCurrentConnectedRssi.size() - 1);
                Log.d(TAG, "Replacing " + deviceToDisconnect + " with " + pairToConnect);
                int indexToInsert = mCurrentConnectedRssi.indexOf(currDevice);
                disconnectDevice(deviceToDisconnect.getKey().getAddress());
                closeDevice(deviceToDisconnect.getKey().getAddress());
                mCurrentConnectedRssi.remove(deviceToDisconnect);
                if (mBluetoothTargetDevicesMap.containsKey(deviceToConnect.getAddress())) {
                    if (connectDevice(deviceToConnect)) {
                        mCurrentConnectedRssi.add(indexToInsert, pairToConnect);
                    }
                }
            }
        }

    }

    /**
     * Method called by the sensortagservice to start or stop the automatic monitoring code
     *
     * @param mode to enable/disable monitoring mode
     */
    public void setAutomaticMode(boolean mode) {
        //isAutomaticMode = mode;

        //If the request is to stop the automatic monitoring
        if (!mode) {

            isAutomaticMode = mode;
            Log.d(TAG, "Automatic mode stopped.");
            stopAutomaticScan();

            //prevent any previously scheduled scan from starting
            mHandler.removeCallbacks(mStartAutomaticRunnable);

            //disconnect and close all sensortag connections
            disconnect();
            close();

        } else

        //If the request is to start the automatic monitoring
        {
            //if currently not in automatic mode, ok to start the mode
            //ignore if we're already in automatic mode
            if (!isAutomaticMode) {
                isAutomaticMode = mode;

                //first retrieve sensortag configurations settings
                //this gives us the list of targeted sensortags and the respective desired sensor types
                String sensorTagIds = Settings.getCurrentSensortagIdGroup(mContext);
                String sensorTypes = Settings.getCurrentSensortagTypeGroup(mContext);
                Log.d(TAG, "SensorTags: " + sensorTagIds);
                Log.d(TAG, "SensorTag Types: " + sensorTypes);

                String[] sensorTagIdList = sensorTagIds.split("/");
                String[] sensorTypeList = sensorTypes.split("/");

                int i = 0;
                for (String id : sensorTagIdList) {
                    SensorTagConfiguration.SensorType tempSensorType = null;
                    switch (sensorTypeList[i]) {
                        case "Accelerometer":
                            tempSensorType = SensorTagConfiguration.SensorType.MOTION;
                            break;
                        case "Gyroscope":
                            tempSensorType = SensorTagConfiguration.SensorType.MOTION;
                            break;
                        case "Magnetometer":
                            tempSensorType = SensorTagConfiguration.SensorType.MOTION;
                            break;
                        case "Pressure":
                            tempSensorType = SensorTagConfiguration.SensorType.PRESSURE;
                            break;
                        case "Humidity":
                            tempSensorType = SensorTagConfiguration.SensorType.HUMIDITY;
                            break;
                        case "Ambient Temp":
                            tempSensorType = SensorTagConfiguration.SensorType.TEMPERATURE;
                            break;
                        case "Object Temp":
                            tempSensorType = SensorTagConfiguration.SensorType.TEMPERATURE;
                            break;
                        case "Brightness":
                            tempSensorType = SensorTagConfiguration.SensorType.BRIGHTNESS;
                            break;
                        default:
                            break;
                    }
                    SensorTagConfiguration tempConfig = new SensorTagConfiguration();
                    if (mBluetoothTargetDevicesMap.containsKey(id)) {
                        tempConfig = mBluetoothTargetDevicesMap.get(id);
                        tempConfig.addSensorType(tempSensorType);
                        mBluetoothTargetDevicesMap.put(id, tempConfig);
                    } else {
                        tempConfig.addSensorType(tempSensorType);
                        mBluetoothTargetDevicesMap.put(id, tempConfig);
                    }
                    i++;
                }

                Log.d(TAG, "Automatic mode started.");
                startAutomaticScan();
            }
        }
    }

    /**
     * Device scan callback.
     * This is called every time a bluetooth scan for advertising devices obtains a result
     */
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {

            // Check if the device is a SensorTag.
            String deviceName = device.getName();
            if (deviceName == null)
                return;
            if (!(deviceName.equals("SensorTag") || deviceName.equals("TI BLE Sensor Tag") || deviceName.equals("CC2650 SensorTag")))
                return;

            //Log.d(TAG, device.getAddress() + " found. RSSI: " + rssi);

            Pair temp = new Pair(device,rssi);
            if (mBluetoothScanResults.contains(temp)) {
                mBluetoothScanResults.remove(temp);
            }
            mBluetoothScanResults.add(temp);
        }
    };


    /**
     * Connect to the specific bluetooth device, i.e. sensortag
     * @param device sensortag bluetooth device
     * @return successful connection?
     */
    public boolean connectDevice(BluetoothDevice device) {

        //first check whether we are still in monitoring mode and whether the device
        //is in the list of targeted sensortags
        if (isAutomaticMode && mBluetoothTargetDevicesMap.containsKey(device.getAddress())) {
            if (connect(device.getAddress())) {
                mCurrentConnectedBluetooth.add(device.getAddress());
                mConnectionsAvailable --;
                Log.d(TAG, "New SensorTag connected - " + device.getAddress());
                return true;
            }
        }
        return false;
    }

    /**
     * Create sensors for the specific bluetooth device, i.e. sensortag
     * @param device sensortag bluetooth device
     */
    public void createSensors(BluetoothDevice device) {
        if (isAutomaticMode && mBluetoothTargetDevicesMap.containsKey(device.getAddress())) {
            ArrayList<Sensor> sensors = new ArrayList<>();
            String address = device.getAddress();

            for (BluetoothGattService service : getSupportedGattServices(address)) {
                Sensor sensor = null;
                for (SensorTagConfiguration.SensorType sensorType : mBluetoothTargetDevicesMap.get(address).getSensorTypes()) {
                    //Log.d(TAG, "GATT Service UUID - " + service.getUuid().toString() + " - " + address);
                    if (sensorType == SensorTagConfiguration.SensorType.TEMPERATURE && "f000aa00-0451-4000-b000-000000000000".equals(service.getUuid().toString())) {
                        sensor = new IRTSensor(service.getUuid(), this, address);
                        //Log.d(TAG, "New IRT sensor created - " + address);
                    } else if (sensorType == SensorTagConfiguration.SensorType.HUMIDITY && "f000aa20-0451-4000-b000-000000000000".equals(service.getUuid().toString())) {
                        sensor = new HumiditySensor(service.getUuid(), this, address);
                        //Log.d(TAG, "New humidity sensor created - " + address);
                    } else if (sensorType == SensorTagConfiguration.SensorType.PRESSURE && "f000aa40-0451-4000-b000-000000000000".equals(service.getUuid().toString())) {
                        sensor = new BarometerSensor(service.getUuid(), this, address);
                        //Log.d(TAG, "New barometer sensor created - " + address);
                    } else if (sensorType == SensorTagConfiguration.SensorType.BRIGHTNESS && "f000aa70-0451-4000-b000-000000000000".equals(service.getUuid().toString())) {
                        sensor = new LuxometerSensor(service.getUuid(), this, address);
                        //Log.d(TAG, "New luxometer sensor created - " + address);
                    } else if (sensorType == SensorTagConfiguration.SensorType.MOTION && "f000aa80-0451-4000-b000-000000000000".equals(service.getUuid().toString())) {
                        sensor = new MotionSensor(service.getUuid(), this, address);
                        //Log.d(TAG, "New motion sensor created - " + address);
                    }
                }
                if (sensor != null) {
                    sensors.add(sensor);
                }
            }

            mSensorsMap.put(address, sensors);
        }
    }

     /**
     * Post the sensor readings to the listener, in order for it to be saved to persistent storage
     * @param value raw reading to be converted into readable format
     * @param deviceAddress sensortag address
     */
    private static void updateSensorReading(byte[] value, String deviceAddress) {

        if (isAutomaticMode) {

            ArrayList<Sensor> sensors = mSensorsMap.get(deviceAddress);
            if (sensors != null) {
                for (Sensor s : sensors) {
                    s.receiveNotification();
                    s.convert(value);
                    s.getStatus().setLatestReadingTimestamp(new Date());

                    for (SensorTagListener listener : mListeners)
                        listener.onSensorTagReading(s.getReading());
                }
            }
        }
    }

    /**
     * Checks connected SensorTags regularly to see if readings were retrieved within the last
     * threshold period. If not, assume that the connection is dead and disconnect from it.
     */
    public static void checkSensorTagInactivity() {

        Log.d(TAG,"Checking SensorTag inactivity threshold...");

        Date currentTime = new Date();
        ArrayList<String> inactiveDevices = new ArrayList<>();
        for (String address : mBluetoothDeviceMap.keySet()) {
            boolean inactive = false;

            ArrayList<Sensor> sensors = mSensorsMap.get(address);
            if (sensors != null) {
                for (Sensor sensor : sensors) {
                    if (sensor != null) {

                        Date lastUpdated = sensor.getStatus().getLatestReadingTimestamp();
                        if (currentTime.getTime() - lastUpdated.getTime() > SENSOR_INACTIVITY_THRESHOLD) {
                            //check if the last updated time has elapsed by more than the threshold
                            inactive = true;
                            Log.d(TAG,sensor.getAddress() + " inactive for " + (currentTime.getTime() - lastUpdated.getTime())/1000 + "s" );
                            break;
                        }
                    } else {
                        //unlikely to be null since we created it previously.
                        //but set to inactive as a failsafe if it does occur so we can reconnect properly
                        inactive = true;
                    }
                }
            }
            if (inactive) {
                inactiveDevices.add(address);
            }
        }

        for (String address : inactiveDevices) {
            //close the bluetooth connection if sensortag has been inactive beyond desired threshold
            //upon closing, the sensortag should enter advertising mode, giving us the chance to reconnect to it
            disconnectDevice(address);
            closeDevice(address);
        }
    }


    /**
     * Adds a sensortag listener which is called each time a sensortag reading is received
     */
    public void addSensorTagListener(SensorTagListener listener) {
        synchronized (mListeners) {
            mListeners.add(listener);
        }
    }

    /**
     * Removes a sensortag listener.
     */
    public void removeSensorTagListener(SensorTagListener listener) {
        synchronized (mListeners) {
            mListeners.remove(listener);
        }
    }

    public interface SensorTagListener {
        /**
         * Method called at regular interval.
         *
         * @param readings List of sensortag readings
         */
        void onSensorTagReading(List<SensorTagReading> readings);
    }
}
