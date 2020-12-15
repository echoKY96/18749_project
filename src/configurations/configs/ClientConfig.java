package configurations.configs;

public class ClientConfig {
    int requestFrequency;

    public ClientConfig(int requestFrequency) {
        this.requestFrequency = requestFrequency;
    }

    public int getRequestFrequency() {
        return requestFrequency;
    }

    @Override
    public String toString() {
        return "ClientConfig{" +
                "requestFrequency=" + requestFrequency +
                '}';
    }
}
