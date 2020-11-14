package servers;

import pojo.Interval;
import tasks.CheckPointReceiveTask;
import tasks.CheckPointSendTask;
import tasks.GameTask;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class PassiveServerReplica {

    /* Configuration info about a primary - backups group */
    private static final List<Integer> backups = new ArrayList<>();
    private static final String localhost = "127.0.0.1";

    /* Server state */
    private final Integer listeningPort;
    private final Integer checkpointPort;
    private Boolean isPrimary;
    private ConcurrentHashMap<Integer, Interval> state;
    private Integer checkpointCount;

    private PassiveServerReplica(Integer listeningPort, Integer checkpointPort, Boolean isPrimary) {
        this.listeningPort = listeningPort;
        this.checkpointPort = checkpointPort;
        this.isPrimary = isPrimary;
        this.state = new ConcurrentHashMap<>();
        this.checkpointCount = 0;
    }

    public static PassiveServerReplica getPrimaryServer(int listeningPort) {
        return new PassiveServerReplica(listeningPort, null, true);
    }

    public static PassiveServerReplica getSlaveServer(int listeningPort, int checkpointCount) {
        return new PassiveServerReplica(listeningPort, checkpointCount, false);
    }

    public ConcurrentHashMap<Integer, Interval> getState() {
        return state;
    }

    public void setState(ConcurrentHashMap<Integer, Interval> state) {
        this.state = state;
    }

    public Boolean isPrimary() {
        return isPrimary;
    }

    @SuppressWarnings("unused")
    public void setPrimary() {
        isPrimary = true;
    }

    @SuppressWarnings("unused")
    public void setSlave() {
        isPrimary = false;
    }

    public Integer getCheckpointCount() {
        return checkpointCount;
    }

    public void setCheckpointCount(Integer checkpointCount) {
        this.checkpointCount = checkpointCount;
    }

    public void incrementCheckpointCount() {
        this.checkpointCount = this.checkpointCount + 1;
    }

    public static List<Integer> getSlaves() {
        return backups;
    }

    public static String getHostname() {
        return localhost;
    }

    public Integer getCheckpointPort() {
        return checkpointPort;
    }

    static class IdleTask implements Runnable {
        private final Socket socket;

        public IdleTask(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            // DataOutputStream dos;
            DataInputStream dis;
            try {
                // dos = new DataOutputStream(socket.getOutputStream());
                dis = new DataInputStream(socket.getInputStream());
            } catch (IOException e) {
                System.out.println("Input stream failed to set up");
                e.printStackTrace();
                return;
            }

            /* Idle task keeps reading message from client but not responding */
            while (true) {
                String line;
                try {
                    line = dis.readUTF();
                    System.out.println(line);
                } catch (IOException e) {
                    System.out.println("Read error");
                    e.printStackTrace();
                    break;
                }
            }
        }
    }

    public void service() {
        ServerSocket ss;
        try {
            ss = new ServerSocket(listeningPort);
            System.out.println("Server starts listening to: " + InetAddress.getLocalHost().getHostAddress() + ":" + listeningPort);
        } catch (IOException e) {
            System.out.println("Server " + listeningPort + " failed to set up.");
            e.printStackTrace();
            return;
        }

        if (isPrimary()) {
            CheckPointSendTask task = new CheckPointSendTask(this);
            new Thread(task).start();
        } else {
            CheckPointReceiveTask task = new CheckPointReceiveTask(this);
            new Thread(task).start();
        }

        /* Primary server provides game service, Slave server stays idle */
        while (true) {
            try {
                Socket socket = ss.accept();
                if (isPrimary()) {
                    GameTask gameTask = new GameTask(socket, getState());
                    new Thread(gameTask).start();
                } else {
                    IdleTask idleTask = new IdleTask(socket);
                    new Thread(idleTask).start();
                }
            } catch (Exception e) {
                System.out.println("Error in accepting connection request");
                e.printStackTrace();
                break;
            }
        }
    }

    public static void main(String[] args) {
        int listeningPort = Integer.parseInt(args[0]);
        boolean isPrimary = Boolean.parseBoolean(args[1]);

        PassiveServerReplica server;
        if (isPrimary) {
            for (int i = 2; i < args.length; i++) {
                int backupPort = Integer.parseInt(args[i]);
                backups.add(backupPort);
            }
            server = getPrimaryServer(listeningPort);
        } else {
            int checkpointCount = Integer.parseInt(args[2]);
            server = getSlaveServer(listeningPort, checkpointCount);
        }

        System.out.println((isPrimary ? "Primary server " : "Backup server " ) + listeningPort + " to be set up");

        server.service();
    }
}