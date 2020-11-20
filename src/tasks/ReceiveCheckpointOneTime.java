package tasks;

import pojo.Checkpoint;
import servers.ActiveServerReplica;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ReceiveCheckpointOneTime implements Runnable {
    private final ActiveServerReplica server;

    public ReceiveCheckpointOneTime(ActiveServerReplica server) {
        this.server = server;
    }

    @Override
    public void run() {
        ServerSocket ss;
        try {
            ss = new ServerSocket(server.getCheckpointPort());
        } catch (IOException e) {
            System.out.println("Listening error");
            e.printStackTrace();
            return;
        }

        while (true) {
            Socket socket;
            try {
                socket = ss.accept();
            } catch (Exception e) {
                System.out.println("Accept failed");
                e.printStackTrace();
                continue;
            }

            try {
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                Checkpoint checkpoint = (Checkpoint) in.readObject();

                server.setState(checkpoint.getState()); // thread safety: volatile ensures visibility

                System.out.println("Checkpoint " + " received");
                server.logState();

                server.setReady();
                /* Checkpoint received, terminate the thread */
                break;
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}
