package servers;

import pojo.Interval;

import java.util.concurrent.ConcurrentHashMap;

abstract public class ServerReplica {
    protected ConcurrentHashMap<Integer, Interval> state = new ConcurrentHashMap<>();

    protected final Integer listeningPort;

    public ServerReplica(int listeningPort) {
        this.listeningPort = listeningPort;
    }

    public ConcurrentHashMap<Integer, Interval> getState() {
        return state;
    }

    public void setState(ConcurrentHashMap<Integer, Interval> state) {
        this.state = state;
    }

    public void logState() {
        state.forEach((k,v)-> System.out.println("Client " + k + " state: [" + v.getLo() + ", " + v.getHi() + "]"));
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

    public abstract void service();
}

// timestamp, logger
// state should be modified by clients
// print message: timestamp, client ID, client message, state modification command
// updated state, updated timestamp
// number range as state, stored at server and returned to client
// ask for the game when game over