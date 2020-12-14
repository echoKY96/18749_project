package servers.rm_command_handlers;

import servers.ActiveServerReplica;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

public class ActiveRMCommandDispatcher implements Runnable {

    ServerSocket rmSS;
    ActiveServerReplica server;
    Logger activeDispathcer = Logger.getLogger("activeDispathcer");
    public ActiveRMCommandDispatcher(ServerSocket rmSS, ActiveServerReplica server) {
        this.rmSS = rmSS;
        this.server = server;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Socket socket = rmSS.accept();
                ActiveRMCommandHandler handler = new ActiveRMCommandHandler(socket, server);
                new Thread(handler).start();
            } catch (Exception e) {
                activeDispathcer.info("Error in accepting connection request");
                e.printStackTrace();
                break;
            }
        }
    }
}
