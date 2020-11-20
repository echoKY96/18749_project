package servers.server_threads;

import servers.ActiveServerReplica;
import tasks.ActiveTask;
import tasks.RMCommandHandler;

import java.net.ServerSocket;
import java.net.Socket;

public class RMCommandDispatcher implements Runnable {

    ServerSocket rmSS;
    ActiveServerReplica server;

    public RMCommandDispatcher(ServerSocket rmSS, ActiveServerReplica server) {
        this.rmSS = rmSS;
        this.server = server;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Socket socket = rmSS.accept();
                RMCommandHandler handler = new RMCommandHandler(socket, server);
                new Thread(handler).start();
            } catch (Exception e) {
                System.out.println("Error in accepting connection request");
                e.printStackTrace();
                break;
            }
        }
    }
}
