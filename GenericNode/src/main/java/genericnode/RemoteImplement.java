package genericnode;

import java.io.*;
import java.util.*;
import java.rmi.*;
import java.rmi.server.*;

public class RemoteImplement implements RemoteInterface {

    boolean isExit = false;
    String response = "";
    HashMap<String, String> hm = new HashMap<>();

    // default constructor
    RemoteImplement() throws RemoteException {
        super();
    }

    // put a value to the hashmap
    public String put(String key, String val) {
        hm.put(key, val);
        response = "server response:put key=" + key;
        return response;
    }

    // returns the value stored at key
    public String get(String key) {
        String val = hm.get(key);
        response = "server response:get key=" + key + " get val=" + val;
        return response;
    }

    // delete the value stored at key
    public String del(String key) {
        hm.remove(key);
        response = "server response:delete key=" + key;
        return response;
    }

    // print all the key value pairs stored in hashmap
    public String store() {
        // collect all the key value pairs in hashmap
        StringBuilder sb = new StringBuilder();
        for (String key : hm.keySet()) {
            sb.append("key:" + key + ":value:" + hm.get(key) + ":");
        }
        response = sb.toString();

        // if there are more than 65000, truncate it
        if (response.length() > 65000) {
            response = "TRIMMED:" + response.substring(0,65000);
        }

        return response;
    }

    // client uses this method to exit the server
    public void exit() {
        isExit = true;
    }

    // tell the server whether it is called exit or not
    public boolean calledExit() {
        return isExit;
    }
}



