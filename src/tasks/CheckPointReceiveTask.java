package tasks;

import pojo.Checkpoint;
import servers.PassiveServerReplica;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class CheckPointReceiveTask implements Runnable {
    private final PassiveServerReplica server;

    public CheckPointReceiveTask(PassiveServerReplica server) {
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
            try {
                Socket socket = ss.accept();

                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                Checkpoint checkpoint = (Checkpoint)in.readObject();

                server.setState(checkpoint.getState()); // thread safety: single thread is safe
                server.setCheckpointCount(checkpoint.getCheckpointCount());

                System.out.println("Checkpoint " + server.getCheckpointCount() + " received");
                server.logState();
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
    }
}
