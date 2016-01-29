package com.ucsf.core_phone.services;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import com.ucsf.core.data.Settings;
import com.ucsf.core.services.ResponseListener;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManagerFactory;

/**
 * Server protocol implementation using a secured Https connection with certificate.
 */
public class ServerSecuredHttpsProtocol extends ServerProtocol {
    private static final int    TIMEOUT       = 60000;
    private static final String TAG           = "ucsf:HttpsProtocol";
    private static final String BOUNDARY      = "*****";
    private static final String LINE_END      = "\r\n";
    private static final String TWO_HYPHENS   = "--";
    private static final String DATA_HEADER   = TWO_HYPHENS + BOUNDARY + LINE_END;
    private static final String DATA_CONTENT_DESCRIPTION =
            "Content-Disposition: form-data; name=\"uploadedfile\";filename=\"%s\"" + LINE_END;
    private static final String DATA_FOOTER   = TWO_HYPHENS + BOUNDARY + TWO_HYPHENS + LINE_END;
    private static final String CONTENT_TYPE_PROPERTY = "multipart/form-data;boundary=" + BOUNDARY;

    protected final String     mHost;
    protected       Context    mContext;
    protected       SSLContext mSSLContext;

    protected ServerSecuredHttpsProtocol(String host) {
        mHost = host;
    }

    @Override
    public synchronized void writeData(String folder, String filename, byte[] data,
                                       ResponseListener listener)
    {
        HttpsURLConnection connection;
        try {
            URL url = new URL(String.format("https://%s/%s", mHost, folder));

            connection = (HttpsURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setConnectTimeout(TIMEOUT);
            connection.setReadTimeout(TIMEOUT);

            connection.setSSLSocketFactory(mSSLContext.getSocketFactory());

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("Content-Type", CONTENT_TYPE_PROPERTY);

            DataOutputStream dos = new DataOutputStream(connection.getOutputStream());
            dos.writeBytes(DATA_HEADER);
            dos.writeBytes(String.format(DATA_CONTENT_DESCRIPTION, filename));
            dos.writeBytes(LINE_END);

            dos.write(data);

            dos.writeBytes(LINE_END);
            dos.writeBytes(DATA_FOOTER);

            dos.flush();
            dos.close();
        } catch (Exception e) {
            listener.onFailure(String.format("Client Message error(%s): ", e.getClass()), e);
            return;
        }

        try {
            int responseCode = connection.getResponseCode();
            if (responseCode != 200)
                listener.onFailure(String.format("Wrong response code(%d): %s", responseCode,
                        connection.getResponseMessage()), null);
            else
                listener.onSuccess();
        } catch (Exception e) {
            listener.onFailure("Connection error: ", e);
        }
    }

    @Override
    protected synchronized void openConnection(Context context) throws Exception {
        mContext = context;

        // Create the ssl context if needed
        if (mSSLContext == null) {
            mSSLContext = SSLContext.getInstance("SSLv3");
            mSSLContext.init(null, null, null);
        }
    }

    @Override
    protected synchronized void closeConnection() {}

    @Override
    public void execute(final Context context, final Runnable runnable,
                        final ResponseListener listener)
    {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                ServerSecuredHttpsProtocol.super.execute(context, runnable, listener);
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
