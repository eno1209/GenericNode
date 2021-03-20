package genericnode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.AbstractMap.SimpleEntry;
import java.util.*;

public class GenericNode {

    protected static final Map<String, String> keyValueStore = new HashMap<>();
    protected static final List<String> lockedKeys = Collections.synchronizedList(new ArrayList<String>());
    protected static final long configReadInterval = 300L; // seconds
    protected static final String configFilePath = "/tmp/nodes.cfg";
    protected static final int maxRetries = 10;
    protected static final int retrySleepTime = 30; // seconds
    protected static boolean KEEP_TCP_SERVER_RUNNING = true;
    protected static List<DistributedServerNode> serverNodes = new ArrayList<>();
    protected static boolean isServer = false;
    protected static InetAddress serverIP = null;
    protected static int serverPort = -1;
    protected static long lastConfigReadStamp = -1L;

    public static void main(String[] args) {

        if (args.length > 0) {

            // ======================== TCP CLIENT ========================
            if (args[0].equals("tc")) {

                System.out.println("TCP CLIENT");
                String addr = args[1];
                int port = Integer.parseInt(args[2]);
                String cmd = args[3];
                String key = (args.length > 4) ? args[4] : "";
                String val = (args.length > 5) ? args[5] : "";
                SimpleEntry<String, String> se = new SimpleEntry<>(key, val);

                // insert code to make TCP client request to server at addr:port
                try (Socket tcpSocket = new Socket(addr, port)) {
                    DataOutputStream request = new DataOutputStream(tcpSocket.getOutputStream());
                    request.writeUTF(cmd + "," + se.toString());
                    DataInputStream fromServer = new DataInputStream(tcpSocket.getInputStream());
                    String response = fromServer.readUTF();
                    System.out.println(response);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            // ======================== TCP SERVER ========================
            if (args[0].equals("ts")) {
                System.out.println("TCP SERVER");
                isServer = true;
                serverPort = Integer.parseInt(args[1]);
                ServerSocket tcpServerSocket = null;
                try {
                    serverIP = InetAddress.getLocalHost();
                    tcpServerSocket = new ServerSocket(serverPort);
                    tcpServerSocket.setReuseAddress(true);
                    while (KEEP_TCP_SERVER_RUNNING) {
                        // If server is being initialized
                        if (lastConfigReadStamp == -1) {
                            readConfig(configFilePath);
                            lastConfigReadStamp = System.currentTimeMillis();
                        }
                        Socket clientSocket = tcpServerSocket.accept();
                        // If a new client request arrives, check if the config file has been update.
                        long curTime = System.currentTimeMillis();
                        if ((curTime - lastConfigReadStamp) > 1000 * configReadInterval) {
                            readConfig(configFilePath);
                            lastConfigReadStamp = System.currentTimeMillis();
                        }
                        HandleRequest requestHandler = new HandleRequest(clientSocket);
                        new Thread(requestHandler).start();

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (tcpServerSocket != null) {
                        try {
                            tcpServerSocket.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }


        } else {
            String msg = "GenericNode Usage:\n\n" +
                    "Client:\n" +
                    "uc/tc <address> <port> put <key> <msg>  UDP/TCP CLIENT: Put an object into store\n" +
                    "uc/tc <address> <port> get <key>  UDP/TCP CLIENT: Get an object from store by key\n" +
                    "uc/tc <address> <port> del <key>  UDP/TCP CLIENT: Delete an object from store by key\n" +
                    "uc/tc <address> <port> store  UDP/TCP CLIENT: Display object store\n" +
                    "uc/tc <address> <port> exit  UDP/TCP CLIENT: Shutdown server\n" +
                    "rmic <address> put <key> <msg>  RMI CLIENT: Put an object into store\n" +
                    "rmic <address> get <key>  RMI CLIENT: Get an object from store by key\n" +
                    "rmic <address> del <key>  RMI CLIENT: Delete an object from store by key\n" +
                    "rmic <address> store  RMI CLIENT: Display object store\n" +
                    "rmic <address> exit  RMI CLIENT: Shutdown server\n\n" +
                    "Server:\n" +
                    "us/ts <port>  UDP/TCP SERVER: run udp or tcp server on <port>.\n" +
                    "rmis  run RMI Server.\n";
            System.out.println(msg);
        }
    }


    public static void readConfig(String filepath) throws Exception {
        File config = new File(filepath);
        Scanner fr = new Scanner(config);
        while (fr.hasNextLine()) {
            String serverData = fr.nextLine();
            System.out.println(serverData);
            String[] data = serverData.split(":");
            if (data.length != 2) {
                throw new Exception("Invalid configuration file!");
            }
            DistributedServerNode sNode = new DistributedServerNode(data[0], Integer.parseInt(data[1]));
            if (sNode.ip.equals(serverIP) && sNode.port == serverPort) {
                System.out.println("Updating list of servers from config file");
            }
            serverNodes.add(sNode);
        }
    }
}

class DistributedServerNode {
    public InetAddress ip;
    public int port;

    public DistributedServerNode(String ip, int port) throws UnknownHostException {
        this.ip = InetAddress.getByName(ip);
        this.port = port;
    }
}