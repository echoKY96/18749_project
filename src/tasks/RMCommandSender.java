package tasks;

import servers.ActiveServerReplica;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class RMCommandSender implements Runnable {
    private static final String hostname = ActiveServerReplica.getHostname();

    private final String NEW_ADD = "new server added";

    @Override
    public void run() {
        Socket socket;
        DataOutputStream out;

        for (int rmListeningPort : ActiveServerReplica.getRmListeningPorts()) {
            /* Establish TCP/IP connection */
            try {
                socket = new Socket(hostname, rmListeningPort);
                out = new DataOutputStream(socket.getOutputStream());
            } catch (IOException u) {
                System.out.println("Backup " + rmListeningPort + " is not open");
                continue;
            }

            /* Send command to each server */
            try {
                out.writeUTF(NEW_ADD);

                System.out.println("Sent new_add command " + " to server " + rmListeningPort);
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
