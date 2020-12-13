package servers;

import configurations.Configuration;
import servers.rm_command_handlers.ActiveRMCommandDispatcher;
import servers.services.ActiveTask;
import servers.checkpoint_tasks.ReceiveCheckpointOneTime;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class ActiveServerReplica extends ServerReplica {

    private static final String QUERY_ONLINE = "queryOnline";

    private final Integer rmQueryPort = Configuration.getConfig().getRMConfig().getQueryServerPort();

    private final Integer checkpointPort;

    private final AtomicBoolean checkpointing = new AtomicBoolean(false);

    private ActiveServerReplica(int serverPort, int rmCommandPort, int checkpointPort) {
        super(serverPort, rmCommandPort);
        this.checkpointPort = checkpointPort;
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

    public boolean queryOtherServersOnline() {
        Socket socket;
        DataOutputStream out;
        ObjectInputStream in;

        /* Establish TCP/IP connection */
        while (true) {
            try {
                socket = new Socket(hostName, rmQueryPort);
                out = new DataOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());
                break;
            } catch (IOException u) {
                System.out.println("Backup " + rmQueryPort + " is not open");
            }
        }

        Map<String,Boolean> map = null;

        /* Send checkpoint to a newly added server */
        try {
            out.writeUTF(QUERY_ONLINE);
            map = (HashMap<String,Boolean>)in.readObject();
        } catch (IOException e) {
            System.out.println("Error in sending checkpoint");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.out.println("Error in object type");
            e.printStackTrace();
        }

        /* Close the connection */
        try {
            socket.close();
        } catch (IOException e) {
            System.out.println("Error in closing socket");
            e.printStackTrace();
        }

        if (map == null) {
            System.out.println("Impossible");
        } else {
            for (Map.Entry<String, Boolean> entry : map.entrySet()) {
                String key = entry.getKey();
                Boolean exist = entry.getValue();

                if (!key.equals(String.valueOf(serverPort)) && exist) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void service() {
        ServerSocket serviceSS;
        ServerSocket rmSS;
        try {
            serviceSS = new ServerSocket(serverPort);
            rmSS= new ServerSocket(rmCommandPort);
            System.out.println("Active Server: starts listening at client requests port: " + InetAddress.getLocalHost().getHostAddress() + ":" + serverPort);
            System.out.println("Active Server: starts listening at RM commands port: " + InetAddress.getLocalHost().getHostAddress() + ":" + rmCommandPort);
        } catch (IOException e) {
            System.out.println("Active Server: client request listening port: " + serverPort + " failed to set up.");
            System.out.println("Active Server: rm command listening port: " + rmCommandPort + " failed to set up.");
            e.printStackTrace();
            return;
        }

        /* First server added is ready, others are not */
        if (queryOtherServersOnline()) {
            System.out.println("Active Server: Other servers online, receive checkpoint first");

            setNotReady();
            Thread receiver = new Thread(new ReceiveCheckpointOneTime(this));
            receiver.start();
        } else {
            setReady();
        }

        /* Launch RM command dispatcher to receive RM command */
        Thread dispatcher = new Thread(new ActiveRMCommandDispatcher(rmSS, this));
        dispatcher.start();

        /* Provides service */
        while (true) {
            try {
                Socket socket = serviceSS.accept();
                ActiveTask task = new ActiveTask(socket, this);
                new Thread(task).start();
            } catch (Exception e) {
                System.out.println("Active Server: Error in accepting connection request");
                e.printStackTrace();
                break;
            }
        }
    }

    public static void main(String[] args) {
        int id = Integer.parseInt(args[0]);
        Configuration config = Configuration.getConfig();

        int serverPort;
        int rmCommandPort;
        int checkpointPort;
        if (id == 1) {
            serverPort = config.getR1Config().getServerPort();
            rmCommandPort = config.getR1Config().getRmCommandPort();
            checkpointPort = config.getR1Config().getCheckpointPort();
        } else if (id == 2) {
            serverPort = config.getR2Config().getServerPort();
            rmCommandPort = config.getR2Config().getRmCommandPort();
            checkpointPort = config.getR3Config().getCheckpointPort();
        } else if (id == 3) {
            serverPort = config.getR3Config().getServerPort();
            rmCommandPort = config.getR3Config().getRmCommandPort();
            checkpointPort = config.getR3Config().getCheckpointPort();
        } else {
            System.out.println("Impossible");
            return;
        }

        ActiveServerReplica server = new ActiveServerReplica(serverPort, rmCommandPort, checkpointPort);
        server.service();
    }
}
