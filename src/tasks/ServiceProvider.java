package tasks;

import servers.ServerReplica;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.AbstractMap;
import java.util.Map;

public class ServiceProvider {
    private static final String PLAY_MSG = "play";
    private static final String REPLAY_MSG = "replay";
    private static final String YES_MSG = "y";
    private static final String NO_MSG = "n";
    private static final String EXIT_MSG = "exit";

    private final ServerReplica server;
    private final Socket socket;

    public ServiceProvider(Socket socket, ServerReplica server) {
        this.socket = socket;
        this.server = server;
    }

    public void logRange(int clientId) {
        System.out.println("Client " + socket.getPort() + " state: [" + server.getLoById(clientId) + ", " + server.getHiById(clientId) + "]");
    }

    public Map.Entry<Integer, String> parseLine(String line) {
        String[] splitLine = line.split(": ");
        Integer clientId = Integer.parseInt(splitLine[0]);
        String message = splitLine[1];

        return new AbstractMap.SimpleEntry<>(clientId, message);
    }

    public boolean gameService(DataOutputStream dos, Integer clientId, String message) throws IOException {

        if (message.equalsIgnoreCase(REPLAY_MSG) || (!server.containsClientState(clientId) && message.equalsIgnoreCase(PLAY_MSG))) {
            server.clearStateById(clientId);
            server.initStateById(clientId);

            // logger
            System.out.println("Start a new game for client " + socket.getPort());
            logRange(clientId);

            dos.writeUTF("Server " + socket.getLocalPort() + " : Is this number greater than " + server.getMidById(clientId) + " y/n?");
        } else if (message.equalsIgnoreCase(EXIT_MSG)) {
            server.clearStateById(clientId);

            // logger
            System.out.println("Game Over for client " + socket.getPort());

            dos.writeUTF("Server " + socket.getLocalPort() + " : Game Over");

            // exit command would disconnect from the server
            return false;
        } else if (server.containsClientState(clientId)) {
            int mid = server.getMidById(clientId);

            if (message.equalsIgnoreCase(YES_MSG)) {
                server.setLoById(clientId, mid + 1);
            } else if (message.equalsIgnoreCase(NO_MSG)) {
                server.setHiById(clientId, mid);
            } else {
                dos.writeUTF("Please enter a valid command.");
                return true;
            }

            if (server.getLoById(clientId) == server.getHiById(clientId)) {
                /* Game over*/
                dos.writeUTF("Server " + socket.getLocalPort() + " : Your number is " + server.getLoById(clientId) + "\nGame Over");

                server.clearStateById(clientId);

                // logger
                System.out.println("Game Over for client " + socket.getPort());

                return false;
            } else {
                /* Game continue */
                dos.writeUTF("Server " + socket.getLocalPort() + " : Is this number greater than " + server.getMidById(clientId) + " y/n?");

                // logger
                logRange(clientId);
            }
        } else {
            dos.writeUTF("Please enter a valid command.");
        }

        return true;
    }

    public void queuingService(String line) {
        /* Idle task keeps queueing messages from client but not responding */
        server.enqueue(line);
    }
}
