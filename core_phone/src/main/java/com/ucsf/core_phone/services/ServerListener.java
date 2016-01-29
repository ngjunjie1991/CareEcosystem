package com.ucsf.core_phone.services;

import android.os.Bundle;

/**
 * Interface to implement in order to receive messages from the server.
 * In order to be notified, the listener has to be registered by the provider.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public interface ServerListener {
    /** Method called when a message is received from the server. */
    void onServerMessage(ServerMessage message, Bundle data);
}