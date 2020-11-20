package servers;

import pojo.Interval;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

abstract public class ServerReplica {
    private static final String hostname = "127.0.0.1";

    private static final Integer rmQueryPort = 7001;

    private static final String QUERY_NUM = "queryNum";

    protected volatile ConcurrentHashMap<Integer, Interval> state = new ConcurrentHashMap<>();

    protected final BlockingQueue<String> requestQueue = new LinkedBlockingQueue<>();

    protected final Integer listeningPort;

    protected final Integer rmListeningPort;

    protected AtomicBoolean iAmReady = new AtomicBoolean(false);

    public ServerReplica(int listeningPort, int rmListeningPort) {
        this.listeningPort = listeningPort;
        this.rmListeningPort = rmListeningPort;
    }

    public ConcurrentHashMap<Integer, Interval> getState() {
        return state;
    }

    public void setState(ConcurrentHashMap<Integer, Interval> state) {
        this.state = state;
    }

    public void logState() {
        state.forEach((k, v) -> System.out.println("Client " + k + " state: [" + v.getLo() + ", " + v.getHi() + "]"));
    }

    public boolean isRQEmpty() {
        return this.requestQueue.isEmpty();
    }

    public void enqueue(String line) {
        this.requestQueue.offer(line);
    }

    public String dequeue() {
        return this.requestQueue.poll();
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

    public Integer queryServerNum() {
        Socket socket;
        DataOutputStream out;
        DataInputStream in;

        /* Establish TCP/IP connection */
        while (true) {
            try {
                socket = new Socket(hostname, rmQueryPort);
                out = new DataOutputStream(socket.getOutputStream());
                in = new DataInputStream(socket.getInputStream());
                break;
            } catch (IOException u) {
                System.out.println("Backup " + rmQueryPort + " is not open");
            }
        }

        Integer result = null;

        /* Send checkpoint to a newly added server */
        try {
            out.writeUTF(QUERY_NUM);
            result = Integer.parseInt(in.readUTF());
        } catch (IOException e) {
            System.out.println("Error in sending checkpoint");
            e.printStackTrace();
        }

        /* Close the connection */
        try {
            socket.close();
        } catch (IOException e) {
            System.out.println("Error in closing socket");
            e.printStackTrace();
        }

        return result;
    }

    public abstract void service();
}

// timestamp, logger
// state should be modified by clients
// print message: timestamp, client ID, client message, state modification command
// updated state, updated timestamp
// number range as state, stored at server and returned to client
// ask for the game when game over