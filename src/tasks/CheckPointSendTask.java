package tasks;

import pojo.Checkpoint;
import servers.PassiveServerReplica;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

public class CheckPointSendTask implements Runnable {

    /* Configuration info about a primary - backups group */
    private static final List<Integer> backups = PassiveServerReplica.getSlaves();
    private static final String hostname = PassiveServerReplica.getHostname();
    private static final int CKPT_FREQ = 5000;

    private final PassiveServerReplica server;

    public CheckPointSendTask(PassiveServerReplica server) {
        this.server = server;
    }

    @Override
    public void run() {
        Socket socket;
        ObjectOutputStream out;

        while (true) {
            /* Send checkpoint to each buckup */
            boolean allSlavesDown = true;
            for (int serverPort : backups) {
                /* Establish TCP/IP connection */
                try {
                    socket = new Socket(hostname, serverPort);
                    out = new ObjectOutputStream(socket.getOutputStream());
                } catch (IOException u) {
                    System.out.println("Slave " + serverPort + " is not open");
                    continue;
                }

                /* Send checkpoint to a buckup server */
                allSlavesDown = false;
                try {
                    Checkpoint checkpoint = new Checkpoint(server.getState(), server.getCheckpointCount());
                    out.writeObject(checkpoint);

                    System.out.println("Sent checkpoint " + server.getCheckpointCount() + " to backup " + serverPort);
                    server.getState().forEach((k, v) -> System.out.println("clients.Client " + k + " number range: [" + v.getLeft() + ", " + v.getRight() + "]"));
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

            if (!allSlavesDown) {
                server.incrementCheckpointCount();
            }

            /* Sleep for CKPT_FREQ */
            try {
                Thread.sleep(CKPT_FREQ);
            } catch (InterruptedException e) {
                System.out.println("Interruption to sleep");
                e.printStackTrace();
                return;
            }
        }
    }
}
