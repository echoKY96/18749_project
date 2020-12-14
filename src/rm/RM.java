package rm;

import configurations.Configuration;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

abstract public class RM {
    protected static Configuration.Mode mode;

    protected static int GFDServerPort;
    protected static int queryServerPort;

    protected static final List<String> registeredServers;
    protected static final List<String> serverPorts;
    protected static final List<Integer> rmCommandPorts;
    protected static final List<Integer> checkpointPorts;
    protected static final Map<String, Boolean> registrationMap;
    protected static final Map<Integer, String> rmToServerPortMap;
    protected static final Map<Integer, Integer> rmToCheckpointPortMap;
    protected static final Map<String, Integer> serverToCheckpointPortMap;

    protected static final String SERVER_LAUNCH_CMD = "cmd /c start ServerLauncher.bat ";
    private static final Logger RMLog;
    static {
        registeredServers = new ArrayList<>();
        serverPorts = new ArrayList<>();
        rmCommandPorts = new ArrayList<>();
        checkpointPorts = new ArrayList<>();
        registrationMap = new HashMap<>();
        rmToServerPortMap = new HashMap<>();
        rmToCheckpointPortMap = new HashMap<>();
        serverToCheckpointPortMap = new HashMap<>();
        RMLog = Logger.getLogger("RMLog");
        RMLog.setLevel(Level.INFO);
        Locale.setDefault(new Locale("en", "EN"));
    }

    /**
     * Convert serverPort to server ID
     * @param serverPort serverPort
     */
    protected static int getServerId(String serverPort) {
        int sp = Integer.parseInt(serverPort);
        Configuration config = Configuration.getConfig();
        if (sp == config.getR1Config().getServerPort()) {
            return 1;
        } else if (sp == config.getR2Config().getServerPort()) {
            return 2;
        } else if (sp == config.getR3Config().getServerPort()) {
            return 3;
        } else {
            RMLog.info("Impossible");
            return -1;
        }
    }

    /**
     * Read configuration information
     */
    protected void readConfiguration() {
        Configuration config = Configuration.getConfig();

        mode = config.getMode();

        GFDServerPort = config.getRMConfig().getGFDServerPort();
        queryServerPort = config.getRMConfig().getQueryServerPort();

        serverPorts.add(String.valueOf(config.getR1Config().getServerPort()));
        serverPorts.add(String.valueOf(config.getR2Config().getServerPort()));
        serverPorts.add(String.valueOf(config.getR3Config().getServerPort()));
        rmCommandPorts.add(config.getR1Config().getRmCommandPort());
        rmCommandPorts.add(config.getR2Config().getRmCommandPort());
        rmCommandPorts.add(config.getR3Config().getRmCommandPort());
        checkpointPorts.add(config.getR1Config().getCheckpointPort());
        checkpointPorts.add(config.getR2Config().getCheckpointPort());
        checkpointPorts.add(config.getR3Config().getCheckpointPort());

        for (int i = 0; i < serverPorts.size(); i++) {
            String serverPort = serverPorts.get(i);
            int rmCommandPort = rmCommandPorts.get(i);
            int checkpointPort = checkpointPorts.get(i);

            registrationMap.put(serverPort, false);
            rmToServerPortMap.put(rmCommandPort, serverPort);
            rmToCheckpointPortMap.put(rmCommandPort, checkpointPort);
            serverToCheckpointPortMap.put(serverPort, checkpointPort);
        }

        RMLog.info("Reading configuration:");
        RMLog.info("Server port - Registered: " + registrationMap);
        RMLog.info("RM command listening port - Server port: " + rmToServerPortMap);
        RMLog.info("RM command listening port - Checkpoint receiving port: " + rmToCheckpointPortMap);
        RMLog.info("Server port - Checkpoint receiving port: " + serverToCheckpointPortMap);
    }

    public abstract void service();

    public static void main(String[] args) {
        Configuration config = Configuration.getConfig();
        RM rm;
        if (config.getMode() == Configuration.Mode.Active) {
            RMLog.info("Active RM: running");
            rm = new ActiveRM();
        } else if (config.getMode() == Configuration.Mode.Passive) {
            RMLog.info("Passive RM: running");
            rm = new PassiveRM();
        } else {
            RMLog.info("Impossible");
            return;
        }
        rm.readConfiguration();
        rm.service();
    }
}
// timestamp
// client
// sign of sending
// rechecking connect: change to
// automatic client request: freq
// print i_am_ready, print queue, thread_safe queue
// kill a port command
// manual or both
