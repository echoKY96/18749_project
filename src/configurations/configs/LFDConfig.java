package configurations.configs;

public class LFDConfig {
    int heartBeatFrequency;

    public LFDConfig(int heartBeatFrequency) {
        this.heartBeatFrequency = heartBeatFrequency;
    }

    public int getHeartBeatFrequency() {
        return heartBeatFrequency;
    }

    @Override
    public String toString() {
        return "LFDConfig{" +
                "heartBeatFrequency=" + heartBeatFrequency +
                '}';
    }
}
