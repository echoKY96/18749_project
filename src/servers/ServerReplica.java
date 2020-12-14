package servers;

import configurations.Configuration;
import pojo.Interval;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

abstract public class ServerReplica {

    protected volatile ConcurrentHashMap<Integer, Interval> state = new ConcurrentHashMap<>();

    protected final BlockingQueue<String> requestQueue = new LinkedBlockingQueue<>();

    protected final String hostName = "127.0.0.1";

    protected final Integer serverPort;

    protected final Integer rmCommandPort;

    protected final Integer checkpointPort;

    protected AtomicBoolean iAmReady = new AtomicBoolean(false);

    public ServerReplica(int serverPort, int rmCommandPort, int checkpointPort) {
        this.serverPort = serverPort;
        this.rmCommandPort = rmCommandPort;
        this.checkpointPort = checkpointPort;
    }

    public ConcurrentHashMap<Integer, Interval> getState() {
        return state;
    }

    @SuppressWarnings("unused")
    public String getHostName() {
        return hostName;
    }

    @SuppressWarnings("unused")
    public Integer getServerPort() {
        return serverPort;
    }

    @SuppressWarnings("unused")
    public Integer getRmCommandPort() {
        return rmCommandPort;
    }

    public Integer getCheckpointPort() {
        return checkpointPort;
    }

    public void setState(ConcurrentHashMap<Integer, Interval> state) {
        this.state = state;
    }

    public void logState() {
        System.out.println("State:");
        state.forEach((k, v) -> System.out.println("Client " + k + " state: [" + v.getLo() + ", " + v.getHi() + "]"));
    }

    public void logBacklog() {
        System.out.println(requestQueue);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isRQEmpty() {
        return this.requestQueue.isEmpty();
    }

    public void enqueue(String line) throws InterruptedException {
        this.requestQueue.put(line);
    }

    public String dequeue() throws InterruptedException {
        return this.requestQueue.take();
    }

    public void clearRQ() {
        this.requestQueue.clear();
    }

    public void setHiById(int clientId, int hi) {
        state.get(clientId).setHi(hi);
    }

    public void setLoById(int clientId, int lo) {
        state.get(clientId).setLo(lo);
    }

    public int getLoById(int clientId) {
        return state.get(clientId).getLo();
    }

    public int getHiById(int clientId) {
        return state.get(clientId).getHi();
    }

    public int getMidById(int clientId) {
        return state.get(clientId).getMid();
    }

    public void clearStateById(int clientId) {
        state.remove(clientId);
    }

    public void initStateById(int clientId) {
        state.put(clientId, Interval.getDeFaultInterval());
    }

    public boolean containsClientState(int clientId) {
        return state.containsKey(clientId);
    }

    public boolean isReady() {
        return iAmReady.get();
    }

    public void setReady() {
        this.iAmReady.getAndSet(true);
    }

    public void setNotReady() {
        this.iAmReady.getAndSet(false);
    }

    public abstract void service();

    public static void main(String[] args) {
        int id = Integer.parseInt(args[0]);
        Configuration config = Configuration.getConfig();

        Configuration.Mode mode = config.getMode();

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

        ServerReplica server;
        if (mode == Configuration.Mode.Active) {
            server = new ActiveServerReplica(serverPort, rmCommandPort, checkpointPort);
            server.service();
        } else if (mode == Configuration.Mode.Passive) {
            server = new PassiveServerReplica(serverPort, rmCommandPort, checkpointPort);
            server.service();
        } else {
            System.out.println("Impossible");
        }
    }
}

// timestamp, logger
// state should be modified by clients
// print message: timestamp, client ID, client message, state modification command
// updated state, updated timestamp
// number range as state, stored at server and returned to client
// ask for the game when game over