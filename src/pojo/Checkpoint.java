package pojo;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

public class Checkpoint implements Serializable {
    private final ConcurrentHashMap<Integer, Interval> state;
    private final int checkpointCount;

    public Checkpoint(ConcurrentHashMap<Integer, Interval> state, int checkpointCount) {
        this.state = state;
        this.checkpointCount = checkpointCount;
    }

    public ConcurrentHashMap<Integer, Interval> getState() {
        return state;
    }

    public int getCheckpointCount() {
        return checkpointCount;
    }
}
// client id, request number, message:y/n
// response from where, timestamp
// use loggers for everything
