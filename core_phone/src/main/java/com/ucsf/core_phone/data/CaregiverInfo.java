package com.ucsf.core_phone.data;

/**
 * Class containing information about the caregiver.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public class CaregiverInfo {
    /** Caregiver unique id. */
    public final String caregiverId;

    /** Caregiver username. */
    public final String username;

    /** Caregiver password to enter the application. */
    public final String password;

    /** Caregiver email. */
    public final String email;

    /** Caregiver phone number. */
    public final String phoneNumber;

    /** Indicates if the caregiver accepts to receiver email. */
    public final boolean acceptMail;

    /** Indicates if the caregiver accepts to receiver SMS. */
    public final boolean acceptSMS;

    public CaregiverInfo(String caregiverId, String username, String password, String email,
                         String phoneNumber, boolean acceptMail, boolean acceptSMS)
    {
        this.caregiverId = caregiverId;
        this.username    = username;
        this.password    = password;
        this.email       = email;
        this.phoneNumber = phoneNumber;
        this.acceptMail  = acceptMail;
        this.acceptSMS   = acceptSMS;
    }
}
