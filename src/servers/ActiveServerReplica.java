package servers;

import servers.server_threads.RMCommandDispatcher;
import tasks.ActiveTask;
import tasks.ReceiveCheckpointOneTime;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ActiveServerReplica extends ServerReplica {
    private static final String localhost = "127.0.0.1";
    private static final List<Integer> rmListeningPorts = Arrays.asList(10000, 10001, 10002);
    private static final List<Integer> checkpointPorts = Arrays.asList(10086, 10087, 10088);

    private final Integer checkpointPort;

    private AtomicBoolean checkpointing = new AtomicBoolean(false);

    public ActiveServerReplica(int listeningPort, int rmListeningPort, int checkpointPort) {
        super(listeningPort, rmListeningPort);
        this.checkpointPort = checkpointPort;
    }

    public static String getHostname() {
        return localhost;
    }

    public static List<Integer> getRmListeningPorts() {
        return rmListeningPorts;
    }

    public static List<Integer> getCheckpointPorts() {
        return checkpointPorts;
    }

    public Integer getCheckpointPort() {
        return checkpointPort;
    }

    public boolean isCheckpointing() {
        return checkpointing.get();
    }

    public void setCheckpointing() {
        this.checkpointing.getAndSet(true);
    }

    public void setNotCheckpointing() {
        this.checkpointing.getAndSet(false);
    }

    @Override
    public void service() {
        ServerSocket serviceSS;
        ServerSocket rmSS;
        try {
            serviceSS = new ServerSocket(listeningPort);
            rmSS= new ServerSocket(rmListeningPort);
            System.out.println("Server starts listening at clients: " + InetAddress.getLocalHost().getHostAddress() + ":" + listeningPort);
            System.out.println("Server starts listening at commands: " + InetAddress.getLocalHost().getHostAddress() + ":" + rmListeningPort);
        } catch (IOException e) {
            System.out.println("Server listening port: " + listeningPort + " failed to set up.");
            System.out.println("Server rm command port: " + rmListeningPort + " failed to set up.");
            e.printStackTrace();
            return;
        }

        /* New server added */
        if (checkOtherServersOnline()) {
            Thread receiver = new Thread(new ReceiveCheckpointOneTime(this));
            receiver.start();
        } else {
            setReady();
        }

        Thread dispatcher = new Thread(new RMCommandDispatcher(rmSS, this));
        dispatcher.start();

        while (true) {
            try {
                Socket socket = serviceSS.accept();
                ActiveTask task = new ActiveTask(socket, this);
                new Thread(task).start();
            } catch (Exception e) {
                System.out.println("Error in accepting connection request");
                e.printStackTrace();
                break;
            }
        }
    }

    public static void main(String[] args) {
        int listeningPort = Integer.parseInt(args[0]);
        int rmListeningPort = Integer.parseInt(args[1]);
        int checkpointPort = Integer.parseInt(args[2]);

        ActiveServerReplica server = new ActiveServerReplica(listeningPort, rmListeningPort, checkpointPort);

        System.out.println("Active server " + listeningPort + " to be set up");
        server.service();
    }
}
