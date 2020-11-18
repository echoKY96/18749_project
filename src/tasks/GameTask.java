package tasks;

import servers.ServerReplica;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.AbstractMap;
import java.util.Map;

public class GameTask implements Runnable {
    private static final String LFD_MSG = "heartbeat";
    private static final String PLAY_MSG = "play";
    private static final String REPLAY_MSG = "replay";
    private static final String YES_MSG = "y";
    private static final String NO_MSG = "n";
    private static final String EXIT_MSG = "exit";

    private final ServerReplica server;
    private final Socket socket;

    public GameTask(Socket socket, ServerReplica server) {
        this.socket = socket;
        this.server = server;
    }

    private void logRange(int clientId) {
        System.out.println("Client " + socket.getPort() + " state: [" + server.getLoById(clientId) + ", " + server.getHiById(clientId) + "]");
    }

    private Map.Entry<Integer, String> parseLine(String line) {
        String[] splitLine = line.split(": ");
        Integer clientId = Integer.parseInt(splitLine[0]);
        String message = splitLine[1];

        return new AbstractMap.SimpleEntry<>(clientId, message);
    }

    @Override
    public void run() {
        try {
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            DataInputStream dis = new DataInputStream(socket.getInputStream());

            dos.writeUTF("Server " + socket.getLocalPort() + " : Welcome, keep a number in range [1, 10] and I'll guess it, enter \"play\" to start a game.");

            while (true) {
                String line = dis.readUTF();

                if (line.contains(LFD_MSG)) {
                    /* LFD connection */
                    System.out.println(line);
                    break;
                } else {
                    /* Player connection */
                    Map.Entry<Integer, String> messageEntry = parseLine(line);
                    Integer clientId = messageEntry.getKey();
                    String message = messageEntry.getValue();

                    // logger
                    System.out.println("Client " + socket.getPort() + ": " + message);

                    if (message.equalsIgnoreCase(REPLAY_MSG) || (!server.containsClientState(clientId) && message.equalsIgnoreCase(PLAY_MSG))) {
                        server.clearStateById(clientId);
                        server.initStateById(clientId);

                        // logger
                        System.out.println("Start a new game for client " + socket.getPort());
                        logRange(clientId);

                        dos.writeUTF("Server " + socket.getLocalPort() + " : Is this number greater than " + server.getMidById(clientId) + " y/n?");
                    }

                    else if (server.containsClientState(clientId) && message.equalsIgnoreCase(YES_MSG)) {
                        int mid = server.getMidById(clientId);
                        server.setLoById(clientId, mid + 1);

                        if (server.getLoById(clientId) == server.getHiById(clientId)) {
                            server.clearStateById(clientId);

                            // logger
                            System.out.println("Game Over for client " + socket.getPort());

                            dos.writeUTF("Server " + socket.getLocalPort() + " : Your number is " + server.getLoById(clientId) + "\nGame Over");
                        } else {
                            // logger
                            logRange(clientId);

                            dos.writeUTF("Server " + socket.getLocalPort() + " : Is this number greater than " + server.getMidById(clientId) + " y/n?");
                        }
                    }

                    else if (server.containsClientState(clientId) && message.equalsIgnoreCase(NO_MSG)) {
                        int mid = server.getMidById(clientId);
                        server.setHiById(clientId, mid);

                        if (server.getLoById(clientId) == server.getHiById(clientId)) {
                            server.clearStateById(clientId);
                            // logger
                            System.out.println("Game Over for client " + socket.getPort());

                            dos.writeUTF("Server " + socket.getLocalPort() + " : Your number is " + server.getLoById(clientId) + "\nGame Over");
                        } else {
                            // logger
                            logRange(clientId);

                            dos.writeUTF("Server " + socket.getLocalPort() + " : Is this number greater than " + server.getMidById(clientId) + " y/n?");
                        }
                    }

                    else if (message.equalsIgnoreCase(EXIT_MSG)) {
                        server.clearStateById(clientId);

                        // logger
                        System.out.println("Game Over for client " + socket.getPort());

                        dos.writeUTF("Server " + socket.getLocalPort() + " : Game Over");
                        break; // exit command would disconnect from the server
                    }

                    else {
                        dos.writeUTF("Please enter a valid command.");
                    }
                }
            }

            dos.flush();
            dos.close();
            dis.close();
            socket.close();
        } catch (Exception e) {
            System.out.println("Client " + socket.getPort() + " Lost connection");
        }
    }
}
