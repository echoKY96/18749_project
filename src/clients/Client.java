package clients;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class Client {
    private static final String localhost = "127.0.0.1";
    private static Scanner in = new Scanner(System.in);
    private volatile static String response="";
    private static String notified = "true";
    private static int request_num = 0;
    public static void main(String args[]) {
        int clientId = Integer.parseInt(args[0]);
        for (int i = 1; i < args.length; i++) {
            int serverPort = Integer.parseInt(args[i]);
            Thread clientThread = new ClientThread(localhost, serverPort, clientId);
            clientThread.start();
        }


    }

    static class ClientThread extends Thread {

        private int clientId;
        private static Set<String> messages;
        private static Object messagesLock = new Object();
        private static AtomicInteger count = new AtomicInteger(1);
        private static int threadCount = 0;
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
            boolean reconnect = false;
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
            threadCount++;

            // Receive message from server
            while (true) {
                try {
                    String line = input.readUTF();

//                    System.out.println(1);


                    String[] replies = line.split(":");
                    String serverInfo = replies[0];
                    String serviceInfo = replies[1];
                    synchronized (messagesLock) {

                        if (!messages.contains(serviceInfo)) {
                            messages.add(serviceInfo);
                            System.out.println(line);
                        } else {
                            System.out.println("Discard duplicate from " + serverInfo);
                        }
                    }
                    if (line.contains("Game Over")) {
                        System.out.println(line);
                        getResponse();
                        if(response.equals("exit")){
                            break;
                        }
                    }
                    else{
                        getResponse();
                    }


                    out.writeUTF(clientId + ": " + response);

                } catch (IOException e) {
                    System.out.println("Server " + serverPort + " breaks down");
                    break;
                }
            }

            // close the connection
            try {
                input.close();
                out.close();
                socket.close();
            } catch (IOException i) {
                System.out.println(i);
            }
        }
        private void getResponse(){
            if(count.get()==threadCount){
                response = in.next();
                if(response.equals("replay")){
                    messages.clear();
                }
                synchronized (notified){
                    notified.notifyAll();
                }
            }
            else{
                synchronized (notified) {
                    count.addAndGet(1);
                    try {
                        notified.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

}
