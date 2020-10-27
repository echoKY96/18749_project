import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server_replica {
    private static int port;

    public static void main(String[] args) throws IOException {
        Counter counter = new Counter();
        port = Integer.parseInt(args[0]);
        ServerSocket ss = new ServerSocket(port);
        System.out.println("Server starts working");
        System.out.println("Local host ip:" + InetAddress.getLocalHost().getHostAddress().toString());

        while (true) {
            try {
                Socket socket = ss.accept();
                ReceiveThread receiveThread = new ReceiveThread(socket, counter);
                receiveThread.start();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}

class ReceiveThread extends Thread {
    private static final String LFDmessage = "heartbeat";
    private Socket socket;
    private Counter counter;

    public ReceiveThread(Socket socket, Counter counter) {
        this.socket = socket;
        this.counter = counter;
    }

    @Override
    public void run() {
        try {
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            dos.writeUTF("Server "+socket.getLocalPort()+" : Welcome, please keep a number between from 1 to 10\nif you are ready, press y");

            DataInputStream dis = new DataInputStream(socket.getInputStream());
            String isReady = dis.readUTF();

            if (isReady.equals("y")) {
                System.out.println("Message from client:" + socket.getPort() + " " + isReady);
                int start = 1;
                int end = 10;
                while (start + 1 < end) {
                    int mid = (start + end) / 2;

                    dos.writeUTF("Server "+socket.getLocalPort()+" : Is this number greater than " + mid + " y/n?");
                    String response = dis.readUTF();
                    System.out.println("Message from client:" + socket.getPort() + " " + response);
                    if (response.equals("y")) {
                        start = mid;
                    }
                    if (response.equals("n")) {
                        end = mid;
                    }
                }
                dos.writeUTF("Server "+socket.getPort()+" : Is this number " + end + " y/n?");
                if (dis.readUTF().equals("y")) {
                    dos.writeUTF("Server "+socket.getLocalPort()+" : Your number is " + end + "\nGame Over");
                } else {
                    dos.writeUTF("Server "+socket.getLocalPort()+" : Your number is " + start + "\nGame Over");
                }
                System.out.println("Game Over for client:" + socket.getPort());
                counter.increase();
            } else if (isReady.equals(LFDmessage)) {
                System.out.println("Message from LFD");
            } else {
                dos.writeUTF("Game Over");
            }

            dos.flush();
            dos.close();
            dis.close();
            socket.close();
        } catch (Exception e) {
            System.out.println("Client "+socket.getPort()+" Lost connection");;
        }

    }
}

class Counter {
    @SuppressWarnings("checkstyle:JavadocVariable")
    public static long number = 0;

    public synchronized void increase() {
        number++;
        System.out.println("Number of games played: " + number);
    }
}

// timestamp, logger
// state should be modified by clients
// print message: timestamp, client ID, client message, state modification command
// updated state, updated timestamp
// number range as state, stored at server and returned to client
// ask for the game when game over