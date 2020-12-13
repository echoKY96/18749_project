package servers.rm_command_handlers;

import servers.ActiveServerReplica;

import java.net.ServerSocket;
import java.net.Socket;

public class ActiveRMCommandDispatcher implements Runnable {

    ServerSocket rmSS;
    ActiveServerReplica server;

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
                System.out.println("Error in accepting connection request");
                e.printStackTrace();
                break;
            }
        }
    }
}
