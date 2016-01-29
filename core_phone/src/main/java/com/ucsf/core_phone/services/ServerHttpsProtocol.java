package com.ucsf.core_phone.services;

import android.content.Context;

import com.ucsf.core.services.ResponseListener;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * {@link ServerProtocol Protocol} using https to push data to the server.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public class ServerHttpsProtocol extends ServerJSchProtocol {
    private static final int    TIMEOUT = 60000;
    private static final String BOUNDARY = "*****";
    private static final String LINE_END = "\r\n";
    private static final String TWO_HYPHENS = "--";
    private static final String DATA_HEADER = TWO_HYPHENS + BOUNDARY + LINE_END;
    private static final String DATA_CONTENT_DESCRIPTION =
            "Content-Disposition: form-data; name=\"uploadedfile\";filename=\"%s\"" + LINE_END;
    private static final String DATA_FOOTER = TWO_HYPHENS + BOUNDARY + TWO_HYPHENS + LINE_END;
    private static final String CONTENT_TYPE_PROPERTY = "multipart/form-data;boundary=" + BOUNDARY;

    protected final int mLPort;

    protected ServerHttpsProtocol(String host, int port, int lPort, String username, String password) {
        super(host, port, username, password);
        mLPort = lPort;
    }

    @Override
    protected void openConnection(Context context) throws Exception {
        super.openConnection(context);
        mSession.setPortForwardingL(mLPort, mHost, mLPort);
    }

    @Override
    public void writeData(String folder, String filename, byte[] data, final ResponseListener handler) {
        HttpURLConnection connection;
        try {
            URL url = new URL(String.format("http://localhost:%d/%s", mLPort, folder));

            connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setConnectTimeout(TIMEOUT);
            connection.setReadTimeout(TIMEOUT);

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
            handler.onFailure("Client Message error: ", e);
            return;
        }

        try {
            int responseCode = connection.getResponseCode();
            if (responseCode != 200)
                handler.onFailure(String.format("Wrong response code(%d): %s", responseCode,
                        connection.getResponseMessage()), null);
            else
                handler.onSuccess();
        } catch (Exception e) {
            handler.onFailure("Connection error: ", e);
        }
    }
}
