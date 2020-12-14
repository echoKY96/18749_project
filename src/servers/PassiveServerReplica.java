package servers;

import configurations.Configuration;
import servers.rm_command_handlers.PassiveRMCommandDispatcher;
import servers.services.PassiveTask;
import servers.checkpoint_tasks.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class PassiveServerReplica extends ServerReplica {

    /* Configuration info */
    private Integer primaryCheckpointPort = null;

    /* Server state */
    private Boolean isPrimary = false;

    @SuppressWarnings("FieldMayBeFinal")
    private AtomicInteger checkpointCount = new AtomicInteger(0);

    public PassiveServerReplica(Integer serverPort, Integer rmCommandPort, Integer checkpointPort) {
        super(serverPort, rmCommandPort, checkpointPort);
    }

    /* Getters and setters */

    public Integer getPrimaryCheckpointPort() {
        return primaryCheckpointPort;
    }

    public void setPrimaryCheckpointPort(Integer primaryCheckpointPort) {
        this.primaryCheckpointPort = primaryCheckpointPort;
    }

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
        return checkpointCount.get();
    }

    public void setCheckpointCount(Integer checkpointCount) {
        this.checkpointCount.getAndSet(checkpointCount);
    }

    public void incrementCheckpointCount() {
        this.checkpointCount.getAndIncrement();
    }

    public List<Integer> getBackupCheckpointPorts() {
        List<Integer> backupCheckpointPorts = new ArrayList<>();
        for (int checkpointPort : Configuration.getConfig().getCheckPointPorts()) {
            if (primaryCheckpointPort == checkpointPort) {
                continue;
            }
            backupCheckpointPorts.add(checkpointPort);
        }
        return backupCheckpointPorts;
    }

    @Override
    public void service() {
        ServerSocket serviceSS;
        ServerSocket rmSS;
        try {
            serviceSS = new ServerSocket(serverPort);
            rmSS= new ServerSocket(rmCommandPort);
            System.out.println("Passive Server: starts listening at client requests port: " + InetAddress.getLocalHost().getHostAddress() + ":" + serverPort);
            System.out.println("Passive Server: starts listening at RM commands port: " + InetAddress.getLocalHost().getHostAddress() + ":" + rmCommandPort);
        } catch (IOException e) {
            System.out.println("Passive Server: client request listening port: " + serverPort + " failed to set up.");
            System.out.println("Passive Server: rm command listening port: " + rmCommandPort + " failed to set up.");
            e.printStackTrace();
            return;
        }

        /* Launched as backup, not ready state */
        setNotReady();
        setBackup();

        /* Launch checkpoint task to receive checkpoint as backup or send checkpoint as primary */
        new Thread(new SendCheckPointTask(this)).start();
        new Thread(new ReceiveCheckpointTask(this)).start();

        /* Launch RM command dispatcher to receive RM command */
        Thread dispatcher = new Thread(new PassiveRMCommandDispatcher(rmSS, this));
        dispatcher.start();

        /* Provides service */
        while (true) {
            try {
                Socket socket = serviceSS.accept();
                new Thread(new PassiveTask(socket, this)).start();
            } catch (Exception e) {
                System.out.println("Server: Error in accepting connection request");
                e.printStackTrace();
                break;
            }
        }
    }
}