package configurations;

import clients.Client;
import configurations.configs.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class Configuration {
    static {
        Locale.setDefault(new Locale("en", "EN"));
    }

    public enum ReplicationMode {Active, Passive}

    public enum RecoveryMode {Manual, Auto}

    private static final File CONFIG_FILE = new File("config.txt");

    private volatile static Configuration singleton;

    private ReplicationMode replicationMode;

    private RecoveryMode recoveryMode;

    private ServerReplicaConfig R1Config;
    private ServerReplicaConfig R2Config;
    private ServerReplicaConfig R3Config;

    private ClientConfig C1Config;
    private ClientConfig C2Config;
    private ClientConfig C3Config;

    private GFDConfig GFDConfig;

    private LFDConfig LFDConfig;

    private RMConfig RMConfig;

    public static Configuration getConfig() {
        if (singleton == null) {
            synchronized (Configuration.class) {
                if (singleton == null) {
                    singleton = new Configuration();
                }
            }
        }
        return singleton;
    }

    private Configuration() {
        Scanner scanner;
        try {
            scanner = new Scanner(CONFIG_FILE);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        while (scanner.hasNext()) {
            String line = scanner.nextLine();
            if (line.startsWith("ReplicationMode")) {
                parseReplicationModeConfig(line);
            } else if (line.startsWith("RecoveryMode")) {
                parseRecoveryModeConfig(line);
            } else if (line.startsWith("RM")) {
                parseRMConfig(line);
            } else if (line.startsWith("GFD")) {
                parseGFDConfig(line);
            } else if (line.startsWith("LFD")) {
                parseLFDConfig(line);
            } else if (line.startsWith("Replica")) {
                parseServerReplicaConfig(line);
            } else if (line.startsWith("Client")) {
                parseClientConfig(line);
            } else {
                if (!line.startsWith("//")) {
                    System.out.println("Impossible");
                }
            }
        }
    }

    private void parseReplicationModeConfig(String line) {
        if (line.contains("Active")) {
            replicationMode = ReplicationMode.Active;
        } else if (line.contains("Passive")) {
            replicationMode = ReplicationMode.Passive;
        } else {
            System.out.println("Impossible");
        }
    }

    private void parseRecoveryModeConfig(String line) {
        if (line.contains("Manual")) {
            recoveryMode = RecoveryMode.Manual;
        } else if (line.contains("Auto")) {
            recoveryMode = RecoveryMode.Auto;
        } else {
            System.out.println("Impossible");
        }
    }

    private void parseRMConfig(String line) {
        line = line.replaceAll("\\s", "");
        String[] idAndConfig = line.split(":");
        String[] configs = idAndConfig[1].split(",");
        int GFDServerPort = Integer.parseInt(configs[0]);
        int queryServerPort = Integer.parseInt(configs[1]);

        RMConfig = new RMConfig(GFDServerPort, queryServerPort);
    }

    private void parseServerReplicaConfig(String line) {
        line = line.replaceAll("\\s", "");
        String[] idAndConfig = line.split(":");
        String id = idAndConfig[0];
        String configLine = idAndConfig[1];

        String[] configs = configLine.split(",");
        int serverPort = Integer.parseInt(configs[0]);
        int rmCommandPort = Integer.parseInt(configs[1]);
        int checkpointPort = Integer.parseInt((configs[2]));

        if (id.equalsIgnoreCase("Replica1")) {
            R1Config = new ServerReplicaConfig(serverPort, rmCommandPort, checkpointPort);
        } else if (id.equalsIgnoreCase("Replica2")) {
            R2Config = new ServerReplicaConfig(serverPort, rmCommandPort, checkpointPort);
        } else if (id.equalsIgnoreCase("Replica3")) {
            R3Config = new ServerReplicaConfig(serverPort, rmCommandPort, checkpointPort);
        } else {
            System.out.println("Impossible");
        }
    }

    private void parseClientConfig(String line) {
        line = line.replaceAll("\\s", "");
        String[] idAndConfig = line.split(":");
        String id = idAndConfig[0];
        String configLine = idAndConfig[1];
        int requestFrequency = Integer.parseInt(configLine);

        if (id.equalsIgnoreCase("Client1")) {
            C1Config = new ClientConfig(requestFrequency);
        } else if (id.equalsIgnoreCase("Client2")) {
            C2Config = new ClientConfig(requestFrequency);
        } else if (id.equalsIgnoreCase("Client3")) {
            C3Config = new ClientConfig(requestFrequency);
        } else {
            System.out.println("Impossible");
        }
    }

    private void parseGFDConfig(String line) {
        line = line.replaceAll("\\s", "");
        String[] idAndConfig = line.split(":");
        int serverPort = Integer.parseInt(idAndConfig[1]);

        GFDConfig = new GFDConfig(serverPort);
    }

    private void parseLFDConfig(String line) {
        line = line.replaceAll("\\s", "");
        String[] idAndConfig = line.split(":");
        int heartBeatFrequency = Integer.parseInt(idAndConfig[1]);

        LFDConfig = new LFDConfig(heartBeatFrequency);
    }

    public ReplicationMode getReplicationMode() {
        return replicationMode;
    }

    public RecoveryMode getRecoveryMode() {
        return recoveryMode;
    }

    public ServerReplicaConfig getR1Config() {
        return R1Config;
    }

    public ServerReplicaConfig getR2Config() {
        return R2Config;
    }

    public ServerReplicaConfig getR3Config() {
        return R3Config;
    }

    public ClientConfig getC1Config() {
        return C1Config;
    }

    public ClientConfig getC2Config() {
        return C2Config;
    }

    public ClientConfig getC3Config() {
        return C3Config;
    }

    public GFDConfig getGFDConfig() {
        return GFDConfig;
    }

    public LFDConfig getLFDConfig() {
        return LFDConfig;
    }

    public RMConfig getRMConfig() {
        return RMConfig;
    }

    public List<Integer> getCheckPointPorts() {
        List<Integer> checkPointPorts = new ArrayList<>();

        Configuration config = Configuration.getConfig();
        checkPointPorts.add(config.getR1Config().getCheckpointPort());
        checkPointPorts.add(config.getR2Config().getCheckpointPort());
        checkPointPorts.add(config.getR3Config().getCheckpointPort());

        return checkPointPorts;
    }

    public List<Integer> getServerPorts() {
        List<Integer> serverPorts = new ArrayList<>();

        Configuration config = Configuration.getConfig();
        serverPorts.add(config.getR1Config().getServerPort());
        serverPorts.add(config.getR2Config().getServerPort());
        serverPorts.add(config.getR3Config().getServerPort());

        return serverPorts;
    }
}
