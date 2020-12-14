package servers.rm_command_handlers;

import pojo.Checkpoint;
import servers.ActiveServerReplica;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.logging.Logger;

public class ActiveRMCommandHandler implements Runnable {

    private static final String hostName = "127.0.0.1";

    private final String NEW_ADD = "new_add";

    private final Socket socket;
    private final ActiveServerReplica server;
    private static final Logger activeHandler = Logger.getLogger("activeHandler");
    public ActiveRMCommandHandler(Socket socket, ActiveServerReplica server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            DataInputStream dis = new DataInputStream(socket.getInputStream());

            String line = dis.readUTF();
            activeHandler.info("RM: " + line);
            String[] messages = line.split(":");
            String new_add = messages[0];
            int checkpointPort = Integer.parseInt(messages[1]);

            if (new_add.equalsIgnoreCase(NEW_ADD) && server.isReady()) {
                if (server.getCheckpointPort() == checkpointPort) {
                    /* New added server itself */
                    activeHandler.info("Active server: RM knows I am online");
                    // back here
                    server.clearRQ();
                } else {
                    server.setCheckpointing();
                    activeHandler.info("Active server: new server added, goes into quiescence");
                    sendCheckpointOneTime(checkpointPort);

                    activeHandler.info("Active Server: Quiescence ends");
                    activeHandler.info("Active Server Backlog:");
                    server.logBacklog();
                    server.setNotCheckpointing();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /* Send checkpoint to new added server */
    private void sendCheckpointOneTime(int checkpointPort) {
        Socket socket;
        ObjectOutputStream out;

        /* Establish TCP/IP connection */
        try {
            socket = new Socket(hostName, checkpointPort);
            out = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException u) {
            activeHandler.info("Active " + checkpointPort + " is not open");
            return;
        }

        /* Send checkpoint to a newly added server */
        try {
            Checkpoint checkpoint = new Checkpoint(server.getState(), 0);
            out.writeObject(checkpoint);

            activeHandler.info("Server: Sent checkpoint to newly added server " + checkpointPort);
            server.logState();
        } catch (IOException e) {
            activeHandler.info("Error in sending checkpoint");
            e.printStackTrace();
        }

        /* Close the connection */
        try {
            socket.close();
        } catch (IOException e) {
            activeHandler.info("Error in closing socket");
            e.printStackTrace();
        }
    }
}

