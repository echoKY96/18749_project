package servers.services;

import pojo.Tuple;
import servers.PassiveServerReplica;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.logging.Logger;

public class PassiveTask implements Runnable {
    private static final String LFD_MSG = "heartbeat";
    private static final Logger passiveTask = Logger.getLogger("passiveTask");
    private final ServiceProvider sp;
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

            while (true) {
                String line;
                if (server.isPrimary() && !server.isRQEmpty()) {
                    line = server.dequeue();
                } else {
                    /* Backup or Primary that has an empty RQ */
                    line = dis.readUTF();
                }

                if (line.contains(LFD_MSG)) {
                    /* LFD connection */
                    passiveTask.info("" + line);
                    break;
                } else {
                    /* Player connection */
                    Tuple tuple = sp.parseLine(line);
                    Integer clientId = tuple.getClientId();
                    String message = tuple.getMessage();
                    Integer requestNum = tuple.getRequestNum();

                    passiveTask.info("Client " + clientId + ", " + "request_num: " + requestNum + ", " + "message: " + message);
                    if (server.isPrimary() && server.isReady()) {
                        if (!sp.gameService(dos, clientId, message)) {
                            break;
                        }
                    } else {
                        sp.queuingService(line);
                        passiveTask.info("Passive backup server: idle, enqueue request");
                        dos.writeUTF("Idle");
                    }
                }
            }

            dos.flush();
            dos.close();
            dis.close();
            socket.close();
        } catch (Exception e) {
            passiveTask.info("Client " + socket.getPort() + " Lost connection");
        }
    }
}
