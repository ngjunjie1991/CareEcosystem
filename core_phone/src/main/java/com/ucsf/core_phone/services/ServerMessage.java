package com.ucsf.core_phone.services;

/**
 * Enumeration listing the possible messages that can be received from the server.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public enum ServerMessage {
    /** Invalid message. */
    Invalid                     (0, "Invalid"),
    /** */
    PatientProfileValidation    (1, "PPV"),
    CaregiverProfileValidation  (2, "CPV"),
    CaregiverAccountConfirmation(6, "AC"),
    PatientAuthorization        (3, "PA"),
    PatientNotification         (4, "PN"),
    CaregiversUsername          (5, "CUR"),
    ForgottenPassword           (7, "FP");

    public static final String MESSAGE_ID = "type";

    public final long flag;
    public final String tag;

    ServerMessage(long flag, String tag) {
        this.flag = flag;
        this.tag = tag;
    }

    public static ServerMessage fromTag(String tag) {
        for (ServerMessage message : ServerMessage.values()) {
            if (message.tag.equals(tag))
                return message;
        }
        return Invalid;
    }

    public static ServerMessage fromFlag(long flag) {
        for (ServerMessage message : ServerMessage.values()) {
            if (message.flag == flag)
                return message;
        }
        return Invalid;
    }
}