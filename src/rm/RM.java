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
    private static Map<Integer,String> rmListeningPorts;
    private static Map<String,Boolean> serverPortMap;
    private static List<String> serverPorts;
    static {
        GFDListeningPortNumber = 7000;
        queryListeningPort = 7001;
        registerServers = new ArrayList<>();
        serverPortMap = new HashMap<>();
        serverPorts = new ArrayList<>();
        serverPorts.add("8080");
        serverPorts.add("8081");
        serverPorts.add("8082");

        for (String port: serverPorts) {
            serverPortMap.put(port,false);
        }

    }

    public static void main(String[] args) throws IOException {
        rmListeningPorts = new HashMap<>();
        for (int i=0; i< rmListeningPorts.size();i++) {
            rmListeningPorts.put(Integer.parseInt(args[i]),serverPorts.get(i));
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
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());

                String line = dis.readUTF();
                System.out.println(line);

                if (line.equalsIgnoreCase(QUERY_NUM)) {
                    oos.writeObject(serverPortMap);
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

//                        System.out.println(serverPortMap);
                        for (int rmListeningPort : rmListeningPorts.keySet()) {
                            if(serverPortMap.get(rmListeningPort)){
                                new RMCommandThread(rmListeningPort).start();
                            }
                        }
                        String[] messages = message.split(" ");
                        serverPortMap.put(messages[messages.length-1],true);

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

            try {
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                out.writeUTF(NEW_ADD);
            } catch (IOException e) {
                System.out.println("OutPut Exception");
            }
        }
    }
}
