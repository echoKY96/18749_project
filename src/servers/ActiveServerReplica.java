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
import java.util.logging.Logger;

public class ActiveServerReplica extends ServerReplica {

    private static final String QUERY_ONLINE = "queryOnline";

    private final Integer rmQueryPort = Configuration.getConfig().getRMConfig().getQueryServerPort();

    private final AtomicBoolean checkpointing = new AtomicBoolean(false);
    private static Logger activeServerReplicaLog = Logger.getLogger("activeServerReplicaLog");
    public ActiveServerReplica(int serverPort, int rmCommandPort, int checkpointPort) {
        super(serverPort, rmCommandPort, checkpointPort);
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
//                System.out.println("Backup " + rmQueryPort + " is not open");
                activeServerReplicaLog.info("Backup " + rmQueryPort + " is not open");
            }
        }

        Map<String,Boolean> map = null;

        /* Send checkpoint to a newly added server */
        try {
            out.writeUTF(QUERY_ONLINE);
            map = (HashMap<String,Boolean>)in.readObject();
        } catch (IOException e) {
//            System.out.println("Error in sending checkpoint");
            activeServerReplicaLog.info("Error in sending checkpoint");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
//            System.out.println("Error in object type");
            activeServerReplicaLog.info("Error in object type");
            e.printStackTrace();
        }

        /* Close the connection */
        try {
            socket.close();
        } catch (IOException e) {
//            System.out.println("Error in closing socket");
            activeServerReplicaLog.info("Error in closing socket");
            e.printStackTrace();
        }

        if (map == null) {
//            System.out.println("Impossible");
            activeServerReplicaLog.info("Impossible");
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
//            System.out.println("Active Server: starts listening at client requests port: " + InetAddress.getLocalHost().getHostAddress() + ":" + serverPort);
            activeServerReplicaLog.info("Active Server: starts listening at client requests port: " + InetAddress.getLocalHost().getHostAddress() + ":" + serverPort);
//            System.out.println("Active Server: starts listening at RM commands port: " + InetAddress.getLocalHost().getHostAddress() + ":" + rmCommandPort);
            activeServerReplicaLog.info("Active Server: starts listening at RM commands port: " + InetAddress.getLocalHost().getHostAddress() + ":" + rmCommandPort);
        } catch (IOException e) {
//            System.out.println("Active Server: client request listening port: " + serverPort + " failed to set up.");
            activeServerReplicaLog.info("Active Server: client request listening port: " + serverPort + " failed to set up.");
//            System.out.println("Active Server: rm command listening port: " + rmCommandPort + " failed to set up.");
            activeServerReplicaLog.info("Active Server: rm command listening port: " + rmCommandPort + " failed to set up.");
            e.printStackTrace();
            return;
        }

        /* First server added is ready, others are not */
        if (queryOtherServersOnline()) {
//            System.out.println("Active Server: Other servers online, receive checkpoint first");
            activeServerReplicaLog.info("Active Server: Other servers online, receive checkpoint first");
            setNotReady();
            Thread receiver = new Thread(new ReceiveCheckpointOneTime(this));
            receiver.start();
        } else {
//            System.out.println("Active Server: First active server, no checkpointing");
            activeServerReplicaLog.info("Active Server: First active server, no checkpointing");
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
//                System.out.println("Active Server: Error in accepting connection request");
                activeServerReplicaLog.info("Active Server: Error in accepting connection request");
                e.printStackTrace();
                break;
            }
        }
    }
}
