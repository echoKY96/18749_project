package tasks;

import servers.PassiveServerReplica;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.Map;

public class PassiveTask implements Runnable {
    private static final String LFD_MSG = "heartbeat";

    private ServiceProvider sp;
    private final PassiveServerReplica server;
    private final Socket socket;

    public PassiveTask(Socket socket, PassiveServerReplica server) {
        this.sp = new ServiceProvider(socket, server);
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            DataInputStream dis = new DataInputStream(socket.getInputStream());

            dos.writeUTF("Server " + socket.getLocalPort() + " : Welcome, keep a number in range [1, 10] and I'll guess it, enter \"play\" to start a game.");

            while (true) {
                String line;
                if (server.isPrimary() && !server.isRQEmpty()) {
                    line = server.dequeue();
                } else {
                    line = dis.readUTF();
                }

                if (line.contains(LFD_MSG)) {
                    /* LFD connection */
                    System.out.println(line);
                    break;
                } else {
                    /* Player connection */
                    Map.Entry<Integer, String> messageEntry = sp.parseLine(line);
                    Integer clientId = messageEntry.getKey();
                    String message = messageEntry.getValue();

                    // logger
                    System.out.println("Client " + socket.getPort() + ": " + message);

                    if (server.isPrimary()) {
                        if (!sp.gameService(dos, clientId, message)) {
                            break;
                        }
                    } else if (!server.isPrimary()) {
                        sp.queuingService(line);
                    } else {
                        System.out.println("Impossible");
                    }
                }
            }

            dos.flush();
            dos.close();
            dis.close();
            socket.close();
        } catch (Exception e) {
            System.out.println("Client " + socket.getPort() + " Lost connection");
        }
    }
}
