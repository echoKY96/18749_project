import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class Client {
    static Scanner in = null;
    private volatile static String response;
    private static String notified = "true";
    public static void main(String args[]) {
        int clientId = Integer.parseInt(args[0]);
        for (int i=1; i<args.length; i++) {
            Thread clientThread = new ClientThread("127.0.0.1", Integer.parseInt(args[i]),clientId);
            clientThread.start();
        }
        in = new Scanner(System.in);
        response = " ";
        while(!response.equals("Exit")) {
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
                System.out.println("Server "+serverPort+" is not open");
                return;
            }


            // keep reading until "Over" is input
            while (true) {
                try {
                    String line = input.readUTF();

                    String[] replies = line.split(":");
                    synchronized (messages) {
//                        System.out.println(messages);
                        if (!messages.contains(replies[1])) {
                            messages.add(replies[1]);
                            System.out.println(line);

                        } else {
                            System.out.println("Discard duplicate from " + replies[0]);
                        }
                    }
                    if(line.contains("Game Over")) {
                        break;
                    }
                    // sleep read thread


                    synchronized (notified) {
//                        System.out.println(Thread.currentThread());
                        notified.wait();
                    }
//                    System.out.println(response);
                    out.writeUTF(response);

                } catch (IOException | InterruptedException i) {
                    System.out.println("Server "+serverPort+" breaks down");
                    break;
                }
            }
            System.out.println("Connection is closed");
            // close the connection
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
