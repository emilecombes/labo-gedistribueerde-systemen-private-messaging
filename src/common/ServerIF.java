package common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.PublicKey;

public interface ServerIF extends Remote {

//  void registerListener(String user, String host, String service) throws RemoteException;

//  void leaveChat(String userName) throws RemoteException;

//  void updateChat(String userName, byte[] chatMessage) throws RemoteException;

//  void sendPM(int[] group, String message) throws RemoteException;

  PublicKey getPublicKey() throws RemoteException;

  byte[] getMessage(int idx, byte[] encryptedRequest) throws RemoteException;

  void writeToBB(int idx, byte[] value, byte[] tag) throws RemoteException;
}
