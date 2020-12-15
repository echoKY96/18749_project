package clients;

import configurations.Configuration;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;

abstract public class Client {
    protected static final String localhost = "127.0.0.1";

    protected final Scanner systemIn = new Scanner(System.in);

    protected final int clientId;

    protected final int requestFrequency;

    protected int request_num = 0;

    protected boolean gameOver = true;

    protected static boolean isManual;

    /* key: server port; value: active socket */
    protected Map<Integer, SocketStream> activeSockets = new HashMap<>();

    public Client(int clientId, int requestFrequency) {
        this.clientId = clientId;
        this.requestFrequency = requestFrequency;
    }

    protected static class SocketStream {
        Socket socket;
        DataInputStream input;
        DataOutputStream output;

        public SocketStream(Socket socket, DataInputStream input, DataOutputStream output) {
            this.socket = socket;
            this.input = input;
            this.output = output;
        }

        public Socket getSocket() {
            return socket;
        }

        public DataInputStream getInput() {
            return input;
        }

        public DataOutputStream getOutput() {
            return output;
        }

        @Override
        public String toString() {
            return "SocketStream{" +
                    "socket=" + socket +
                    ", input=" + input +
                    ", output=" + output +
                    '}';
        }
    }

    protected abstract void play();

    protected boolean connect(int serverPort) {
        Socket socket;
        DataInputStream input;
        DataOutputStream output;

        try {
            socket = new Socket(localhost, serverPort);
            input = new DataInputStream(socket.getInputStream());
            output = new DataOutputStream(socket.getOutputStream());
        } catch (IOException u) {
            System.out.println("Server " + serverPort + " is not open");
            return false;
        }

        try {
            socket.setSoTimeout(2000);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        /* Add to activeSockets */
        activeSockets.put(serverPort, new SocketStream(socket, input, output));

        System.out.println("Server " + serverPort + " connected");
        return true;
    }

    protected String getRandomInput() {
        if (gameOver) {
            gameOver = false;
            return "play";
        }

        Random rand = new Random();

        int randInt = rand.nextInt(2);

        if (randInt == 0) {
            return "y";
        } else {
            return "n";
        }
    }

    public static void main(String[] args) {
        int clientId = Integer.parseInt(args[0]);
        isManual = args[1].equalsIgnoreCase("Manual");

        Configuration config = Configuration.getConfig();

        int requestFrequency;
        if (clientId == 1) {
            requestFrequency = config.getC1Config().getRequestFrequency();
        } else if (clientId == 2) {
            requestFrequency = config.getC2Config().getRequestFrequency();
        } else if (clientId == 3) {
            requestFrequency = config.getC3Config().getRequestFrequency();
        } else {
            System.out.println("Impossible");
            return;
        }

        Client client;
        if (config.getReplicationMode() == Configuration.ReplicationMode.Active) {
            System.out.println("Active client " + clientId);
            client = new ActiveClient(clientId, requestFrequency);
        } else if (config.getReplicationMode() == Configuration.ReplicationMode.Passive) {
            System.out.println("Passive client " + clientId);
            client = new PassiveClient(clientId, requestFrequency);
        } else {
            System.out.println("Impossible");
            return;
        }

        client.play();
    }
}
