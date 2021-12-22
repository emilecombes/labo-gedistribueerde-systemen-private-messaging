package server;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.RemoteRef;

public interface ServerIF extends Remote {

  void registerListener(String user, String host, String service) throws RemoteException;
}
