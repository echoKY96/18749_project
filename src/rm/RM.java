package rm;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class RM {
    private static int portNumber;
    private static List<String> registerServers;
    static {
        portNumber = 7000;
        registerServers = new ArrayList<>();
    }
    public static void main(String[] args) throws IOException{

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
                System.out.println("RM: "+registerServers.size()+" member:"+message);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

        }
    }
}
