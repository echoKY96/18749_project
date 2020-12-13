package servers;

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

    protected AtomicBoolean iAmReady = new AtomicBoolean(false);

    public ServerReplica(int serverPort, int rmCommandPort) {
        this.serverPort = serverPort;
        this.rmCommandPort = rmCommandPort;
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

    public void setState(ConcurrentHashMap<Integer, Interval> state) {
        this.state = state;
    }

    public void logState() {
        state.forEach((k, v) -> System.out.println("Client " + k + " state: [" + v.getLo() + ", " + v.getHi() + "]"));
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
}

// timestamp, logger
// state should be modified by clients
// print message: timestamp, client ID, client message, state modification command
// updated state, updated timestamp
// number range as state, stored at server and returned to client
// ask for the game when game over