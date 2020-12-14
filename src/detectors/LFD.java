package detectors;

import configurations.Configuration;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Locale;
import java.util.logging.Logger;

public class LFD {
    static {
        Locale.setDefault(new Locale("en", "EN"));
        LFDLog = Logger.getLogger("LFDLog");
    }

    private int lfdId;
    private final int serverPortNumber;
    private final String GFDAddress;
    private final int GFDPortNumber;
    private final int heartBeatFrequency;
    private volatile boolean serverConnected;
    private volatile boolean serverRegister;
    private volatile boolean serverDeleter;
    private static Logger LFDLog;
    public LFD(int serverPortNumber, String GFDAddress, int GFDPortNumber, int heartBeatFrequency) {
        this.serverPortNumber = serverPortNumber;
        this.GFDAddress = GFDAddress;
        this.GFDPortNumber = GFDPortNumber;
        this.heartBeatFrequency = heartBeatFrequency;
        this.serverConnected = false;
        this.serverRegister = false;
        this.serverDeleter = false;
    }

    public void setLfdId(int id) {
        this.lfdId = id;
    }

    public static void main(String[] args) {
        Configuration config = Configuration.getConfig();

        int lfdNum = Integer.parseInt(args[0]);

        int GFDPortNumber = config.getGFDConfig().getServerPort();
        int heartBeatFrequency = config.getLFDConfig().getHeartBeatFrequency();
        int serverPortNumber;
        if (lfdNum == 1 ) {
            serverPortNumber = config.getR1Config().getServerPort();
        } else if (lfdNum == 2) {
            serverPortNumber = config.getR2Config().getServerPort();
        } else if (lfdNum == 3) {
            serverPortNumber = config.getR3Config().getServerPort();
        } else {
//            System.out.println("Impossible");
            LFDLog.info("Impossible");
            return;
        }

        LFD lfd = new LFD(serverPortNumber, "127.0.0.1", GFDPortNumber, heartBeatFrequency);
        Thread serverThread = new ServerThread(lfd);
        Thread gfdThread = new GFDThread(lfd);
        gfdThread.start();
        serverThread.start();
    }

    static class ServerThread extends Thread {
        private static final String HEARTBEAT_MSG = "heartbeat";
        private final LFD lfd;

        public ServerThread(LFD lfd) {
            this.lfd = lfd;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Socket socket = new Socket("127.0.0.1", lfd.serverPortNumber);
//                    System.out.println("Server " + lfd.serverPortNumber + ": Alive");
                    LFDLog.info("Server " + lfd.serverPortNumber + ": Alive");
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
//                    System.out.println("LFD " + lfd.lfdId + ": " + HEARTBEAT_MSG + " to server " + lfd.serverPortNumber);
                    LFDLog.info("LFD " + lfd.lfdId + ": " + HEARTBEAT_MSG + " to server " + lfd.serverPortNumber);
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
//                    System.out.println("LFD " + lfd.lfdId + ": " + "lost Connection with server " + lfd.serverPortNumber);
                    LFDLog.info("LFD " + lfd.lfdId + ": " + "lost Connection with server " + lfd.serverPortNumber);
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
        private final LFD lfd;

        public GFDThread(LFD lfd) {
            this.lfd = lfd;
        }

        @Override
        public void run() {
            Socket socket;
            DataOutputStream out;
            DataInputStream in;
            String message;
            int heartBeatNum = 1;
            while (true) {
                try {
                    socket = new Socket(lfd.GFDAddress, lfd.GFDPortNumber);
//                    System.out.println("Heart beating with GFD");
                    LFDLog.info("Heart beating with GFD");
                    out = new DataOutputStream(socket.getOutputStream());
                    in = new DataInputStream(socket.getInputStream());
                    out.writeUTF("LFD: " + socket.toString() + " connection request "+ lfd.serverPortNumber);
                    message = in.readUTF();
                    int lfdId = Integer.parseInt(message.split(" ")[5]);
                    lfd.setLfdId(lfdId);
                    break;
                } catch (IOException u) {
//                    System.out.println("Can't connect GFD");
                    LFDLog.info("Can't connect GFD");

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
//                    System.out.println("GFD: " + message);
                    LFDLog.info("GFD: " + message);
                    Thread.sleep(lfd.heartBeatFrequency);
                } catch (Exception e) {
//                    System.out.println("GFD is Closed");
                    LFDLog.info("GFD is Closed");
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

