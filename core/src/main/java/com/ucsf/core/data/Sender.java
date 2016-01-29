package com.ucsf.core.data;

/**
 * Class used in communication protocol to identify the message sender. Contains the user unique id
 * and the device from the message is coming from.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public class Sender {
    public final String         id;       /**< Unique id of the user. */
    public final DeviceLocation location; /**< Device from where the message is coming from. */

    public Sender(String id, DeviceLocation location) {
        this.id = id;
        this.location = location;
    }
}
