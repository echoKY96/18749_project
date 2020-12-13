package clients;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Client {

    private static final String localhost = "127.0.0.1";
    private static final Scanner in = new Scanner(System.in);
    private static int request_num = 0;

    public static void main(String[] args) {
        int clientId = Integer.parseInt(args[0]);
        List<BlockingQueue<String>> mqList = new ArrayList<>();

        for (int i = 1; i < args.length; i++) {
            int serverPort = Integer.parseInt(args[i]);
            BlockingQueue<String> mq = new LinkedBlockingQueue<>();
            mqList.add(mq);

            Thread clientThread = new ClientThread(localhost, serverPort, clientId, mq);
            clientThread.start();
        }

        try {
            while (true) {
                String line = in.next();

                request_num++;
                for (BlockingQueue<String> mq : mqList) {
                    mq.put(line);
                }

                if (line.equalsIgnoreCase("exit")) {
                    break;
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    static private class ClientThread extends Thread {
        private final String serverAddress;
        private final int serverPort;
        private final int clientId;
        BlockingQueue<String> mq;

        private static final Set<String> responseContainer = new HashSet<>();
        private static final Object responseLock = new Object();


        public ClientThread(String serverAddress, int serverPort, int clientId, BlockingQueue<String> mq) {
            this.clientId = clientId;
            this.serverAddress = serverAddress;
            this.serverPort = serverPort;
            this.mq = mq;
        }

        @Override
        public void run() {
            Socket socket;
            DataInputStream input;
            DataOutputStream out;

            while (true) {
                try {
                    socket = new Socket(serverAddress, serverPort);
                    input = new DataInputStream(socket.getInputStream());
                    out = new DataOutputStream(socket.getOutputStream());
                    break;
                } catch (IOException u) {
                    System.out.println("Server " + serverPort + " is not open");
                }
            }

            /* Receive message from server */
            while (true) {
                try {
                    String response = input.readUTF();

                    if (response.contains("Welcome")) {
                        System.out.println(response);
                    } else {
                        String[] replies = response.split(": ");
                        String serverInfo = replies[0];
                        String serviceInfo = replies[1];
                        synchronized (responseLock) {
                            if (responseContainer.contains(serviceInfo)) {
                                System.out.println("Discard duplicate from " + serverInfo);
                            } else {
                                responseContainer.clear();
                                responseContainer.add(serviceInfo);
                                System.out.println(response);
                            }
                        }
                    }

                    String userInput = mq.take();

                    out.writeUTF(clientId + ": " + userInput + ": " + request_num);

                    if (userInput.equalsIgnoreCase("exit")) {
                        break;
                    }
                } catch (IOException e) {
                    System.out.println("Server " + serverPort + " breaks down");

                    mq.clear();
                    Thread clientThread = new ClientThread(localhost, serverPort, clientId, mq);
                    clientThread.start();

                    break;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // close the connection
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
