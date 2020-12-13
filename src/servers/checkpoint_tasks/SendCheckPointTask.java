package servers.checkpoint_tasks;

import pojo.Checkpoint;
import servers.PassiveServerReplica;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class SendCheckPointTask implements Runnable {

    /* Configuration info about a primary - backups group */
    private static final int CKPT_FREQ = 5000;
    private static final String hostName = "127.0.0.1";

    private final PassiveServerReplica server;

    public SendCheckPointTask(PassiveServerReplica server) {
        this.server = server;
    }

    private void sendCheckpoint() {
        /* Primary: Send checkpoint to each buckup */
        boolean allBackupsDown = true;
        for (int serverPort : server.getBackupCheckpointPorts()) {
            /* Establish TCP/IP connection */
            Socket socket;
            ObjectOutputStream out;
            try {
                socket = new Socket(hostName, serverPort);
                out = new ObjectOutputStream(socket.getOutputStream());
            } catch (IOException u) {
                System.out.println("Backup " + serverPort + " is not open");
                continue;
            }

            /* Send checkpoint to a buckup server */
            allBackupsDown = false;
            try {
                Checkpoint checkpoint = new Checkpoint(server.getState(), server.getCheckpointCount());
                out.writeObject(checkpoint);

                System.out.println("Sent checkpoint " + server.getCheckpointCount() + " to backup " + serverPort);
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

        if (!allBackupsDown) {
            server.incrementCheckpointCount();
        }

        /* Sleep for CKPT_FREQ */
        try {
            Thread.sleep(CKPT_FREQ);
        } catch (InterruptedException e) {
            System.out.println("Interruption to sleep");
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (true) {
            // not ready: block
            // ready and primary: send
            // ready and backup: block
            if (server.isReady() && server.isPrimary()) {
                sendCheckpoint();
            } else {
                synchronized (server) {
                    try {
                        System.out.println("Passive Server: Checkpoint Sending task paused");
                        server.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                System.out.println("Passive Server: Checkpoint Sending task awaken");
            }
        }
    }
}
