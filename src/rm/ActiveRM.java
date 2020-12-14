package rm;

import configurations.Configuration;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

public class ActiveRM extends RM {

    private static final String NEW_ADD = "new_add";
    private static final String QUERY_ONLINE = "queryOnline";
    private static final Logger activeLog = Logger.getLogger("activeLog");
    public void service() {
        ServerSocket gfd;
        ServerSocket ss;
        try {
            gfd = new ServerSocket(GFDServerPort);
            ss = new ServerSocket(queryServerPort);
//            System.out.println("RM: " + registeredServers.size() + " member");
            activeLog.info("RM: " + registeredServers.size() + " member");
            /* Only one GFD, won't cause blocking once the only GFD is accepted */
            new GFDHandleThread(gfd.accept()).start();

            while (true) {
                new queryHandleThread(ss.accept()).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class queryHandleThread extends Thread {
        private final Socket socket;

        public queryHandleThread(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                DataInputStream dis = new DataInputStream(socket.getInputStream());
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());

                String line = dis.readUTF();

                if (line.equalsIgnoreCase(QUERY_ONLINE)) {
                    oos.writeObject(registrationMap);

//                    System.out.println("Active new server " + socket.getPort() + " query members");
                    activeLog.info("Active new server " + socket.getPort() + " query members");
                } else {
//                    System.out.println("Impossible");
                    activeLog.info("Impossible");
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static class GFDHandleThread extends Thread {
        private final Socket gfdSocket;

        public GFDHandleThread(Socket gfdSocket) {
            this.gfdSocket = gfdSocket;
        }

        @Override
        public void run() {
            DataInputStream in;
            try {
                in = new DataInputStream(gfdSocket.getInputStream());

            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            while (true) {
                try {
                    String message = in.readUTF();
                    registeredServers.clear();
                    if (message.length() != 0) {
                        String[] servers = message.split(" ");
                        for (String server : servers) {
                            if (server.equals("add") || server.equals("delete")) {
                                break;
                            }
                            registeredServers.add(server);
                        }
                    }

                    if (message.contains("add")) {
                        String[] messages = message.split(" ");
                        String serverPort = messages[messages.length - 1];
                        int checkpointPort = serverToCheckpointPortMap.get(serverPort);

                        // Update the map after sending commands
                        registrationMap.put(serverPort, true);

                        /* Broadcast NEW_ADD to all servers */
                        for (int rmCommandPort : rmCommandPorts) {
                            String port = rmToServerPortMap.get(rmCommandPort);
                            if (registrationMap.get(port)) {
                                new SendCommandThread(rmCommandPort, checkpointPort).start();
                            }
                        }
                    }
                    if (message.contains("delete")) {
                        String[] messages = message.split(" ");
                        String serverPort = messages[messages.length - 1];
                        registrationMap.put(serverPort, false);

                        // restart server
                        int serverId = getServerId(serverPort);
                        Runtime.getRuntime().exec(SERVER_LAUNCH_CMD + serverId);
                    }
//                    System.out.println("RM: " + registeredServers.size() + " member:" + registeredServers);
                        activeLog.info("RM: " + registeredServers.size() + " member:" + registeredServers);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }

    /**
     * Send RM command to servers' rm listening port
     */
    private static class SendCommandThread extends Thread {
        private final int rmCommandPort;
        private final int newAddCheckpointPort;

        public SendCommandThread(int rmCommandPort, int newAddCheckpointPort) {
            this.rmCommandPort = rmCommandPort;
            this.newAddCheckpointPort = newAddCheckpointPort;
        }

        @Override
        public void run() {
            Socket socket;
            try {
                socket = new Socket("127.0.0.1", rmCommandPort);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            try {
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                out.writeUTF(NEW_ADD + ":" + newAddCheckpointPort);

//                System.out.println("RM: sent NEW_ADD command to: " + rmCommandPort);
                activeLog.info("RM: sent NEW_ADD command to: " + rmCommandPort);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
