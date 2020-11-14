package clients;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class Client {
    private static final String localhost = "127.0.0.1";
    private static Scanner in = new Scanner(System.in);
    private volatile static String response;
    private static String notified = "true";

    public static void main(String args[]) {
        int clientId = Integer.parseInt(args[0]);
        for (int i = 1; i < args.length; i++) {
            int serverPort = Integer.parseInt(args[i]);
            Thread clientThread = new ClientThread(localhost, serverPort, clientId);
            clientThread.start();
        }

        response = "";
        while (!response.equalsIgnoreCase("exit")) {
            try {
                Thread.currentThread().sleep(100);
                synchronized (notified) {
                    response = in.next();
                    notified.notifyAll();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    static class ClientThread extends Thread {
        private int clientId;
        private static Set<String> messages;
        private static Object messagesLock = new Object();


        private String serverAddress;
        private int serverPort;

        static {
            messages = new HashSet<>();
        }

        public ClientThread(String serverAddress, int serverPort, int clientId) {
            this.clientId = clientId;
            this.serverAddress = serverAddress;
            this.serverPort = serverPort;
        }

        @Override
        public void run() {
            Socket socket = null;
            DataInputStream input = null;
            DataOutputStream out = null;

            try {
                socket = new Socket(serverAddress, serverPort);
                // takes input from terminal
                input = new DataInputStream(socket.getInputStream());

                // sends output to the socket
                out = new DataOutputStream(socket.getOutputStream());

            } catch (IOException u) {
                System.out.println("Server " + serverPort + " is not open");
                return;
            }


            // Receive message from server
            while (true) {
                try {
                    String line = input.readUTF();
                    // System.out.println(line);

                    if (line.contains("Game Over")) {
                        break;
                    }

                    String[] replies = line.split(":");
                    String serverInfo = replies[0];
                    String serviceInfo = replies[1];
                    synchronized (messagesLock) {
//                        System.out.println(messages);
                        if (!messages.contains(serviceInfo)) {
                            messages.add(serviceInfo);
                            System.out.println(line);
                        } else {
                            System.out.println("Discard duplicate from " + serverInfo);
                        }
                    }

                    // system.in lock, get y/n from stdin
                    synchronized (notified) {
//                        System.out.println(Thread.currentThread());
                        notified.wait();
                    }
//                    System.out.println(response);
                    // send to clientID and server y/n
                    out.writeUTF(clientId + ": " + response);

                } catch (IOException | InterruptedException i) {
                    System.out.println("Server " + serverPort + " breaks down");
                    break;
                }
            }

            // close the connection
            // System.out.println("Connection is closed");
            try {
                input.close();
                out.close();
                socket.close();
            } catch (IOException i) {
                System.out.println(i);
            }
        }
    }
}
