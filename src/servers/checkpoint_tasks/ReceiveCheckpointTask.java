package servers.checkpoint_tasks;

import pojo.Checkpoint;
import servers.PassiveServerReplica;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

public class ReceiveCheckpointTask implements Runnable {
    private final PassiveServerReplica server;
    private static final Logger receiveCheck = Logger.getLogger("receiveCheck");
    public ReceiveCheckpointTask(PassiveServerReplica server) {
        this.server = server;
    }

    @Override
    public void run() {
        ServerSocket ss;
        try {
            ss = new ServerSocket(server.getCheckpointPort());
        } catch (IOException e) {
            receiveCheck.info("Listening error");
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
                    server.setState(checkpoint.getState());
                    server.setCheckpointCount(checkpoint.getCheckpointCount());
                    server.incrementCheckpointCount();

                    receiveCheck.info("Checkpoint " + checkpoint.getCheckpointCount() + " received");
                    server.logState();
                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }
            } else {
                /* Server is ready and primary */
                synchronized (server) {
                    try {
                        receiveCheck.info("Passive Server: Checkpoint receiving task paused");
                        server.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                receiveCheck.info("Passive Server: Checkpoint receiving task awaken");
            }
        }
    }
}