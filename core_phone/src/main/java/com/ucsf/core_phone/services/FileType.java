package com.ucsf.core_phone.services;

/**
 * Indicates the type of the file sent to the server.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public enum FileType {
    /** File containing raw data, for instance data from the sensors. */
    Data   ("data"),
    /** File containing profile information. */
    Config ("config"),
    /** File describing an event. */
    Event  ("events"),
    /** Request file, for instance password verification. */
    Request("requests");

    private final String mTag;

    FileType(String tag) {
        mTag = tag;
    }

    @Override
    public String toString() {
        return mTag;
    }
}