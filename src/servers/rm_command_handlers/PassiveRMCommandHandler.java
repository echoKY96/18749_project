package servers.rm_command_handlers;

import servers.PassiveServerReplica;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.logging.Logger;

public class PassiveRMCommandHandler implements Runnable {

    @SuppressWarnings("FieldCanBeLocal")
    private final String PRIMARY_CKPT_PORT = "primary_checkpoint_port";

    private final Socket socket;
    private final PassiveServerReplica server;
    private static final Logger passiveCommandLog = Logger.getLogger("passiveCommandLog");
    public PassiveRMCommandHandler(Socket socket, PassiveServerReplica server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            DataInputStream dis = new DataInputStream(socket.getInputStream());

            String line = dis.readUTF();
            passiveCommandLog.info("RM: " + line);
            String[] messages = line.split(":");
            String message = messages[0];
            int primaryCheckpointPort = Integer.parseInt(messages[1]);

            if (message.equalsIgnoreCase(PRIMARY_CKPT_PORT)) {
                if (server.getPrimaryCheckpointPort() == null || server.getPrimaryCheckpointPort() != primaryCheckpointPort) {
                    synchronized (server) {
                        /* Primary re-elected */
                        server.setPrimaryCheckpointPort(primaryCheckpointPort);

                        if (server.getCheckpointPort() == primaryCheckpointPort) {
                            server.setReady();
                            server.setPrimary();

                            passiveCommandLog.info("Passive server: elected as primary");
                        } else {
                            server.setReady();
                            server.setBackup();

                            passiveCommandLog.info("Passive server: elected as backup");
                        }

                        server.notifyAll();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

