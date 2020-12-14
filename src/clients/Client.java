package clients;

import configurations.Configuration;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.*;

abstract public class Client {
    protected static final String localhost = "127.0.0.1";

    protected final Scanner systemIn = new Scanner(System.in);

    protected final int clientId;

    protected int request_num = 0;

    /* key: server port; value: active socket */
    protected Map<Integer, SocketStream> activeSockets = new HashMap<>();

    public Client(int clientId) {
        this.clientId = clientId;
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

        /* Add to activeSockets */
        activeSockets.put(serverPort, new SocketStream(socket, input, output));

        return true;
    }

    public static void main(String[] args) {
        int clientId = Integer.parseInt(args[0]);

        Configuration config = Configuration.getConfig();

        Client client;
        if (config.getMode() == Configuration.Mode.Active) {
            System.out.println("Active client " + clientId);
            client = new ActiveClient(clientId);
        } else if (config.getMode() == Configuration.Mode.Passive) {
            System.out.println("Passive client " + clientId);
            client = new PassiveClient(clientId);
        } else {
            System.out.println("Impossible");
            return;
        }

        client.play();
    }
}
