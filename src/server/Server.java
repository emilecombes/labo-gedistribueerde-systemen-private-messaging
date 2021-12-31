package server;

import common.ServerIF;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import java.nio.ByteBuffer;
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
  public byte[] getMessage(byte[] encryptedIdx, byte[] encryptedTag) throws RemoteException {
    //Decrypt message from client
    byte[] decryptedIdx = decrypt(encryptedIdx);
    assert decryptedIdx != null;
    ByteBuffer buff = ByteBuffer.wrap(decryptedIdx);
    int idx = buff.getInt();
    byte[] decryptedTag = decrypt(encryptedTag);
//    byte[] decryptedTag = encryptedTag;
    assert decryptedTag != null;

    ByteBuffer buffer = ByteBuffer.wrap(decryptedTag);
    int tag = buffer.getInt();

    printServer();
    System.out.println("--------------GetMessage--------------");
    System.out.println("Id aangekomen bij server: " + idx);
//    System.out.println("Tag aangekomen bij server: " + new String(decryptedTag));
    System.out.println("Tag aangekomen bij server: " + tag);

    byte[] hashedTag = null;
    try {
      MessageDigest hash = MessageDigest.getInstance("SHA-512");
      hash.update(decryptedTag);
      hashedTag = hash.digest();
      System.out.println("Tag na hashen: " + new String(hashedTag));
      System.out.println();
      byte[] message = null;
      for (Map.Entry<byte[], byte[]> e: bulletinBoard.get(idx).entrySet()){
        if(Arrays.equals(e.getKey(), hashedTag)) message = e.getValue();
      }
//      byte[] message = bulletinBoard.get(idx).get(hashedTag);
      if(message != null){
        System.out.println("Opgehaald bericht: " + new String(message));
        System.out.println();
        bulletinBoard.get(idx).remove(hashedTag);
        System.out.println("--------------EXIT GetMessage--------------");
        printServer();
        return message;
      }
    } catch(Exception e) {
      e.printStackTrace();
    }
    System.out.println("--------------EXIT GetMessage--------------");
    printServer();
    return new byte[0];
  }

  @Override
  public void writeToBB(byte[] encryptedIdx, byte[] encryptedValue, byte[] encryptedTag) throws RemoteException {
    printServer();
    System.out.println("--------------writeToBB--------------");

    //Decrypt from client
    byte[] decryptedIdx = decrypt(encryptedIdx);
    assert decryptedIdx != null;
    ByteBuffer buff = ByteBuffer.wrap(decryptedIdx);
    int idx = buff.getInt();
    byte[] decrypted = decrypt(encryptedValue);
    //TODO
//    byte[] decrypted = encryptedValue;
//    byte[] tag = decrypt(encryptedTag);
    byte[] tag = encryptedTag;
    assert tag != null;
    assert decrypted != null;

    System.out.println("Id aangekomen bij server: " + idx);
    System.out.println("message aangekomen bij server: " + new String(decrypted));
    System.out.println("tag aangekomen bij server: " + new String(tag));


//    System.out.println("Decrypted u: " + new String(decrypted) + " tag: " + new String(tag));
    bulletinBoard.get(idx).put(tag, decrypted);
    System.out.println("--------------EXIT writeToBB--------------");
    printServer();
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

  public void printServer(){
    System.out.println("-------------SERVERINHOUD------------------");
    for(int i = 0; i < bulletinBoard.size(); i++){
      System.out.println("_____Idx: " + i + "_____");
      LinkedHashMap<byte[], byte[]> map = bulletinBoard.get(i);
      for(Map.Entry<byte[], byte[]> e : map.entrySet()){
        System.out.println("tag: " + new String(e.getKey()));
        System.out.println(e.getKey());
        System.out.println("message: " + new String(e.getValue()));
        System.out.println(e.getValue());
      }
    }
  }
}
