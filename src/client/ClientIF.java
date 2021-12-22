package client;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientIF extends Remote{
  void messageFromServer(String message) throws RemoteException;
}
