package com.ucsf.core.data;

/**
 * Structure representing a mote (bluetooth beacon). Contains an unique id and the name of the room
 * in which the beacon is.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public class Mote {
    private String mId;
    private String mRoom;

    protected Mote() {
        mId   = "";
        mRoom = "";
    }

    protected Mote(String id, String room) {
        mId   = id;
        mRoom = room;
    }

    /** Returns the mote unique id. */
    public String getMoteId() {
        return mId;
    }

    /** Sets the mote unique id. */
    public void setMoteId(String id) {
        mId = id;
    }

    /** Returns the name of the room on which the mote is. */
    public String getRoomName() {
        return mRoom;
    }

    /** Set the room in which the mote is. */
    public void setRoomName(String name) {
        mRoom = name;
    }

    @Override
    public String toString() {
        return String.format("[%s: %s]", getRoomName(), getMoteId());
    }
}
