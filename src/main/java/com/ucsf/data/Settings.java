package com.ucsf.data;

import android.content.Context;
import android.util.Log;

import com.ucsf.core.data.DataManager;
import com.ucsf.core.data.DataManager.TableField;
import com.ucsf.core.data.DeviceLocation;
import com.ucsf.core.data.Entry;
import com.ucsf.core.data.SharedTables;
import com.ucsf.core.services.UploaderService;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Set;

/**
 * Class storing the application settings, as the patient profile, ...
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public class Settings extends com.ucsf.core.data.Settings {
    private static final String TAG                    = "ucsf:Settings";
    private static final String PATIENT_PROFILES_TABLE = "patient_profiles";
    private static final String ROOMS_TABLE            = "rooms";
    private static final String KEY_PARAMETER          = "parameter";
    private static final String KEY_VALUE              = "value";
    private static final String KEY_VALIDATION_STEP    = "VALIDATION_STEP";
    private static final String KEY_IS_HOME_ACQUIRED   = "IS_HOME_ACQUIRED";
    private static final String KEY_REGISTERED         = "REGISTERED";

    private static HashMap<String, PatientProfile> mPatientProfiles      = null;
    private static DataManager.Table               mPatientProfilesTable = null;
    private static DataManager.Table               mRoomsTable           = null;
    private static DataManager.Table               mLogsTable            = null;
    private static Settings                        mInstance             = null;

    protected Settings(Context context) {
        super(context);
    }

    /**
     * Returns the database table storing the patients profiles.
     */
    private static DataManager.Table getPatientProfilesTable(DataManager instance)
            throws Exception
    {
        if (mPatientProfilesTable == null)
            mPatientProfilesTable = instance.createTable(
                    PATIENT_PROFILES_TABLE,
                    DeviceLocation.Unknown,
                    new TableField(DataManager.KEY_PATIENT_ID, DataManager.Type.Text),
                    new TableField(KEY_PARAMETER, DataManager.Type.Text),
                    new TableField(KEY_VALUE, DataManager.Type.Text)
            );
        return mPatientProfilesTable;
    }

    /**
     * Returns the database table storing the rooms profiles.
     */
    private static DataManager.Table getRoomsTable(DataManager instance) throws Exception {
        if (mRoomsTable == null)
            mRoomsTable = instance.createTable(
                    ROOMS_TABLE,
                    DeviceLocation.Unknown,
                    new TableField(DataManager.KEY_PATIENT_ID, DataManager.Type.Text),
                    new TableField(PatientProfile.KEY_ROOM_IDX, DataManager.Type.Integer),
                    new TableField(KEY_PARAMETER, DataManager.Type.Text),
                    new TableField(KEY_VALUE, DataManager.Type.Text)
            );
        return mRoomsTable;
    }

    /**
     * Returns the database table storing the warning and errors longs.
     */
    public static DataManager.Table getPhoneLogsTable(DataManager instance) throws Exception {
        if (mLogsTable == null)
            mLogsTable = UploaderService.addTable(
                    instance,
                    "phone_logs",
                    DeviceLocation.PatientPhone,
                    SharedTables.Logs.getTable(instance).fields
            );
        return mLogsTable;
    }

    /**
     * Returns the settings instance.
     */
    public static Settings getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new Settings(context);
        }
        return mInstance;
    }

    /**
     * Returns all registered patients' id.
     */
    public static Set<String> getPatientIDs(Context context) {
        if (mPatientProfiles == null)
            loadPatientProfiles(context);
        return mPatientProfiles.keySet();
    }

    /**
     * Get the current patient profile.
     */
    public static PatientProfile getCurrentPatientProfile(Context context) {
        return getPatientProfile(context, getCurrentUserId(context));
    }

    /**
     * Returns the patient profile corresponding to the given id.
     */
    public static PatientProfile getPatientProfile(Context context, String patientID) {
        if (mPatientProfiles == null)
            loadPatientProfiles(context);
        return mPatientProfiles.get(patientID);
    }

    /**
     * Updates the given patient profile.
     */
    public static void updatePatientProfile(Context context, PatientProfile profile) {
        if (mPatientProfiles == null)
            loadPatientProfiles(context);
        mPatientProfiles.put(profile.patientId, profile);

        try (DataManager instance = DataManager.get(context)) {
            // Save the patient properties
            savePatientParameter(instance, profile.patientId, PatientProfile.KEY_USERNAME       , profile.username);
            savePatientParameter(instance, profile.patientId, PatientProfile.KEY_TALLEST_CEILING, profile.tallestCeilingHeight);
            savePatientParameter(instance, profile.patientId, PatientProfile.KEY_HOME_LATITUDE  , profile.homeLatitude);
            savePatientParameter(instance, profile.patientId, PatientProfile.KEY_HOME_LONGITUDE , profile.homeLongitude);
            savePatientParameter(instance, profile.patientId, KEY_VALIDATION_STEP               , profile.validationStep);
            savePatientParameter(instance, profile.patientId, KEY_IS_HOME_ACQUIRED              , profile.isHomeAcquired);
            savePatientParameter(instance, profile.patientId, KEY_REGISTERED                    , profile.registered);
            savePatientParameter(instance, profile.patientId, PatientProfile.KEY_START_TIMESTAMP, profile.setupStartTimestamp);
            savePatientParameter(instance, profile.patientId, PatientProfile.KEY_END_TIMESTAMP  , profile.setupEndTimestamp);

            // Save the patient rooms
            getRoomsTable(instance).erase(new DataManager.Condition.Equal<>(
                    DataManager.KEY_PATIENT_ID, profile.patientId));
            if (profile.rooms != null) {
                int idx = 0;
                for (PatientProfile.Room room : profile.rooms) {
                    saveRoomParameter(instance, profile.patientId, idx, PatientProfile.KEY_MOTE_ID         , room.getMoteId());
                    saveRoomParameter(instance, profile.patientId, idx, PatientProfile.KEY_ROOM_NAME       , room.getRoomName());
                    saveRoomParameter(instance, profile.patientId, idx, PatientProfile.KEY_CEILING_HEIGHT  , room.getHeight());
                    saveRoomParameter(instance, profile.patientId, idx, PatientProfile.KEY_FLOOR           , room.getFloor());
                    saveRoomParameter(instance, profile.patientId, idx, PatientProfile.KEY_X_DIST_FROM_PREV, room.getXDistanceFromPrevious());
                    saveRoomParameter(instance, profile.patientId, idx, PatientProfile.KEY_Y_DIST_FROM_PREV, room.getYDistanceFromPrevious());
                    ++idx;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to save patient profile: ", e);
        }
    }

    /**
     * Loads all the stored patients profiles.
     */
    private static void loadPatientProfiles(Context context) {
        mPatientProfiles = new HashMap<>();
        try (DataManager instance = DataManager.get(context)) {
            // Load the patient profiles
            DataManager.Cursor cursor = getPatientProfilesTable(instance).fetch();
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String patientID = cursor.getString(DataManager.KEY_PATIENT_ID);
                    if (patientID != null && !patientID.isEmpty()) {
                        // Get or create the patient profile
                        PatientProfile profile = mPatientProfiles.get(patientID);
                        if (profile == null) {
                            profile = new PatientProfile(patientID);
                            mPatientProfiles.put(patientID, profile);
                        }

                        switch (cursor.getString(KEY_PARAMETER)) {
                            case PatientProfile.KEY_USERNAME:
                                profile.username = cursor.getString(KEY_VALUE);
                                break;
                            case PatientProfile.KEY_TALLEST_CEILING:
                                profile.tallestCeilingHeight = cursor.getDouble(KEY_VALUE);
                                break;
                            case PatientProfile.KEY_HOME_LATITUDE:
                                profile.homeLatitude = cursor.getDouble(KEY_VALUE);
                                break;
                            case PatientProfile.KEY_HOME_LONGITUDE:
                                profile.homeLongitude = cursor.getDouble(KEY_VALUE);
                                break;
                            case KEY_VALIDATION_STEP:
                                profile.validationStep = cursor.getInt(KEY_VALUE);
                                break;
                            case KEY_IS_HOME_ACQUIRED:
                                profile.isHomeAcquired = cursor.getBoolean(KEY_VALUE);
                                break;
                            case KEY_REGISTERED:
                                profile.registered = cursor.getBoolean(KEY_VALUE);
                                break;
                            case PatientProfile.KEY_START_TIMESTAMP:
                                profile.setupStartTimestamp = cursor.getString(KEY_VALUE);
                                break;
                            case PatientProfile.KEY_END_TIMESTAMP:
                                profile.setupEndTimestamp = cursor.getString(KEY_VALUE);
                                break;
                        }
                    }
                } while (cursor.moveToNext());
            }

            // Load the patients' rooms
            for (PatientProfile patientProfile : mPatientProfiles.values()) {
                cursor = getRoomsTable(instance).fetch(new DataManager.Condition.Equal<>(
                        DataManager.KEY_PATIENT_ID, patientProfile.patientId));
                int validationStep = patientProfile.validationStep;
                boolean isHomeAcquired = patientProfile.isHomeAcquired;
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        int idx = cursor.getInt(PatientProfile.KEY_ROOM_IDX);

                        // Make sure that the room array has the right size
                        if (patientProfile.rooms == null || idx >= patientProfile.rooms.length) {
                            int count = idx + 1;
                            PatientProfile.Room[] newRooms = new PatientProfile.Room[count];
                            count = Math.min(count, patientProfile.rooms == null ? 0 :
                                    patientProfile.rooms.length);

                            if (count > 0)
                                System.arraycopy(patientProfile.rooms, 0, newRooms, 0, count);
                            for (int i = count; i < newRooms.length; ++i)
                                newRooms[i] = patientProfile.new Room();

                            patientProfile.rooms = newRooms;
                        }
                        PatientProfile.Room room = patientProfile.rooms[idx];

                        switch (cursor.getString(KEY_PARAMETER)) {
                            case PatientProfile.KEY_MOTE_ID:
                                room.setMoteId(cursor.getString(KEY_VALUE));
                                break;
                            case PatientProfile.KEY_ROOM_NAME:
                                room.setRoomName(cursor.getString(KEY_VALUE));
                                break;
                            case PatientProfile.KEY_CEILING_HEIGHT:
                                room.setHeight(cursor.getDouble(KEY_VALUE));
                                break;
                            case PatientProfile.KEY_FLOOR:
                                room.setFloor(cursor.getInt(KEY_VALUE));
                                break;
                            case PatientProfile.KEY_X_DIST_FROM_PREV:
                                room.setXDistanceFromPrevious(cursor.getDouble(KEY_VALUE));
                                break;
                            case PatientProfile.KEY_Y_DIST_FROM_PREV:
                                room.setYDistanceFromPrevious(cursor.getDouble(KEY_VALUE));
                                break;
                        }
                    } while (cursor.moveToNext());
                }
                patientProfile.validationStep = validationStep;
                patientProfile.isHomeAcquired = isHomeAcquired;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load settings: ", e);
        }
    }

    /**
     * Saves the given patient profile parameter.
     */
    private static void savePatientParameter(DataManager instance, String patientId, String key,
                                             Serializable value) throws Exception
    {
        if (value == null)
            return;

        getPatientProfilesTable(instance).fetchAndAdd(new Entry[]{
                        new Entry(DataManager.KEY_PATIENT_ID, patientId),
                        new Entry(KEY_PARAMETER             , key)
                },
                new Entry(KEY_VALUE, value)
        );
    }

    /**
     * Saves the given room profile parameter.
     */
    private static void saveRoomParameter(DataManager instance, String patientId, int idx,
                                          String key, Serializable value) throws Exception
    {
        if (value == null)
            return;

        getRoomsTable(instance).add(
                new Entry(DataManager.KEY_PATIENT_ID , patientId),
                new Entry(PatientProfile.KEY_ROOM_IDX, idx),
                new Entry(KEY_PARAMETER              , key),
                new Entry(KEY_VALUE                  , value)
        );
    }

    @Override
    public DataManager.Table getLogsTable(DataManager instance) throws Exception {
        return getPhoneLogsTable(instance);
    }

}
