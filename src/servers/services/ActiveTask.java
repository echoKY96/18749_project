package servers.services;

import pojo.Tuple;
import servers.ActiveServerReplica;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.logging.Logger;

public class ActiveTask implements Runnable {

    private static final String LFD_MSG = "heartbeat";

    private final ServiceProvider sp;
    private final ActiveServerReplica server;
    private final Socket socket;
    private static final Logger activeTaskLog = Logger.getLogger("activeTask");
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

            while (true) {
                String line;
                if (server.isReady() && !server.isRQEmpty()) {
                    line = server.dequeue();
                } else {
                    line = dis.readUTF();
                }

                if (line.contains(LFD_MSG)) {
                    /* LFD connection */
                    activeTaskLog.info(""+line);
                    break;
                } else {
                    /* Player connection */
                    Tuple tuple = sp.parseLine(line);
                    Integer clientId = tuple.getClientId();
                    String message = tuple.getMessage();
                    Integer requestNum = tuple.getRequestNum();

                    activeTaskLog.info("Client " + clientId + ", " + "request_num: " + requestNum + ", " + "message: " + message);
                    if (server.isReady() && !server.isCheckpointing()) {
                        if (!sp.gameService(dos, clientId, message)) {
                            break;
                        }
                    } else if (server.isReady() && server.isCheckpointing()) {
                        activeTaskLog.info("Active server: quiescence, enqueue request");
                        sp.queuingService(line);
                    } else if (!server.isReady()) {
                        activeTaskLog.info("Active server: receiving checkpoint, enqueue request");
                        sp.queuingService(line);
                    } else {
                        activeTaskLog.info("Impossible");
                    }
                }
            }

            dos.flush();
            dos.close();
            dis.close();
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
            activeTaskLog.info("Client " + socket.getPort() + " Lost connection");
        }
    }
}
