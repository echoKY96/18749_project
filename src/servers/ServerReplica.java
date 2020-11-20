package servers;

import pojo.Interval;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

abstract public class ServerReplica {
    private static final String hostname = "127.0.0.1";

    private static final Integer rmQueryPort = 7001;

    private static final String QUERY_ONLINE = "queryOnline";

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

    public boolean checkOtherServersOnline() {
        Socket socket;
        DataOutputStream out;
        ObjectInputStream in;

        /* Establish TCP/IP connection */
        while (true) {
            try {
                socket = new Socket(hostname, rmQueryPort);
                out = new DataOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());
                break;
            } catch (IOException u) {
                System.out.println("Backup " + rmQueryPort + " is not open");
            }
        }

        Map<String,Boolean> map = null;

        /* Send checkpoint to a newly added server */
        try {
            out.writeUTF(QUERY_ONLINE);
            map = (HashMap<String,Boolean>)in.readObject();
        } catch (IOException e) {
            System.out.println("Error in sending checkpoint");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.out.println("Error in object type");
            e.printStackTrace();
        }

        /* Close the connection */
        try {
            socket.close();
        } catch (IOException e) {
            System.out.println("Error in closing socket");
            e.printStackTrace();
        }

        if (map == null) {
            System.out.println("Impossible");
        } else {
            for (Map.Entry<String, Boolean> entry : map.entrySet()) {
                String key = entry.getKey();
                Boolean exist = entry.getValue();

                if (!key.equals(String.valueOf(listeningPort)) && exist) {
                    return true;
                }
            }
        }
        return false;
    }

    public abstract void service();
}

// timestamp, logger
// state should be modified by clients
// print message: timestamp, client ID, client message, state modification command
// updated state, updated timestamp
// number range as state, stored at server and returned to client
// ask for the game when game over