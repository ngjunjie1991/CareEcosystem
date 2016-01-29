package com.ucsf.data;


import android.location.Location;
import android.location.LocationManager;

import com.estimote.sdk.Beacon;
import com.ucsf.core.data.Mote;
import com.ucsf.core.services.BeaconMonitoring;

/**
 * Class containing patient information, such as id, rooms, ...
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public class PatientProfile implements Cloneable {
    public static final int    VALIDATION_STEP_COUNT = 3;
    public static final String KEY_PROFILE           = "PROFILE";
    public static final String KEY_REGISTRATION_ID   = "reg_id";
    public static final String KEY_ROOMS             = "ROOMS";
    public static final String KEY_TALLEST_CEILING   = "TALLEST_CEILING";
    public static final String KEY_USERNAME          = "USERNAME";
    public static final String KEY_HOME_LATITUDE     = "HOME_LATITUDE";
    public static final String KEY_HOME_LONGITUDE    = "HOME_LONGITUDE";
    public static final String KEY_ROOM_NAME         = "ROOM_NAME";
    public static final String KEY_ROOM_IDX          = "ROOM_IDX";
    public static final String KEY_MOTE_ID           = "MOTE_ID";
    public static final String KEY_CEILING_HEIGHT    = "CEILING_HEIGHT";
    public static final String KEY_FLOOR             = "FLOOR";
    public static final String KEY_X_DIST_FROM_PREV  = "X_DIST_FROM_PREV";
    public static final String KEY_Y_DIST_FROM_PREV  = "Y_DIST_FROM_PREV";
    public static final String KEY_START_TIMESTAMP   = "START";
    public static final String KEY_END_TIMESTAMP     = "END";

    /** Unique patient identifier. The server is responsible of creating and validating them. */
    public final String patientId;

    /** Patient username for easy identification. */
    public       String username;

    /** Height of the tallest ceiling in the house (in feet). */
    public       double tallestCeilingHeight = -1;

    /** GPS latitude of the home location. */
    public       double homeLatitude;

    /** GPS longitude of the home location. */
    public       double homeLongitude;

    /** Timestamp corresponding to the beginning of the home data acquisition (during setup). */
    public       String setupStartTimestamp = "";

    /** Timestamp corresponding to the end of the home data acquisition (during setup). */
    public       String setupEndTimestamp = "";

    /** List of the home rooms with a beacon. */
    public       Room[] rooms = null;

    /** Indicates if the GPS coordinates of the house have been acquired. */
    public       boolean isHomeAcquired = false;

    /**
     * Current validation steps. When all the rooms have been acquired, this value is
     * {@link PatientProfile#rooms rooms}.length * {@link PatientProfile#VALIDATION_STEP_COUNT}.
     */
    public       int validationStep = 0;

    /** Indicates if the profile data have been successfully sent to the server. */
    public       boolean registered = false;

    public PatientProfile(String patientId) {
        this.patientId = patientId;
    }

    public PatientProfile(PatientProfile other) {
        this(other.patientId);
        other.copyTo(this);
    }

    /**
     * Check if the room is valid.
     */
    public static RoomStatus isRoomValid(Room room, Room[] rooms) {
        if (room.getMoteId() == null || room.getMoteId().isEmpty())
            return RoomStatus.InvalidBeaconId;
        if (room.getRoomName() == null || room.getRoomName().isEmpty())
            return RoomStatus.InvalidRoomName;
        if (room.getHeight() <= 0)
            return RoomStatus.InvalidCeilingHeight;

        for (Room r : rooms) {
            if (r != room) {
                if (r.getRoomName().equals(room.getRoomName()))
                    return RoomStatus.InvalidRoomName;
                if (r.getMoteId().equals(room.getMoteId()))
                    return RoomStatus.InvalidBeaconId;
            }
        }

        return RoomStatus.Valid;
    }

    /**
     * Set the room count. Tries to keep previous defined rooms.
     */
    public void setRoomCount(int count) {
        if (rooms != null && count == rooms.length)
            return;

        resetHouseSetup();
        Room[] newRooms = new Room[count];
        count = Math.min(count, rooms == null ? 0 : rooms.length);

        if (count > 0)
            System.arraycopy(rooms, 0, newRooms, 0, count);
        for (int i = count; i < newRooms.length; ++i)
            newRooms[i] = new Room();

        rooms = newRooms;
    }

    /**
     * Get the room corresponding to the given beacon.
     */
    public Room getRoom(Beacon beacon) {
        if (beacon == null)
            return null;

        for (Room room : rooms)
            if (BeaconMonitoring.getBeaconUniqueId(beacon).equals(room.getMoteId()))
                return room;

        return null;
    }

    /**
     * Indicates if the profile is valid.
     */
    public boolean isValid() {
        for (Stage stage : Stage.values()) {
            if (!isValid(stage))
                return false;
        }
        return true;
    }

    /**
     * Indicates if the given stage of profile initialization is valid.
     */
    public boolean isValid(Stage stage) {
        switch (stage) {
            case PatientId:
                return patientId != null && !patientId.isEmpty();
            case PatientUsername:
                return username != null && !username.isEmpty();
            case RoomCount:
                return rooms != null && rooms.length > 0;
            case TallestCeiling:
                return tallestCeilingHeight > 0;
            case RoomSetup:
                for (Room room : rooms) {
                    if (isRoomValid(room, rooms) != RoomStatus.Valid)
                        return false;
                }
                return true;
            case MoteValidation:
                return isHomeAcquired && validationStep == rooms.length * VALIDATION_STEP_COUNT;
            case Registration:
                return registered;
        }
        return false; // Never reached
    }

    /**
     * Copy the current profile to the given one.
     */
    public void copyTo(PatientProfile other) {
        other.username             = username;
        other.tallestCeilingHeight = tallestCeilingHeight;
        other.homeLatitude         = homeLatitude;
        other.homeLongitude        = homeLongitude;
        other.validationStep       = validationStep;
        other.isHomeAcquired       = isHomeAcquired;
        other.registered           = registered;
        other.setupStartTimestamp  = setupStartTimestamp;
        other.setupEndTimestamp    = setupEndTimestamp;
        if (rooms != null) {
            other.rooms = new Room[rooms.length];
            for (int i = 0; i < rooms.length; ++i)
                other.rooms[i] = other.new Room(rooms[i]);
        } else
            other.rooms = null;
    }

    /**
     * Returns the home location.
     */
    private Location getHomeLocation() {
        Location location = new Location(LocationManager.GPS_PROVIDER);
        location.setLatitude(homeLatitude);
        location.setLongitude(homeLongitude);
        return location;
    }

    /**
     * Returns the distance between the given location and the home location (in meters).
     */
    public float getHomeDistance(Location location) {
        return location.distanceTo(getHomeLocation());
    }

    private void resetHouseSetup() {
        validationStep = 0;
        isHomeAcquired = false;
    }

    /**
     * Indicate in which initialization step the profile is.
     */
    public enum Stage {
        /** Patient id needs to be validated by the server. */
        PatientId,

        /** Patient username requested. */
        PatientUsername,

        /** Room count requested. */
        RoomCount,

        /** Height of the tallest ceiling requested. */
        TallestCeiling,

        /** Rooms setup. */
        RoomSetup,

        /** Room validation step, i.e. step during which one beacons are tested. */
        MoteValidation,

        /**
         * Server validation requested. Validation will failed if the server is not able to predict
         * patient location an accuracy good enough.
         */
        Registration
    }

    /**
     * Enumeration of all the possible room profile status.
     */
    public enum RoomStatus {
        /** The room profile is valid. */
        Valid,

        /** The beacon id is not valid. */
        InvalidBeaconId,

        /** The room name is not valid. Room names should be no empty and unique. */
        InvalidRoomName,

        /** The ceiling height is not valid (<= 0). */
        InvalidCeilingHeight
    }

    /**
     * Room representation. Contains {@link Mote mote} identification, room floor, ceiling height
     * and distance from the previous room. If a room value is changed, the user will have to redo
     * the beacons test process.
     */
    public class Room extends Mote {
        private double mHeight;
        private int    mFloor;
        private double mXDistFromPrev;
        private double mYDistFromPrev;

        public Room() {
            super();
        }

        public Room(Room other) {
            super(other.getMoteId(), other.getRoomName());
            mHeight        = other.mHeight;
            mFloor         = other.mFloor;
            mXDistFromPrev = other.mXDistFromPrev;
            mYDistFromPrev = other.mYDistFromPrev;
        }

        public Room(String id, String room) {
            super(id, room);
        }

        @Override
        public void setMoteId(String id) {
            if (getMoteId().equals(id))
                return;
            resetHouseSetup();
            super.setMoteId(id);
        }

        @Override
        public void setRoomName(String name) {
            if (getRoomName().equals(name))
                return;
            resetHouseSetup();
            super.setRoomName(name);
        }

        /** Returns the room floor (-1 for Basement). */
        public int getFloor() {
            return mFloor;
        }

        /** Sets the room floor (-1 for Basement). */
        public void setFloor(int floor) {
            if (mFloor == floor)
                return;
            resetHouseSetup();
            mFloor = floor;
        }

        /** Returns the room ceiling height (in feet). */
        public double getHeight() {
            return mHeight;
        }

        /** Sets the room ceiling height (in feet). */
        public void setHeight(double height) {
            if (mHeight == height)
                return;
            resetHouseSetup();
            mHeight = height;
        }

        /**
         * Returns the distance (in feet) from the previous room beacon in a first arbitrary
         * direction. This first arbitrary direction should be the same for all the house rooms.
         */
        public double getXDistanceFromPrevious() {
            return mXDistFromPrev;
        }

        /**
         * Sets the distance (in feet) from the previous room beacon in a first arbitrary direction.
         * This first arbitrary direction should be the same for all the house rooms.
         */
        public void setXDistanceFromPrevious(double distance) {
            if (mXDistFromPrev == distance)
                return;
            resetHouseSetup();
            mXDistFromPrev = distance;
        }

        /**
         * Returns the distance (in feet) from the previous room beacon in a second arbitrary
         * direction. This second arbitrary direction should be the same for all the house rooms.
         */
        public double getYDistanceFromPrevious() {
            return mYDistFromPrev;
        }

        /**
         * Sets the distance (in feet) from the previous room beacon in a second arbitrary
         * direction. This second arbitrary direction should be the same for all the house rooms.
         */
        public void setYDistanceFromPrevious(double distance) {
            if (mYDistFromPrev == distance)
                return;
            resetHouseSetup();
            mYDistFromPrev = distance;
        }
    }
}
