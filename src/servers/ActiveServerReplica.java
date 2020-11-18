package servers;

import tasks.GameTask;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class ActiveServerReplica extends ServerReplica {

    public ActiveServerReplica(int listeningPort) {
        super(listeningPort);
    }

    @Override
    public void service() {
        ServerSocket ss;
        try {
            ss = new ServerSocket(listeningPort);
            System.out.println("Server starts listening to: " + InetAddress.getLocalHost().getHostAddress() + ":" + listeningPort);
        } catch (IOException e) {
            System.out.println("Server " + listeningPort + " failed to set up.");
            e.printStackTrace();
            return;
        }

        while (true) {
            try {
                Socket socket = ss.accept();
                GameTask gameTask = new GameTask(socket, this);
                new Thread(gameTask).start();
            } catch (Exception e) {
                System.out.println("Error in accepting connection request");
                e.printStackTrace();
                break;
            }
        }
    }

    public static void main(String[] args) {
        int listeningPort = Integer.parseInt(args[0]);
        ActiveServerReplica server = new ActiveServerReplica(listeningPort);

        System.out.println("Active server " + listeningPort + " to be set up");
        server.service();
    }
}
