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

        ServerSocket ss = new ServerSocket(portNumber);
        System.out.println("GFD is listening on port " + portNumber);
        System.out.println("GFD: " + LFDHandleThread.threadCount + " members");
        while (true) {
            LFDHandleThread handThread = new LFDHandleThread(ss.accept());
            handThread.start();
        }

    }

    static class LFDHandleThread extends Thread {
        private Socket lfdSocket;
        private volatile static int threadCount = 0;

        public LFDHandleThread(Socket lfdSocket) {
            this.lfdSocket = lfdSocket;
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
                        System.out.println("Registered Servers: " + registerServers);
                    } else if (message.contains("delete replica")) {
                        String[] messages = message.split(" ");
                        registerServers.remove(messages[messages.length - 1]);
                        System.out.println("Registered Servers: " + registerServers);
                    } else {
                        out.writeUTF("GFD has received heart beat number");//independent threads
                    }
                } catch (IOException e) {
                    System.out.println("LFD" + threadCount + " Lost Connection");
                    return;
                }

            }
        }
    }
}
