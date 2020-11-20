package tasks;

import pojo.Checkpoint;
import servers.ActiveServerReplica;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class SendCheckpointOneTime implements Runnable {

    private static final String hostname = ActiveServerReplica.getHostname();

    private final ActiveServerReplica server;

    public SendCheckpointOneTime(ActiveServerReplica server) {
        this.server = server;
    }

    @Override
    public void run() {
        Socket socket;
        ObjectOutputStream out;

        /* Send checkpoint to each server */
        for (int checkpointPort : ActiveServerReplica.getCheckpointPorts()) {
            /* Establish TCP/IP connection */
            try {
                socket = new Socket(hostname, checkpointPort);
                out = new ObjectOutputStream(socket.getOutputStream());
            } catch (IOException u) {
                System.out.println("Newly added server checkpoint port  " + checkpointPort + " is not open");
                continue;
            }

            /* Send checkpoint to a newly added server */
            try {
                Checkpoint checkpoint = new Checkpoint(server.getState(), 0);
                out.writeObject(checkpoint);

                System.out.println("Server: Sent checkpoint to newly added server " + checkpointPort);
                server.logState();
            } catch (IOException e) {
                System.out.println("Error in sending checkpoint");
                e.printStackTrace();
            }

            /* Close the connection */
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Error in closing socket");
                e.printStackTrace();
            }
        }
    }
}

