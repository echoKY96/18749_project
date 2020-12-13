package servers.rm_command_handlers;

import servers.PassiveServerReplica;

import java.net.ServerSocket;
import java.net.Socket;

public class PassiveRMCommandDispatcher implements Runnable {

    ServerSocket rmSS;
    PassiveServerReplica server;

    public PassiveRMCommandDispatcher(ServerSocket rmSS, PassiveServerReplica server) {
        this.rmSS = rmSS;
        this.server = server;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Socket socket = rmSS.accept();
                PassiveRMCommandHandler handler = new PassiveRMCommandHandler(socket, server);
                new Thread(handler).start();
            } catch (Exception e) {
                System.out.println("Error in accepting connection request");
                e.printStackTrace();
                break;
            }
        }
    }
}
