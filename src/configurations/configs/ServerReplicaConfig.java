package configurations.configs;

public class ServerReplicaConfig {
    private final Integer serverPort;
    private final Integer rmCommandPort;
    private final Integer checkpointPort;

    public ServerReplicaConfig(int serverPort, int rmCommandPort, int checkpointPort) {
        this.serverPort = serverPort;
        this.rmCommandPort = rmCommandPort;
        this.checkpointPort = checkpointPort;
    }

    public Integer getServerPort() {
        return serverPort;
    }

    public Integer getRmCommandPort() {
        return rmCommandPort;
    }

    public Integer getCheckpointPort() {
        return checkpointPort;
    }

    @Override
    public String toString() {
        return "ServerReplicaConfig{" +
                "serverPort=" + serverPort +
                ", rmCommandPort=" + rmCommandPort +
                ", checkpointPort=" + checkpointPort +
                '}';
    }
}
