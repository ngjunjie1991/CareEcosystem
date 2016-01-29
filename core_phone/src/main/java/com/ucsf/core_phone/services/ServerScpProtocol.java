package com.ucsf.core_phone.services;


import com.jcraft.jsch.ChannelExec;
import com.ucsf.core.services.ResponseListener;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

/**
 * {@link ServerProtocol Protocol} using scp to push data to the server.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public class ServerScpProtocol extends ServerJSchProtocol {
    public ServerScpProtocol(String host, int port, String username, String password) {
        super(host, port, username, password);
    }

    @Override
    public void writeData(String folder, String filename, byte[] data, ResponseListener listener) {
        ChannelExec channel = null;
        OutputStream out = null;

        try {
            // Exec 'scp -t rfile' remotely
            channel = (ChannelExec) mSession.openChannel("exec");
            channel.setCommand(String.format("scp -t %s/%s", folder, filename));

            // Get I/O streams for remote scp
            out = channel.getOutputStream();
            InputStream stderr = channel.getErrStream();
            InputStream in = channel.getInputStream();
            channel.connect();

            // Make sure that the command is sent
            checkAck(in, stderr);

            // Send "C0644 filesize filename", where filename should not include '/'
            String command = String.format("C0644 %d %s\n", data.length, filename);
            out.write(command.getBytes());
            out.flush();

            checkAck(in, stderr);

            // Send the buffer to the server
            out.write(data);
            out.write(0);
            out.flush();

            checkAck(in, stderr);
        } catch (Exception e) {
            listener.onFailure("Failed to send data to the server: ", e);
            return;
        } finally {
            try {
                if (out != null)
                    out.close();
            } catch (Exception ignored) {
            }
            if (channel != null)
                channel.disconnect();
        }

        listener.onSuccess();
    }

    /**
     * Verifies that a file is successfully sent through JSch.
     */
    private void checkAck(InputStream in, InputStream stderr) throws Exception {
        // b may be 0 for success,  1 for error, 2 for fatal error, -1
        int b = in.read();

        if (b == 1 || b == 2) {
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            throw new Exception("An error occurred during commit: " + br.readLine());
        } else if (b != 0) {
            BufferedReader br = new BufferedReader(new InputStreamReader(stderr));
            throw new Exception("An error occurred during commit: " + br.readLine());
        }
    }
}
