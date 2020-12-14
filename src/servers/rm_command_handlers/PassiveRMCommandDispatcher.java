package servers.rm_command_handlers;

import servers.PassiveServerReplica;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

public class PassiveRMCommandDispatcher implements Runnable {

    ServerSocket rmSS;
    PassiveServerReplica server;
    Logger passiveDispatcher  = Logger.getLogger("passiveDispatcher");
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
                passiveDispatcher.info("Error in accepting connection request");
                e.printStackTrace();
                break;
            }
        }
    }
}
