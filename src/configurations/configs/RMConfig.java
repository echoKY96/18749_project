package configurations.configs;

public class RMConfig {
    private final Integer GFDServerPort;
    private final Integer queryServerPort;

    public RMConfig(Integer GFDServerPort, int queryServerPort) {
        this.GFDServerPort = GFDServerPort;
        this.queryServerPort = queryServerPort;
    }

    public Integer getGFDServerPort() {
        return GFDServerPort;
    }

    public Integer getQueryServerPort() {
        return queryServerPort;
    }

    @Override
    public String toString() {
        return "RMConfig{" +
                "GFDServerPort=" + GFDServerPort +
                ", queryServerPort=" + queryServerPort +
                '}';
    }
}
