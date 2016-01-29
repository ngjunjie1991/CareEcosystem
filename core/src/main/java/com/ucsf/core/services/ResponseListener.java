package com.ucsf.core.services;

/**
 * Interface for basicc request listener.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public interface ResponseListener {
    /** Method called when the request is successful. */
    void onSuccess();

    /** Method called when an error is raised during execution of the request. */
    void onFailure(String error, Throwable e);
}
