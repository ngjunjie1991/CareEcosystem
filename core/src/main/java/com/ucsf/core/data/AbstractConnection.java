package com.ucsf.core.data;

import android.content.Context;


/**
 * Class describing an abstract connection. Should be use when a connection can be opened more
 * than once and when you need that all the owners release it before closing it.                    <br/>
 *                                                                                                  <br/>
 * The typical way to use such a connection is the following:                                       <br/><pre>
 * {@code
 *      try (Connection instance = connection.open(context)) {
 *          // Use the opened connection
 *      } catch (Exception e) {
 *          // Handle eventual exceptions
 *      }
 * }
 *                                                                                                  </pre><br/>
 * By this way, the connection is automatically closed no matter what.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public abstract class AbstractConnection implements AutoCloseable {
    private int mInstanceCount = 0; /**< Number of opened connections. */

    /**
     * Increments the number if opened connections. If it's the first time that this method is
     * called, opens for real the underlying connection.
     */
    public final synchronized AbstractConnection open(Context context)
            throws Exception
    {
        if (mInstanceCount == 0)
            openConnection(context);
        ++mInstanceCount;
        return this;
    }

    /**
     * Decrements the number of opened connections. If there are no connections left, closes for
     * real the underlying connection.
     */
    @Override
    public final synchronized void close() throws Exception {
        if (mInstanceCount == 0)
            return;

        if (mInstanceCount == 1)
            closeConnection();
        --mInstanceCount;
    }

    /**
     * Opens the underlying connection. Should never be called directly, use
     * {@link AbstractConnection#open(Context context) open()} instead.
     */
    protected abstract void openConnection(Context context) throws Exception;

    /**
     * Closes the underlying connection. Should never be called directly, use
     * {@link AbstractConnection#close() close()} instead.
     */
    protected abstract void closeConnection();
}
