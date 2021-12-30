package server;

import common.ServerIF;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import java.security.*;
import java.util.*;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

import java.rmi.RemoteException;


public class Server extends UnicastRemoteObject implements ServerIF {
  private ArrayList<LinkedHashMap<byte[], byte[]>> bulletinBoard;
  private int bulletinBoardSize = 10;
  private KeyPair keyPair;
  private Cipher serverCipher;
  private int clientNumber;

  // Constructor
  public Server() throws RemoteException {
    super();
    clientNumber = 0;

    bulletinBoard = new ArrayList<>(bulletinBoardSize);
    for(int i = 0; i < bulletinBoardSize; i++){
      bulletinBoard.add(new LinkedHashMap<>());
    }

    // Init server keypair
    try{
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
      keyPairGenerator.initialize(2048);
      keyPair = keyPairGenerator.generateKeyPair();
      PrivateKey priv = keyPair.getPrivate();

      serverCipher = Cipher.getInstance("RSA");
      serverCipher.init(Cipher.DECRYPT_MODE, priv);
    } catch(Exception e){
      e.printStackTrace();
    }
  }

  // Local Methods
  public static void main(String[] args){
    startRMIRegistry();
    String hostName = "localhost";
    String serviceName = "chatService";

    try{
      ServerIF server = new Server();
      Naming.rebind("rmi://" + hostName + "/" + serviceName, server);
      System.out.println("Chat Server is running...");
    } catch (Exception e) {
      System.err.println("Server exception: " + e);
      e.printStackTrace();
    }
  }

  public static void startRMIRegistry(){
    try{
      LocateRegistry.createRegistry(1099);
      System.out.println("RMI Server is ready");
    }catch (RemoteException e){
      e.printStackTrace();
    }
  }


  // Remote Methods
  @Override
  public int getUserId(){
    return clientNumber++;
  }

  @Override
  public byte[] getMessage(byte[] encryptedRequest) throws RemoteException {
    //Decrypt message from client
    byte[] decrypted = decrypt(encryptedRequest);
//    byte[] decrypted = encryptedRequest;
    assert decrypted != null;
    String[] request = new String(decrypted).split("\\|");
    System.out.println("(1): " + request[0] + "(2): " + request[1]);
    int idx = Integer.parseInt(request[0]);
    String tag = request[1];
    byte[] hashedTag = null;
    try {
      MessageDigest hash = MessageDigest.getInstance("SHA-512");
      hash.update(tag.getBytes());
      hashedTag = hash.digest();
      String[] message = new String(bulletinBoard.get(idx).get(hashedTag)).split("\\|");
      //TODO: return is niet gencrypteerd tussen server en client, wel door symm key
      if(message.length>0){
        bulletinBoard.get(idx).remove(hashedTag);
        return message[1].getBytes();
      }
    } catch(Exception e) {
      e.printStackTrace();
    }
    return new byte[0];
  }

  @Override
  public void writeToBB(int idx, byte[] value, byte[] tag) throws RemoteException {
    // TODO: RSA
    bulletinBoard.get(idx).put(tag, value);
  }

  private byte[] decrypt(byte[] encrypted){
    try {
      return serverCipher.doFinal(encrypted);
    } catch (IllegalBlockSizeException | BadPaddingException e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public PublicKey getPublicKey() throws RemoteException {
    return keyPair.getPublic();
  }
}
