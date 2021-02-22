package genericnode;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteInterface extends Remote {

    public String put(String key, String val) throws RemoteException;
    public String get(String key) throws RemoteException;
    public String del(String key) throws RemoteException;
    public String store() throws RemoteException;

    public void exit() throws RemoteException;
    public boolean calledExit() throws RemoteException;

}
