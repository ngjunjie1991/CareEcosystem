package com.ucsf.core_phone.services;

import android.content.Context;

import com.ucsf.core.data.AbstractConnection;
import com.ucsf.core.services.ResponseListener;

/**
 * Abstract protocol to communicate with a remote server.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public abstract class ServerProtocol extends AbstractConnection {
    /**
     * Writes the given data to the given server file.
     */
    public abstract void writeData(String category, String id, byte[] data, ResponseListener listener);

    /**
     * Opens, executes and closes the protocol.
     */
    public synchronized void execute(Context context, Runnable runnable, ResponseListener listener) {
        try (ServerProtocol instance = (ServerProtocol) open(context)) {
            runnable.run();
        } catch (Exception e) {
            listener.onFailure("An error occurred while sending data to the server: ", e);
        }
    }
}
