package configurations.configs;

public class GFDConfig {
    private final Integer serverPort;

    public GFDConfig(int serverPort) {
        this.serverPort = serverPort;
    }

    public Integer getServerPort() {
        return serverPort;
    }

    @Override
    public String toString() {
        return "GFDConfig{" +
                "serverPort=" + serverPort +
                '}';
    }
}
