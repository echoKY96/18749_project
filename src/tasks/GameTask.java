package tasks;

import pojo.Interval;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class GameTask implements Runnable{
    private static final String LFD_MSG = "heartbeat";
    private final ConcurrentHashMap<Integer, Interval> state;
    private final Socket socket;

    public GameTask(Socket socket, ConcurrentHashMap<Integer, Interval> state) {
        this.socket = socket;
        this.state = state;
    }

    private void printRange(int clientId) {
        System.out.println("Number for client " + + socket.getPort() + ": [" + state.get(clientId).getLeft() + ", " + state.get(clientId).getRight() + "]");
    }

    @Override
    public void run() {
        try {
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            dos.writeUTF("Server " + socket.getLocalPort() + " : Welcome, please keep a number between from 1 to 10\nif you are ready, press y");

            DataInputStream dis = new DataInputStream(socket.getInputStream());
            String line = dis.readUTF();
            // System.out.println(line);

            if (line.contains(LFD_MSG)) {
                /* detectors.LFD connection */
                System.out.println(line);
            }

            else {
                /* clients.Client connection */
                String[] splitLine = line.split(": ");
                Integer clientId = Integer.parseInt(splitLine[0]);
                String isReady = splitLine[1];

                if (isReady.equals("y")) {

                    if (!state.containsKey(clientId)) {
                        /* Start a new game */
                        System.out.println("Start a new game for client " + socket.getPort());
                        state.put(clientId, new Interval());
                        printRange(clientId);
                    }

                    int start = state.get(clientId).getLeft();
                    int end = state.get(clientId).getRight();
                    while (start + 1 < end) {

                        int mid = (start + end) / 2;

                        dos.writeUTF("Server " + socket.getLocalPort() + " : Is this number greater than " + mid + " y/n?");

                        String response = dis.readUTF();
                        String yesOrNo = response.split(": ")[1];
                        System.out.println("clients.Client " + socket.getPort() + ": " + yesOrNo);

                        if (response.contains("y")) {
                            state.get(clientId).setLeft(mid);
                            start = state.get(clientId).getLeft();
                        }
                        if (response.contains("n")) {
                            state.get(clientId).setRight(mid);
                            end = state.get(clientId).getRight();
                        }

                        printRange(clientId);
                    }
                    // Number guessed, end the game
                    dos.writeUTF("Server " + socket.getLocalPort() + " : Is this number " + state.get(clientId).getRight() + " y/n?");
                    if (dis.readUTF().equals("y")) {
                        System.out.println("Number for client " + + socket.getPort() + " is " + state.get(clientId).getRight());
                        dos.writeUTF("Server " + socket.getLocalPort() + " : Your number is " + state.get(clientId).getRight() + "\nGame Over");
                    } else {
                        System.out.println("Number for client " + + socket.getPort() + " is " + state.get(clientId).getLeft());
                        dos.writeUTF("Server " + socket.getLocalPort() + " : Your number is " + state.get(clientId).getLeft() + "\nGame Over");
                    }
                    state.remove(clientId);
                    System.out.println("Game Over for client " + socket.getPort());
                } else {
                    state.remove(clientId);
                    dos.writeUTF("Game Over");
                }
            }

            dos.flush();
            dos.close();
            dis.close();
            socket.close();
        } catch (Exception e) {
            System.out.println("clients.Client " + socket.getPort() + " Lost connection");
        }
    }
}
