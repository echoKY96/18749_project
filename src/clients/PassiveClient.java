package clients;


import configurations.Configuration;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class PassiveClient extends Client {
    public PassiveClient(int clientId) {
        super(clientId);
    }

    @Override
    protected void play() {
        /* First connection */
        for (int serverPort : Configuration.getConfig().getServerPorts()) {
            connect(serverPort);
        }

        while (true) {
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
            }
            request_num++;

            /* Receive from all active servers */
            for (int serverPort : Configuration.getConfig().getServerPorts()) {
                if (!activeSockets.containsKey(serverPort)) {
                    continue;
                }
                SocketStream socketStream = activeSockets.get(serverPort);
                Socket socket = socketStream.getSocket();
                DataOutputStream output = socketStream.getOutput();
                DataInputStream input = socketStream.getInput();
                try {
                    String response = input.readUTF();
                    if (!response.contains("Idle")) {
                        System.out.println(response);
                    }
                } catch (SocketTimeoutException i) {
                    /* close the connection */
                    System.out.println("Server " + serverPort + " time out, breaks down");
                    try {
                        input.close();
                        output.close();
                        socket.close();
                        activeSockets.remove(serverPort);
                    } catch (IOException k) {
                        k.printStackTrace();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
