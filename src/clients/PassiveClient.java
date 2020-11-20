package clients;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class PassiveClient {
    private static final String localhost = "127.0.0.1";
    private static final Scanner in = new Scanner(System.in);
    private volatile static String response;

    public static void main(String[] args) {
        int clientId = Integer.parseInt(args[0]);
        for (int i = 1; i < args.length; i++) {
            int serverPort = Integer.parseInt(args[i]);
            // PassiveClient passiveClient = new PassiveClient(localhost, serverPort, clientId);
            Thread clientThread = new ClientThread(localhost, serverPort, clientId);
            clientThread.start();
        }

        response = "";
        while (!response.equalsIgnoreCase("exit")) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    static class ClientThread extends Thread {
        private final int clientId;
        private final static Object systemInLock = new Object();

        private final String serverAddress;
        private final int serverPort;

        public ClientThread(String serverAddress, int serverPort, int clientId) {
            this.clientId = clientId;
            this.serverAddress = serverAddress;
            this.serverPort = serverPort;
        }

        @Override
        public void run() {
            Socket socket;
            DataInputStream input;
            DataOutputStream out;

            /* Establish TCP/IP connection */
            try {
                socket = new Socket(serverAddress, serverPort);
                input = new DataInputStream(socket.getInputStream());
                out = new DataOutputStream(socket.getOutputStream());

            } catch (IOException u) {
                System.out.println("Server " + serverPort + " is not open");
                return;
            }

            /* Play the game with the primary server */
            while (true) {
                try {
                    String line = input.readUTF();
                    System.out.println(line);

                    if (line.contains("Game Over")) {
                        break;
                    }

                    // send to server: clientID + y/n
                    synchronized (systemInLock) {
                        response = in.next();
                        out.writeUTF(clientId + ": " + response);
                    }

                } catch (IOException e) {
                    System.out.println("Server " + serverPort + " breaks down");
                    break;
                }
            }

            /* Close the connection */
            try {
                input.close();
                out.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
