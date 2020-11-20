package servers;

import tasks.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class PassiveServerReplica extends ServerReplica {

    /* Configuration info about a primary - backups group */
    private static final List<Integer> backups = new ArrayList<>();
    private static final String localhost = "127.0.0.1";

    /* Server state */
    private final Integer checkpointPort;
    private Boolean isPrimary;
    private Integer checkpointCount = 0;

    private PassiveServerReplica(Integer listeningPort, Integer rmListeningPort, Integer checkpointPort, Boolean isPrimary) {
        super(listeningPort, rmListeningPort);
        this.checkpointPort = checkpointPort;
        this.isPrimary = isPrimary;
    }

    /* Constructors */
    public static PassiveServerReplica getPrimaryServer(int listeningPort, int rmListeningPort) {
        return new PassiveServerReplica(listeningPort, rmListeningPort, null, true);
    }

    public static PassiveServerReplica getBackupServer(int listeningPort, int rmListeningPort, int checkpointPort) {
        return new PassiveServerReplica(listeningPort, rmListeningPort, checkpointPort, false);
    }

    /* Getters and setters */
    public Boolean isPrimary() {
        return isPrimary;
    }

    @SuppressWarnings("unused")
    public void setPrimary() {
        isPrimary = true;
    }

    @SuppressWarnings("unused")
    public void setBackup() {
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

    public static List<Integer> getBackups() {
        return backups;
    }

    public static String getHostname() {
        return localhost;
    }

    public Integer getCheckpointPort() {
        return checkpointPort;
    }

    @Override
    public void service() {
        ServerSocket ss;
        try {
            ss = new ServerSocket(listeningPort);
            System.out.println("Server: starts listening to: " + InetAddress.getLocalHost().getHostAddress() + ":" + listeningPort);
        } catch (IOException e) {
            System.out.println("Server: " + listeningPort + " failed to set up.");
            e.printStackTrace();
            return;
        }

        if (isPrimary()) {
            setReady();

            SendCheckPointTask task = new SendCheckPointTask(this);
            new Thread(task).start();
        } else {
            setNotReady();

            ReceiveCheckPointTask task = new ReceiveCheckPointTask(this);
            new Thread(task).start();
        }

        /* Primary server provides game service, Backup server stays idle */
        while (true) {
            try {
                Socket socket = ss.accept();

                PassiveTask task = new PassiveTask(socket, this);
                new Thread(task).start();
            } catch (Exception e) {
                System.out.println("Server: Error in accepting connection request");
                e.printStackTrace();
                break;
            }
        }
    }

    public static void main(String[] args) {
        boolean isPrimary = Boolean.parseBoolean(args[0]);
        int listeningPort = Integer.parseInt(args[1]);
        int rmListeningPort = Integer.parseInt(args[2]);

        PassiveServerReplica server;
        if (isPrimary) {
            for (int i = 3; i < args.length; i++) {
                int backupPort = Integer.parseInt(args[i]);
                backups.add(backupPort);
            }
            server = getPrimaryServer(listeningPort, rmListeningPort);
        } else {
            int checkpointPort = Integer.parseInt(args[3]);
            server = getBackupServer(listeningPort, rmListeningPort, checkpointPort);
        }

        System.out.println("Server: " + (isPrimary ? "Primary server " : "Backup server ") + listeningPort + " to be set up");

        server.service();
    }
}