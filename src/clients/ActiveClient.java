package clients;

import configurations.Configuration;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

public class ActiveClient extends Client{
    private final Set<String> responseContainer = new HashSet<>();

    public ActiveClient(int clientId) {
        super(clientId);
    }

    @Override
    protected void play() {
        /* First connection */
        for (int serverPort : Configuration.getConfig().getServerPorts()) {
            connect(serverPort);
        }

        while (true) {
            /* Receive from all active servers */
            try {
                responseContainer.clear();

                for (int serverPort : activeSockets.keySet()) {
                    SocketStream socketStream = activeSockets.get(serverPort);
                    DataInputStream input = socketStream.getInput();

                    String response = input.readUTF();
                    String[] replies = response.split(": ");
                    String serverInfo = replies[0];
                    String serviceInfo = replies[1];

                    if (responseContainer.contains(serviceInfo)) {
                        System.out.println("Discard duplicate from " + serverInfo);
                    } else {
                        responseContainer.add(serviceInfo);
                        System.out.println(response);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            /* Read user input */
            String userInput = systemIn.next();
            if (userInput.equalsIgnoreCase("exit")) {
                break;
            }

            /* Try to connect all servers, and send to them */
            for (int serverPort : Configuration.getConfig().getServerPorts()) {
                if (!activeSockets.containsKey(serverPort)) {
                    if (!connect(serverPort)) {
                        continue;
                    }
                }

                SocketStream socketStream = activeSockets.get(serverPort);
                Socket socket = socketStream.getSocket();
                DataOutputStream output = socketStream.getOutput();
                DataInputStream input = socketStream.getInput();

                try {
                    output.writeUTF(clientId + ": " + userInput + ": " + request_num);
                } catch (IOException e) {
                    /* close the connection */
                    System.out.println("Server " + serverPort + " breaks down");
                    try {
                        input.close();
                        output.close();
                        socket.close();
                        activeSockets.remove(serverPort);
                    } catch (IOException i) {
                        i.printStackTrace();
                    }
                }

                request_num++;
            }
        }
    }
}
