package servers;

import pojo.Interval;
import tasks.GameTask;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class ServerReplica {
    private final ConcurrentHashMap<Integer, Interval> state = new ConcurrentHashMap<>();

    public ConcurrentHashMap<Integer, Interval> getState() {
        return state;
    }

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(args[0]);
        ServerSocket ss = new ServerSocket(port);
        System.out.println("Server starts listening to: " + InetAddress.getLocalHost().getHostAddress() + ":" + port);

        ServerReplica serverReplica = new ServerReplica();

        while (true) {
            try {
                Socket socket = ss.accept();
                GameTask gameTask = new GameTask(socket, serverReplica.getState());
                new Thread(gameTask).start();
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
    }
//    static class Counter {
//        @SuppressWarnings("checkstyle:JavadocVariable")
//        public static long number = 0;
//
//        public synchronized void increase() {
//            number++;
//            System.out.println("Number of games played: " + number);
//        }
//    }
}

// timestamp, logger
// state should be modified by clients
// print message: timestamp, client ID, client message, state modification command
// updated state, updated timestamp
// number range as state, stored at server and returned to client
// ask for the game when game over