/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package genericnode;

import java.io.IOException;
import java.sql.Blob;
import java.util.AbstractMap.SimpleEntry;

import java.io.*;
import java.net.*;
import java.util.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.rmi.RemoteException;


/**
 *
 * @author wlloyd
 */
public class GenericNode
{

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {

        if (args.length > 0)
        {

            // RMI

            // Server side
            if (args[0].equals("rmis"))
            {
                System.out.println("RMI SERVER");
                try
                {
                    // start RMI Server
                    RemoteImplement server = new RemoteImplement();
                    // initiate a new remote object, bind it with a name "Server"
                    RemoteInterface stub = (RemoteInterface) UnicastRemoteObject.exportObject(server, 0);
                    Registry registry = LocateRegistry.getRegistry();
                    registry.bind("Server", stub);
                    System.out.println("Sever ready");

                    // check if the client has called exit
                    while (!stub.calledExit()) {}

                    // after client called exit, unexport the server and exit
                    UnicastRemoteObject.unexportObject(server, true);
                    System.exit(0);
                }
                catch (Exception e)
                {
                    // handle exceptions
                    System.out.println("Error initializing RMI server.");
                    e.printStackTrace();
                }
            }

            // Client side
            if (args[0].equals("rmic"))
            {
                try {
                    // preparation
                    System.out.println("RMI CLIENT");
                    String addr = args[1];
                    String cmd = args[2];
                    String key = (args.length > 3) ? args[3] : "";
                    String val = (args.length > 4) ? args[4] : "";

                    // make RMI client request
                    Registry registry = LocateRegistry.getRegistry(addr);
                    RemoteInterface stub = (RemoteInterface) registry.lookup("Server");

                    // operations with the hashmap
                    if (!cmd.equals("exit")) {
                        String response = "";
                        if (cmd.equals("put")) {
                            response = stub.put(key, val);
                        }
                        if (cmd.equals("get")) {
                            response = stub.get(key);
                        }
                        if (cmd.equals("del")) {
                            response = stub.del(key);
                        }
                        if (cmd.equals("store")) {
                            response = stub.store();
                        }
                        // print out the result
                        System.out.println(response);
                    }
                    else {
                        System.out.println("Closing client...");
                        stub.exit();
                        System.exit(0);
                    }
                }
                catch (Exception e) {
                    // handle exceptions
                    System.out.println("Client exception: " + e.toString());
                    e.printStackTrace();
                }
            }


            // TCP

            // Client Side
            if (args[0].equals("tc"))
            {
                // preparation
                System.out.println("TCP CLIENT");
                String addr = args[1];
                int port = Integer.parseInt(args[2]);
                String cmd = args[3];
                String key = (args.length > 4) ? args[4] : "";
                String val = (args.length > 5) ? args[5] : "";

                // make TCP client request to server at addr:port
                Socket clientSocket = new Socket(addr, port);
                // initiate outputStream and inputStream
                DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
                BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                // send operation to server
                outToServer.writeBytes(cmd + '\n' + key + '\n' + val + '\n');
                // get result print out
                if (!cmd.equals("exit")) {
                    String msg = inFromServer.readLine();
                    System.out.println(msg);
                }

                clientSocket.close();
            }

            // Server side
            if (args[0].equals("ts"))
            {
                // preparation
                System.out.println("TCP SERVER");
                int port = Integer.parseInt(args[1]);

                // start TCP server on port
                ServerSocket serverSocket = new ServerSocket(port);
                System.out.println("Server is listening on port " + port);
                // create a new HashMap
                HashMap<String, String> hm = new HashMap<>();

                // keep accepting new clients
                while (true) {
                    // accept a new client
                    Socket socket = serverSocket.accept();
                    System.out.println("New client connected");
                    // read operations
                    BufferedReader inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String cmd = inFromClient.readLine();
                    String key = inFromClient.readLine();
                    String val = inFromClient.readLine();
                    // operations with HashMap
                    DataOutputStream outToClient = new DataOutputStream(socket.getOutputStream());
                    if (cmd.equals("put")) {
                        hm.put(key, val);
                        outToClient.writeBytes("server response:" + cmd + " key=" + key + " ");
                    }
                    if (cmd.equals("get")) {
                        val = hm.get(key);
                        outToClient.writeBytes("server response:" + cmd + " key=" + key + " " + cmd + " val=" + val);
                    }
                    if (cmd.equals("del")) {
                        hm.remove(key);
                        outToClient.writeBytes("server response:" + cmd + " key=" + key + " ");
                    }
                    if (cmd.equals("exit")) {
                        break;
                    }
                    socket.close();
                }
            }


            // UDP
            if (args[0].equals("uc"))
            {
                System.out.println("UDP CLIENT");
                String addr = args[1];
                int sendport = Integer.parseInt(args[2]);
                int recvport = sendport + 1;
                String cmd = args[3];
                String key = (args.length > 4) ? args[4] : "";
                String val = (args.length > 5) ? args[5] : "";
                SimpleEntry<String, String> se = new SimpleEntry<String, String>(key, val);
                // insert code to make UDP client request to server at addr:send/recvport
            }
            if (args[0].equals("us"))
            {
                System.out.println("UDP SERVER");
                int port = Integer.parseInt(args[1]);
                // insert code to start UDP server on port
            }

        }


        else
        {
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
    
    
}
