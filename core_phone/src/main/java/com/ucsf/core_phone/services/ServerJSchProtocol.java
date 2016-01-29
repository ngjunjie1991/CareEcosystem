package com.ucsf.core_phone.services;


import android.content.Context;
import android.os.AsyncTask;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.ucsf.core.services.ResponseListener;

import java.util.Properties;

/**
 * Abstract {@link ServerProtocol protocol} using a JSch connection.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public abstract class ServerJSchProtocol extends ServerProtocol {
    protected final String  mHost;
    protected final int     mPort;
    protected final String  mUsername;
    protected final String  mPassword;
    protected       Session mSession;

    protected ServerJSchProtocol(String host, int port, String username, String password) {
        mHost     = host;
        mPort     = port;
        mUsername = username;
        mPassword = password;
    }

    @Override
    protected void openConnection(Context context) throws Exception {
        JSch jsch = new JSch();
        try {
            mSession = jsch.getSession(mUsername, mHost, mPort);
            mSession.setPassword(mPassword);

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            mSession.setConfig(config);

            mSession.connect();
        } catch (Exception e) {
            if (mSession != null && mSession.isConnected())
                mSession.disconnect();
            throw e;
        }
    }

    @Override
    public void closeConnection() {
        mSession.disconnect();
    }

    @Override
    public void execute(final Context context, final Runnable runnable,
                        final ResponseListener listener)
    {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                ServerJSchProtocol.super.execute(context, runnable, listener);
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
