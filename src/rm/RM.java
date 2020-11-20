package rm;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RM {
    private static int GFDListeningPortNumber;
    private static int queryListeningPort;
    private static List<String> registerServers;
    private static final String NEW_ADD = "new server added";
    private static final String QUERY_NUM = "queryNum";
    private static List<Integer> rmListeningPorts;
    private static Map<String,Boolean> serverPortMap;
    static {
        GFDListeningPortNumber = 7000;
        queryListeningPort = 7001;
        registerServers = new ArrayList<>();
        serverPortMap = new HashMap<>();
        serverPortMap.put("8080",false);
        serverPortMap.put("8081",false);
        serverPortMap.put("8082",false);
    }

    public static void main(String[] args) throws IOException {
        rmListeningPorts = new ArrayList<>();
        for (String arg : args) {
            rmListeningPorts.add(Integer.parseInt(arg));
        }
        ServerSocket gfd = new ServerSocket(GFDListeningPortNumber);
        ServerSocket ss = new ServerSocket(queryListeningPort);
        System.out.println("RM: " + registerServers.size() + " member");
        new GFDHandleThread(gfd.accept()).start();
        while (true) {
            new queryHandleThread(ss.accept()).start();
        }
    }

    static class queryHandleThread extends Thread {
        private Socket socket;

        public queryHandleThread(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                DataInputStream dis = new DataInputStream(socket.getInputStream());
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

                String line = dis.readUTF();
                System.out.println(line);

                if (line.equalsIgnoreCase(QUERY_NUM)) {
                    ObjectOutputStream mapStream = new ObjectOutputStream(dos);
                    mapStream.writeObject(serverPortMap);
//                  dos.writeUTF(String.valueOf(serverPortMap ));
                } else {
                    System.out.println("Impossible");
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static class GFDHandleThread extends Thread {
        private Socket gfdSocket;

        public GFDHandleThread(Socket gfdSocket) {
            this.gfdSocket = gfdSocket;
        }

        @Override
        public void run() {
            DataInputStream in = null;
            try {
                in = new DataInputStream(gfdSocket.getInputStream());

            } catch (IOException e) {
                e.printStackTrace();
            }
            while (true) {
                try {
                    String message = in.readUTF();
                    registerServers.clear();
                    if (message.length() != 0) {
                        String[] servers = message.split(" ");
                        for (String server : servers) {
                            if (server.equals("add") || server.equals("delete")) {
                                break;
                            }
                            registerServers.add(server);
                        }
                    }
                    if (message.contains("add")) {
                        String[] messages = message.split(" ");
                        serverPortMap.put(messages[messages.length-1],true);
//                        System.out.println(serverPortMap);
                        for (int rmListeningPort : rmListeningPorts) {

                            new RMCommandThread(rmListeningPort).start();
                        }

                    }
                    if(message.contains("delete")){
                        String[] messages = message.split(" ");
                        serverPortMap.put(messages[messages.length-1],false);
//                        System.out.println(serverPortMap);
                    }
                    System.out.println("RM: " + registerServers.size() + " member:" + registerServers);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }

            }
        }
    }

    /**
     * Send a command to
     */
    static class RMCommandThread extends Thread {
        private int rmListeningPort;

        public RMCommandThread(int rmListeningPort) {
            this.rmListeningPort = rmListeningPort;

        }

        @Override
        public void run() {
            Socket socket = null;
            try {
                socket = new Socket("127.0.0.1", rmListeningPort);

            } catch (IOException e) {
                System.out.println("cannot connect " + rmListeningPort);
                return;
            }
            DataOutputStream out = null;

            try {
                out = new DataOutputStream(socket.getOutputStream());
                out.writeUTF(NEW_ADD);
            } catch (IOException e) {
                System.out.println("OutPut Exception");
            }
        }
    }
}
