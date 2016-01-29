package com.ucsf.core.data;

/**
 * Enumeration of the possible devices used in this project.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public enum DeviceLocation {
    Unknown       ("XX"),
    PatientPhone  ("MM"),
    PatientWatch  ("HM"),
    CaregiverPhone("CM");

    private final String mTag;

    DeviceLocation(String tag) {
        mTag = tag;
    }

    /**
     * Returns the device corresponding to the given tag, or {@link DeviceLocation#Unknown} if no
     * match is found.
     */
    public static DeviceLocation fromTag(String tag) {
        switch (tag) {
            case "MM":
                return PatientPhone;
            case "HM":
                return PatientWatch;
            case "CM":
                return CaregiverPhone;
            default:
                return Unknown;
        }
    }

    @Override
    public String toString() {
        return mTag;
    }
}
