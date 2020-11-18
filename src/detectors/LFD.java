package detectors;

import java.io.*;
import java.net.Socket;

public class LFD {
    private int lfdId;
    private int serverPortNumber;
    private String GFDAddress;
    private int GFDPortNumber;
    private int heartBeatFrequency;
    private final int defaultHeartBeatFrequency = 3000;
    private volatile boolean serverConnected;
    private volatile boolean serverRegister;
    private volatile boolean serverDeleter;

    public LFD(int serverPortNumber, String GFDAddress, int GFDPortNumber) {
        this.serverPortNumber = serverPortNumber;
        this.GFDAddress = GFDAddress;
        this.GFDPortNumber = GFDPortNumber;
        this.heartBeatFrequency = defaultHeartBeatFrequency;
        this.serverConnected = false;
        this.serverRegister = false;
        this.serverDeleter = false;
    }

    public void setLfdId(int id) {
        this.lfdId = id;
    }

    public static void main(String args[]) {
        int serverPortNumber = Integer.parseInt(args[0]);
        int GFDPortNumber = Integer.parseInt(args[1]);
        LFD lfd = new LFD(serverPortNumber, "127.0.0.1", GFDPortNumber);
        Thread serverThread = new ServerThread(lfd);
        Thread gfdThread = new GFDThread(lfd);
        gfdThread.start();
        serverThread.start();
    }

    static class ServerThread extends Thread {
        private static final String HEARTBEAT_MSG = "heartbeat";
        private LFD lfd;

        public ServerThread(LFD lfd) {
            this.lfd = lfd;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Socket socket = new Socket("127.0.0.1", lfd.serverPortNumber);
                    System.out.println("Server " + lfd.serverPortNumber + ": Alive");

                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    System.out.println("LFD " + lfd.lfdId + ": " + HEARTBEAT_MSG + " to server " + lfd.serverPortNumber);
                    out.writeUTF("LFD " + lfd.lfdId + ": " + HEARTBEAT_MSG);
                    if (!lfd.serverConnected) {
                        lfd.serverConnected = true;
                        lfd.serverRegister = true;
                    }

                } catch (IOException u) {
                    if (lfd.serverConnected) {
                        lfd.serverConnected = false;
                        lfd.serverDeleter = true;
                    }
                    System.out.println("LFD " + lfd.lfdId + ": " + "lost Connection with server " + lfd.serverPortNumber);
                }

                try {
                    Thread.sleep(lfd.heartBeatFrequency);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static class GFDThread extends Thread {
        private LFD lfd;

        public GFDThread(LFD lfd) {
            this.lfd = lfd;
        }

        @Override
        public void run() {
            Socket socket = null;
            DataOutputStream out = null;
            DataInputStream in = null;
            String message = null;
            int heartBeatNum = 1;
            while (true) {
                try {
                    socket = new Socket(lfd.GFDAddress, lfd.GFDPortNumber);
                    System.out.println("Heart beating with GFD");
                    out = new DataOutputStream(socket.getOutputStream());
                    in = new DataInputStream(socket.getInputStream());
                    out.writeUTF("LFD: " + socket.toString() + " connection request");
                    message = in.readUTF();
                    int lfdId = Integer.parseInt(message.split(" ")[5]);
                    lfd.setLfdId(lfdId);
                    break;
                } catch (IOException u) {
                    System.out.println("Can't connect GFD");

                }
                try {
                    Thread.sleep(lfd.heartBeatFrequency);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            while (true) {
                try {
                    if (lfd.serverConnected && lfd.serverRegister) {
                        out.writeUTF("LFD " + lfd.lfdId + ": add replica " + "S" + lfd.lfdId);
                        lfd.serverRegister = false;
                    }
                    if (!lfd.serverConnected && lfd.serverDeleter) {
                        out.writeUTF("LFD " + lfd.lfdId + ": delete replica " + "S" + lfd.lfdId);
                        lfd.serverDeleter = false;
                    }//use functions
                    out.writeUTF("LFD " + lfd.lfdId + ": heartbeat " + heartBeatNum++);
                    message = in.readUTF();
                    System.out.println("GFD: " + message);
                    Thread.sleep(lfd.heartBeatFrequency);
                } catch (Exception e) {
                    System.out.println("GFD is Closed");
                    return;
                }
            }

//inform the number of members
// which member is gone
            //set the frequency from users
            // include request number unique for client
            // print "S1--C1" include log for server
        }
    }
}

