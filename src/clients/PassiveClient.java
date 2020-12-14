package clients;


import configurations.Configuration;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class PassiveClient extends Client{
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
            /* Receive from all active servers */
            try {
                for (int serverPort : activeSockets.keySet()) {
                    SocketStream socketStream = activeSockets.get(serverPort);
                    DataInputStream input = socketStream.getInput();

                    String response = input.readUTF();
                    System.out.println(response);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            /* Read user input */
            String userInput = systemIn.next();
            if (userInput.equalsIgnoreCase("exit")) {
                break;
            }

            /* Send to servers */
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
