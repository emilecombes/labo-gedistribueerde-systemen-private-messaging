package common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.PublicKey;

public interface ServerIF extends Remote {

  int getUserId() throws RemoteException;

  PublicKey getPublicKey() throws RemoteException;

  byte[] getMessage(byte[] idx, byte[] encryptedRequest) throws RemoteException;

  void writeToBB(byte[] idx, byte[] value, byte[] tag) throws RemoteException;
}
