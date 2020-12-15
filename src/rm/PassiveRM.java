package rm;

import configurations.Configuration;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

public class PassiveRM extends RM {

    private static String primaryServerPort = null;

    private static final String PRIMARY_CKPT_PORT = "primary_checkpoint_port";

    private static final String hostName = "127.0.0.1";
    private static final Logger PassiveRMLog = Logger.getLogger("PassiveRMLog");

    public static void setPrimaryServerPort(String primaryServerPort) {
        PassiveRM.primaryServerPort = primaryServerPort;
    }

    public void service() {
        ServerSocket gfd;
        try {
            gfd = new ServerSocket(GFDServerPort);
            PassiveRMLog.info("RM: " + registeredServers.size() + " member");
            /* Only one GFD, won't cause blocking once the only GFD is accepted */
            new GFDHandleThread(gfd.accept()).start();

        } catch (IOException e) {
            e.printStackTrace();
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

                        /* Register new server */
                        registrationMap.put(serverPort, true);

                        electPrimary();

                        /* Broadcast to all servers */
                        for (int rmCommandPort : rmToServerPortMap.keySet()) {
                            boolean online = registrationMap.get(rmToServerPortMap.get(rmCommandPort));
                            if (online) {
                                new SendCommandThread(rmCommandPort).start();
                            }
                        }
                    }

                    if (message.contains("delete")) {
                        String[] messages = message.split(" ");
                        String serverPort = messages[messages.length - 1];

                        /* Unregister deleted server */
                        registrationMap.put(serverPort, false);

                        electPrimary();

                        /* Broadcast to all servers */
                        for (int rmCommandPort : rmToServerPortMap.keySet()) {
                            boolean online = registrationMap.get(rmToServerPortMap.get(rmCommandPort));
                            if (online) {
                                new SendCommandThread(rmCommandPort).start();
                            }
                        }

                        /* Relaunch server */
                        if (Configuration.getConfig().getRecoveryMode() == Configuration.RecoveryMode.Auto) {
                            int serverId = getServerId(serverPort);
                            Runtime.getRuntime().exec(SERVER_LAUNCH_CMD + serverId);
                        }
                    }

                    PassiveRMLog.info("RM: " + registeredServers.size() + " member:" + registeredServers);
                    PassiveRMLog.info("RM: primary server port: " + primaryServerPort);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }

    private static void electPrimary() {
        boolean primaryOnline = false;
        for (String serverPort : registrationMap.keySet()) {
            boolean serverOnline = registrationMap.get(serverPort);
            if (serverOnline && serverPort.equals(primaryServerPort)) {
                primaryOnline = true;
            }
        }

        if (!primaryOnline) {
            /* Select a random online server as primary */
            for (String serverPort : registrationMap.keySet()) {
                boolean serverOnline = registrationMap.get(serverPort);
                if (serverOnline) {
                    setPrimaryServerPort(serverPort);
                    return;
                }
            }

            /* No online server */
            setPrimaryServerPort(null);
        }
    }

    /**
     * Send RM command to servers' rm listening port
     */
    private static class SendCommandThread extends Thread {
        private final int rmCommandPort;

        public SendCommandThread(int rmCommandPort) {
            this.rmCommandPort = rmCommandPort;
        }

        @Override
        public void run() {
            Socket socket;
            try {
                socket = new Socket(hostName, rmCommandPort);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            try {
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                out.writeUTF(PRIMARY_CKPT_PORT + ":" + serverToCheckpointPortMap.get(primaryServerPort));

                PassiveRMLog.info("RM: sent PRIMARY_CKPT_PORT command to: " + rmCommandPort);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
