package detectors;

import configurations.Configuration;

import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GFD {
    private static int portNumber;
    private static List<String> registerServers;
    private static Map<String,Integer> serverPortMap;
    static {
        registerServers = new ArrayList<>();
        serverPortMap = new HashMap<>();
    }

    public static void main(String[] args) throws IOException {
        Configuration config = Configuration.getConfig();
        portNumber = config.getGFDConfig().getServerPort();
        serverPortMap.put(String.valueOf(config.getR1Config().getServerPort()), 1);
        serverPortMap.put(String.valueOf(config.getR2Config().getServerPort()), 2);
        serverPortMap.put(String.valueOf(config.getR3Config().getServerPort()), 3);

        Socket rmSocket = new Socket("127.0.0.1", config.getRMConfig().getGFDServerPort());
        RMHandleThread rmHandleThread = new RMHandleThread(rmSocket,"","");
        rmHandleThread.start();

        ServerSocket ss = new ServerSocket(portNumber);
        System.out.println("GFD is listening on port " + portNumber);
        System.out.println("GFD: 0 members");
        while (true) {
            LFDHandleThread handThread = new LFDHandleThread(ss.accept(),rmSocket);
            handThread.start();
        }

    }
    static class RMHandleThread extends Thread{
        private Socket rmSocket;
        private String operation;
        private String serverPort;
        public RMHandleThread(Socket rmSocket,String operation, String serverPort){
            this.rmSocket = rmSocket;
            this.operation = operation;
            this.serverPort = serverPort;
        }
        @Override
        public void run() {
            DataOutputStream out = null;
            try {
                out = new DataOutputStream(rmSocket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
            StringBuilder sb = new StringBuilder();
            for (String server: registerServers) {
                sb.append(server);
                sb.append(" ");
            }
            sb.append(operation);
            sb.append(" ");
            sb.append(serverPort);
            try {

                out.writeUTF(sb.toString());

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
    static class LFDHandleThread extends Thread {
        private Socket lfdSocket;
        private Socket rmSocket;
        private String severPort;
        public LFDHandleThread(Socket lfdSocket,Socket rmSocket) {
            this.lfdSocket = lfdSocket;
            this.rmSocket = rmSocket;
        }

        @Override
        public void run() {

            DataInputStream in = null;
            DataOutputStream out = null;
            int lfdNumber = 0;
            try {
                in = new DataInputStream(lfdSocket.getInputStream());
                out = new DataOutputStream(lfdSocket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (true) {
                try {
                    String message = in.readUTF();
                    System.out.println(message);
                    if (message.contains("connection request")) {
                        String[] messages = message.split(" ");
                        severPort = messages[messages.length-1];
                        lfdNumber = serverPortMap.get(severPort);
                        out.writeUTF("connection accept, your id is " + lfdNumber);
                    } else if (message.contains("add replica")) {
                        String[] messages = message.split(" ");
                        registerServers.add(messages[messages.length - 1]);
                        printMembers();
                        notifyRM("add",severPort);
                    } else if (message.contains("delete replica")) {
                        String[] messages = message.split(" ");
                        registerServers.remove(messages[messages.length - 1]);
                        printMembers();
                        notifyRM("delete",severPort);
                    } else {
                        out.writeUTF("heart beat received");//independent threads
                    }
                } catch (IOException e) {
                    System.out.println("LFD" + lfdNumber + " Lost Connection");
                    return;
                }

            }
        }
        private void printMembers() {
            System.out.println("GFD: " + registerServers.size() + " members: " + registerServers);
        }
        private void notifyRM(String operation,String serverPort){
            Thread rmHandleThread = new RMHandleThread(rmSocket,operation,serverPort);
            rmHandleThread.start();
            try {
                rmHandleThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
