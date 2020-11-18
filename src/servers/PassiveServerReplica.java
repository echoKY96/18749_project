package servers;

import tasks.CheckPointReceiveTask;
import tasks.CheckPointSendTask;
import tasks.GameTask;
import tasks.IdleTask;

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

    private PassiveServerReplica(Integer listeningPort, Integer checkpointPort, Boolean isPrimary) {
        super(listeningPort);
        this.checkpointPort = checkpointPort;
        this.isPrimary = isPrimary;
    }

    /* Constructors */
    public static PassiveServerReplica getPrimaryServer(int listeningPort) {
        return new PassiveServerReplica(listeningPort, null, true);
    }

    public static PassiveServerReplica getBackupServer(int listeningPort, int checkpointPort) {
        return new PassiveServerReplica(listeningPort, checkpointPort, false);
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

        /* Primary server provides game service, Backup server stays idle */
        while (true) {
            try {
                Socket socket = ss.accept();
                if (isPrimary()) {
                    GameTask gameTask = new GameTask(socket, this);
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
            int checkpointPort = Integer.parseInt(args[2]);
            server = getBackupServer(listeningPort, checkpointPort);
        }

        System.out.println((isPrimary ? "Primary server " : "Backup server ") + listeningPort + " to be set up");

        server.service();
    }
}