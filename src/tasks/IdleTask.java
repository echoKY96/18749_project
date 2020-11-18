package tasks;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

public class IdleTask implements Runnable {
    private final Socket socket;

    public IdleTask(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        // DataOutputStream dos;
        DataInputStream dis;
        try {
            // dos = new DataOutputStream(socket.getOutputStream());
            dis = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            System.out.println("Input stream failed to set up");
            e.printStackTrace();
            return;
        }

        /* Idle task keeps reading message from client but not responding */
        while (true) {
            String line;
            try {
                line = dis.readUTF();
                System.out.println(line);
            } catch (IOException e) {
                System.out.println("Read error");
                e.printStackTrace();
                break;
            }
        }
    }
}
