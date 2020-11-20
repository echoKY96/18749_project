package tasks;

import servers.ActiveServerReplica;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.Map;

public class ActiveTask implements Runnable {

    private static final String LFD_MSG = "heartbeat";

    private final ServiceProvider sp;
    private final ActiveServerReplica server;
    private final Socket socket;

    public ActiveTask(Socket socket, ActiveServerReplica server) {
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
                if (!server.isRQEmpty()) {
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

                    if (server.isReady() && !server.isCheckpointing()) {
                        if (!sp.gameService(dos, clientId, message)) {
                            break;
                        }
                    } else if (server.isReady() && server.isCheckpointing() || !server.isReady()) {
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
            e.printStackTrace();
            System.out.println("Client " + socket.getPort() + " Lost connection");
        }
    }
}
