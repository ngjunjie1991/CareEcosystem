package com.ucsf.core_phone.services;

import android.os.Bundle;

/**
 * Interface to implement to send a request to the server and receive its response.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public interface ServerRequestListener {
    /** Method called when the message has been sent to the server. */
    void onDeliveryStart();

    /** Method called if the API failed to send the message to the server. */
    void onDeliveryFailure(String error, Throwable e);

    /** Method called when no answer has been received from the server. */
    void onTimeout();

    /** Method called when the the server answer has been received. */
    void onResponseReceived(Bundle data);
}