package rm;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class RM {
    private static int portNumber;
    private static List<String> registerServers;
    private static final String NEW_ADD = "new server added";
    private static List<Integer> serverPorts;

    static {
        portNumber = 7000;
        registerServers = new ArrayList<>();
    }
    public static void main(String[] args) throws IOException{
        for (String arg: args) {
            serverPorts.add(Integer.parseInt(arg));
        }
        ServerSocket ss = new ServerSocket(portNumber);
        System.out.println("RM: " + registerServers.size()+" member");
        while(true){
            GFDHandle(ss.accept());
        }
    }
    private static void GFDHandle(Socket gfdSocket) {
        DataInputStream in = null;
        try {
            in = new DataInputStream(gfdSocket.getInputStream());

        } catch (IOException e) {
            e.printStackTrace();
        }
        while (true) {
            try {
                String message = in.readUTF();
                registerServers.clear();
                if(message.length()!=0){
                    String[] servers = message.split(" ");
                    for (String server: servers) {
                        registerServers.add(server);
                    }
                }
                for (int port: serverPorts) {
                    new ServerThread(port).start();
                }
                System.out.println("RM: "+registerServers.size()+" member:"+message);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

        }
    }
    static class ServerThread extends Thread{
        private int serverPort;
        public ServerThread(int serverPort){
            this.serverPort = serverPort;

        }
        @Override
        public void run() {
            Socket socket = null;
            try {
                socket = new Socket("127.0.0.1", serverPort);

            } catch (IOException e) {
                System.out.println("connection fail");
            }
            DataOutputStream out = null;

            try {
                out = new DataOutputStream(socket.getOutputStream());
                out.writeUTF(NEW_ADD);
            } catch (IOException e) {
                System.out.println("OutPut Exception");
            }

        }


    }
}
