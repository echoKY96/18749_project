package rm;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class RM {
    private static int GFDListeningPortNumber;
    private static int SeverListeningPortNumber;
    private static List<String> registerServers;
    private static final String NEW_ADD = "new server added";
    private static List<Integer> rmListeningPorts;

    static {
        GFDListeningPortNumber = 7000;
        SeverListeningPortNumber = 7001;
        registerServers = new ArrayList<>();
    }
    public static void main(String[] args) throws IOException{
        rmListeningPorts = new ArrayList<>();
        for (String arg: args) {
            rmListeningPorts.add(Integer.parseInt(arg));
        }
        ServerSocket gfd = new ServerSocket(GFDListeningPortNumber);
        ServerSocket  ss = new ServerSocket(SeverListeningPortNumber);
        System.out.println("RM: " + registerServers.size()+" member");
        new GFDHandleThread(gfd.accept()).start();
        new CheckFirstThread(ss.accept()).start();

    }
    static class CheckFirstThread extends Thread{
        private Socket serverSocket;
        public CheckFirstThread(Socket serverSocket){
            this.serverSocket = serverSocket;
        }
        @Override
        public void run() {
            super.run();
        }
    }
    static class GFDHandleThread extends Thread {
        private Socket gfdSocket;
        public GFDHandleThread(Socket gfdSocket) {
            this.gfdSocket = gfdSocket;
        }

        @Override
        public void run() {
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
                            if(server.equals("add")||server.equals("delete")){
                                break;
                            }
                            registerServers.add(server);
                        }
                    }
                    if(message.contains("add")){
                        for (int port: rmListeningPorts) {
                            new ServerThread(port).start();
                        }
                    }
                    System.out.println("RM: "+registerServers.size()+" member:"+registerServers);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }

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
                System.out.println("cannot connect "+serverPort);
                return;
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
