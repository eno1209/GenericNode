package genericnode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.Thread.sleep;

public class HandleRequest implements Runnable {

    private final Socket clientSocket;


    public HandleRequest(Socket socket) {
        this.clientSocket = socket;
    }

    private static String serviceRequest(String line) throws Exception {

        String[] args = line.split(",");
        String cmd = args[0];
        if (cmd.equals("put")) {
            String[] keyValuePair = args[1].split("=");
            String key = keyValuePair[0];
            String val = keyValuePair[1];
            int numOfReply = servicePutRequest(key, val);
            assert Math.abs(numOfReply) == GenericNode.serverNodes.size() : "incorrect numOfReply!";
            if (numOfReply > 0) {
                return "server response:put key=" + key + "\n";
            }
            return "server response:abort put key=" + key + "\n";

        } else if (cmd.equals("get")) {
            String[] keyValuePair = args[1].split("=");
            String key = keyValuePair[0];
            String val = GenericNode.keyValueStore.get(key);
            return "server response:get key=" + key + ", val=" + val + "\n";

        } else if (cmd.equals("del")) {
            String[] keyValuePair = args[1].split("=");
            String key = keyValuePair[0];
            int numOfReply = serviceDelRequest(key);
            assert Math.abs(numOfReply) == GenericNode.serverNodes.size() : "incorrect numOfReply!";
            if (numOfReply > 0) {
                return "server response:delete key=" + key + "\n";
            }
            return "server response:abort delete key=" + key + "\n";

        } else if (cmd.equals("store")) {
            StringBuilder sb = new StringBuilder();
            sb.append("server response:\n");
            for (Map.Entry<String, String> entry : GenericNode.keyValueStore.entrySet()) {
                sb.append("key:").append(entry.getKey()).append(" value:").append(entry.getValue()).append("\n");
                if (sb.length() > 65000) return "Trimmed " + sb.substring(0, 65000);
            }
            return sb.toString();

        } else if (cmd.equals("dput1")) {
            String key = args[1];
            if (GenericNode.lockedKeys.contains(key)) return "abort";
            GenericNode.lockedKeys.add(key);
            return "ready";

        } else if (cmd.equals("dput2")) {
            String key = args[1];
            String val = args[2];
            if (!GenericNode.lockedKeys.contains(key)) {
                throw new Exception("Expected key locked!");
            }
            GenericNode.keyValueStore.put(key, val);
            GenericNode.lockedKeys.remove(key);
            return "server response:put key=" + key + "\n";

        } else if (cmd.equals("dputabort")) {
            String key = args[1];
            String val = args[2];
            GenericNode.lockedKeys.remove(key);
            return "aborted";

        } else if (cmd.equals("ddel1")) {
            String key = args[1];
            if (GenericNode.lockedKeys.contains(key)) return "abort";
            GenericNode.lockedKeys.add(key);
            return "ready";

        } else if (cmd.equals("ddel2")) {
            String key = args[1];
            if (!GenericNode.lockedKeys.contains(key)) {
                throw new Exception("Expected key locked!");
            }
            GenericNode.keyValueStore.remove(key);
            GenericNode.lockedKeys.remove(key);
            return "server response:delete key=" + key + "\n";

        } else if (cmd.equals("ddelabort")) {
            String key = args[1];
            GenericNode.lockedKeys.remove(key);
            return "aborted";

        } else {
            return "Invalid command!";
        }
    }

    private static int servicePutRequest(String key, String val) throws Exception {

        int numberOfTry = 0;
        List<DistributedServerNode> nodesToTry = new ArrayList<>(GenericNode.serverNodes);

        while (numberOfTry < GenericNode.maxRetries) {

            boolean retry = false;
            List<DistributedServerNode> nodesToTryNext = new ArrayList<>(nodesToTry);

            for (DistributedServerNode sNode : nodesToTry) {
                String response = s2sTCPRequest(sNode.ip.getHostAddress(), sNode.port, "dput1," + key + "," + val);
                if (response.equals("abort")) {
                    retry = true;
                } else if (response.equals("ready")) {
                    nodesToTryNext.remove(sNode);
                } else {
                    throw new Exception("Unexpected response");
                }
            }
            nodesToTry = new ArrayList<>(nodesToTryNext);

            if (retry) {
                numberOfTry++;
                if (numberOfTry == GenericNode.maxRetries) {
                    return dputabortBroadcast(key, val);
                } else {
                    try {
                        sleep(GenericNode.retrySleepTime * 1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                // Make the dput2 request
                assert nodesToTry.size() == 0 : "Expected all nodes to be ready!";
                return dput2Broadcast(key, val);
            }
        }
        return -1;
    }

    private static String s2sTCPRequest(String addr, int port, String cmd) throws Exception {
        Socket tcpSocket = new Socket(addr, port);
        DataOutputStream request = new DataOutputStream(tcpSocket.getOutputStream());
        request.writeUTF(cmd);
        DataInputStream fromServer = new DataInputStream(tcpSocket.getInputStream());
        return fromServer.readUTF();
    }

    private static int dput2Broadcast(String key, String val) throws Exception {
        String cmd = "dput2," + key + "," + val;
        int cnt = 0;
        for (DistributedServerNode sNode : GenericNode.serverNodes) {
            String response = s2sTCPRequest(sNode.ip.getHostAddress(), sNode.port, cmd);
            if (!response.equals("server response:put key=" + key + "\n")) {
                throw new Exception("Incorrect format dput2 reply");
            }
            cnt++;
        }
        return cnt;
    }

    private static int dputabortBroadcast(String key, String val) throws Exception {
        String cmd = "dputabort," + key + "," + val;
        int cnt = 0;
        for (DistributedServerNode sNode : GenericNode.serverNodes) {
            String response = s2sTCPRequest(sNode.ip.getHostAddress(), sNode.port, cmd);
            if (!response.equals("aborted")) {
                throw new Exception("Incorrect format dput2 reply");
            }
            cnt--;
        }
        return cnt;
    }

    private static int serviceDelRequest(String key) throws Exception {
        int numberOfTry = 0;
        List<DistributedServerNode> nodesToTry = new ArrayList<>(GenericNode.serverNodes);

        while (numberOfTry < GenericNode.maxRetries) {

            boolean retry = false;
            List<DistributedServerNode> nodesToTryNext = new ArrayList<>(nodesToTry);

            for (DistributedServerNode sNode : nodesToTry) {
                String response = s2sTCPRequest(sNode.ip.getHostAddress(), sNode.port, "ddel1," + key);
                if (response.equals("abort")) {
                    retry = true;
                } else if (response.equals("ready")) {
                    nodesToTryNext.remove(sNode);
                } else {
                    throw new Exception("Unexpected response");
                }
            }
            nodesToTry = new ArrayList<>(nodesToTryNext);

            if (retry) {
                numberOfTry++;
                if (numberOfTry == GenericNode.maxRetries) {
                    return ddelabortBroadcast(key);
                } else {
                    try {
                        sleep(GenericNode.retrySleepTime * 1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                // Make the ddel2 request
                assert nodesToTry.size() == 0 : "Expected all nodes to be ready!";
                return ddel2Broadcast(key);
            }
        }
        return -1;

    }

    private static int ddel2Broadcast(String key) throws Exception {
        String cmd = "ddel2," + key;
        int cnt = 0;
        for (DistributedServerNode sNode : GenericNode.serverNodes) {
            String response = s2sTCPRequest(sNode.ip.getHostAddress(), sNode.port, cmd);
            if (!response.equals("server response:delete key=" + key + "\n")) {
                throw new Exception("Incorrect format ddel2 reply");
            }
            cnt++;
        }
        return cnt;
    }

    private static int ddelabortBroadcast(String key) throws Exception {
        String cmd = "ddelabort," + key;
        int cnt = 0;
        for (DistributedServerNode sNode : GenericNode.serverNodes) {
            String response = s2sTCPRequest(sNode.ip.getHostAddress(), sNode.port, cmd);
            if (!response.equals("aborted")) {
                throw new Exception("Incorrect format dput2 reply");
            }
            cnt--;
        }
        return cnt;
    }

    public void run() {
        DataInputStream request = null;
        DataOutputStream toClient = null;
        try {
            request = new DataInputStream(clientSocket.getInputStream());
            toClient = new DataOutputStream(clientSocket.getOutputStream());
            String response = serviceRequest(request.readUTF());
            if (!response.isEmpty()) {
                toClient.writeUTF(response);
            } else {
                GenericNode.KEEP_TCP_SERVER_RUNNING = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (request != null) request.close();
                if (toClient != null) toClient.close();
                clientSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
