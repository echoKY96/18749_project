package servers.checkpoint_tasks;

import pojo.Checkpoint;
import servers.PassiveServerReplica;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class ReceiveCheckpointTask implements Runnable {
    private final PassiveServerReplica server;

    public ReceiveCheckpointTask(PassiveServerReplica server) {
        this.server = server;
    }

    @Override
    public void run() {
        ServerSocket ss;
        try {
            ss = new ServerSocket(server.getCheckpointPort());
        } catch (IOException e) {
            System.out.println("Listening error");
            e.printStackTrace();
            return;
        }

        while (true) {
            // not ready: block
            // ready: receive
            if (server.isReady()) {
                try {
                    Socket socket = ss.accept();

                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                    Checkpoint checkpoint = (Checkpoint)in.readObject();

                    // server.clearRQ();
                    server.setState(checkpoint.getState()); // thread safety: volatile ensures visibility
                    server.setCheckpointCount(checkpoint.getCheckpointCount());
                    server.incrementCheckpointCount();

                    System.out.println("Checkpoint " + checkpoint.getCheckpointCount() + " received");
                    server.logState();
                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }
            } else {
                /* Server is ready and primary */
                synchronized (server) {
                    try {
                        System.out.println("Passive Server: Checkpoint receiving task paused");
                        server.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                System.out.println("Passive Server: Checkpoint receiving task awaken");
            }
        }
    }
}