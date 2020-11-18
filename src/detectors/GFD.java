package detectors;

import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class GFD {
    private static int portNumber;
    private static List<String> registerServers;

    static {
        portNumber = 6000;
        registerServers = new ArrayList<>();
    }

    public static void main(String[] args) throws IOException {
        int rmPortNumber = Integer.parseInt(args[0]);
        Socket rmSocket = new Socket("127.0.0.1", rmPortNumber);
        RMHandleThread rmHandleThread = new RMHandleThread(rmSocket);
        rmHandleThread.start();

        ServerSocket ss = new ServerSocket(portNumber);
        System.out.println("GFD is listening on port " + portNumber);
        System.out.println("GFD: " + LFDHandleThread.threadCount + " members");
        while (true) {
            LFDHandleThread handThread = new LFDHandleThread(ss.accept(),rmSocket);
            handThread.start();
        }

    }
    static class RMHandleThread extends Thread{
        private Socket rmSocket;
        public RMHandleThread(Socket rmSocket){
            this.rmSocket = rmSocket;
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
        private volatile static int threadCount = 0;

        public LFDHandleThread(Socket lfdSocket,Socket rmSocket) {
            this.lfdSocket = lfdSocket;
            this.rmSocket = rmSocket;
        }

        @Override
        public void run() {
            threadCount++;
            DataInputStream in = null;
            DataOutputStream out = null;
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
                        out.writeUTF("connection accept, your id is " + threadCount);
                    } else if (message.contains("add replica")) {
                        String[] messages = message.split(" ");
                        registerServers.add(messages[messages.length - 1]);
                        printMembers();
                        notifyRM();
                    } else if (message.contains("delete replica")) {
                        String[] messages = message.split(" ");
                        registerServers.remove(messages[messages.length - 1]);
                        printMembers();
                        notifyRM();
                    } else {
                        out.writeUTF("heart beat received");//independent threads
                    }
                } catch (IOException e) {
                    System.out.println("LFD" + threadCount + " Lost Connection");
                    return;
                }

            }
        }
        private void printMembers() {
            System.out.println("GFD: " + registerServers.size() + " members: " + registerServers);
        }
        private void notifyRM(){
            Thread rmHandleThread = new RMHandleThread(rmSocket);
            rmHandleThread.start();
            try {
                rmHandleThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
