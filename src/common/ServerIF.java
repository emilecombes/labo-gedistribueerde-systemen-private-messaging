package common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.PublicKey;

public interface ServerIF extends Remote {

  int getUserId() throws RemoteException;

  PublicKey getPublicKey() throws RemoteException;

  byte[] getMessage(int idx, byte[] encryptedRequest) throws RemoteException;

  void writeToBB(int idx, byte[] value, byte[] tag) throws RemoteException;
}
