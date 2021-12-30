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
  private Vector<Chatter> chatters;
  private ArrayList<LinkedHashMap<byte[], byte[]>> bulletinBoard;
  private int bulletinBoardSize = 10;
  private KeyPair keyPair;
  private Cipher serverCipher;

  // Constructor
  public Server() throws RemoteException {
    super();
    chatters = new Vector<>(10, 1);
    bulletinBoard = new ArrayList<>(bulletinBoardSize);
    for(int i = 0; i < bulletinBoardSize; i++){
      bulletinBoard.add(new LinkedHashMap<>());
    }
    //Initialiseren keypair server
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
  public byte[] getMessage(int idx, byte[] tag) throws RemoteException {
    //Decrypt message from client
    //TODO
//    byte[] decrypted = decrypt(encryptedRequest);
//    byte[] decrypted = encryptedRequest;
//    assert decrypted != null;

    System.out.println("--------------GetMessage--------------");
    System.out.println("Id aangekomen bij server: " + idx);
    System.out.println("Tag aangekomen bij server: " + new String(tag));

    byte[] hashedTag = null;
    try {
      MessageDigest hash = MessageDigest.getInstance("SHA-512");
      hash.update(tag);
      hashedTag = hash.digest();
      System.out.println("Tag na hashen: " + new String(hashedTag));
      byte[] message = bulletinBoard.get(idx).get(hashedTag);
      if(message != null){
        System.out.println("Opgehaald bericht: " + new String(message));
        bulletinBoard.get(idx).remove(hashedTag);
        System.out.println("--------------EXIT GetMessage--------------");
        return message;
      }
    } catch(Exception e) {
      e.printStackTrace();
    }
    System.out.println("--------------EXIT GetMessage--------------");
    return new byte[0];
  }

  @Override
  public void writeToBB(int idx, byte[] encryptedValue, byte[] encryptedTag) throws RemoteException {
    System.out.println("--------------writeToBB--------------");
    System.out.println("Id aangekomen bij server: " + idx);
    System.out.println("message aangekomen bij server: " + new String(encryptedValue));
    System.out.println("tag aangekomen bij server: " + new String(encryptedTag));
    //TODO
//    byte[] decrypted = decrypt(encryptedValue);
    byte[] decrypted = encryptedValue;
//    byte[] tag = decrypt(encryptedTag);
    byte[] tag = encryptedTag;
//    System.out.println("Decrypted u: " + new String(decrypted) + " tag: " + new String(tag));
    bulletinBoard.get(idx).put(tag, decrypted);
    System.out.println("--------------EXIT writeToBB--------------");
  }

  private byte[] decrypt(byte[] encrypted){
    try {
      return serverCipher.doFinal(encrypted);
    } catch (IllegalBlockSizeException e) {
      e.printStackTrace();
    } catch (BadPaddingException e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public PublicKey getPublicKey() throws RemoteException {
    return keyPair.getPublic();
  }

//  public void updateChat(String name, byte[] post) throws RemoteException {
//    sendToAll(name + ": " + new String(post) + "\n");
//  }

//  public void updateUserList() {
//    String[] currentUsers = getUserList();
//    for(Chatter c : chatters){
//      try {
//        c.getClient().updateUserList(currentUsers);
//      } catch (RemoteException e){
//        e.printStackTrace();
//      }
//    }
//  }

  public String[] getUserList(){
    String[] allUsers = new String[chatters.size()];
    for(int i = 0; i < allUsers.length; i++) {
      allUsers[i] = chatters.elementAt(i).getName();
    }
    return allUsers;
  }

//  public void sendToAll(String message) {
//    for(Chatter c : chatters){
//      try {
//        c.getClient().messageFromServer(message);
//      } catch (RemoteException e){
//        e.printStackTrace();
//      }
//    }
//  }

//  @Override
//  public void registerListener(String user, String host, String service) throws RemoteException{
//    System.out.println(user + " has joined.");
//    System.out.println("hostname: " + host);
//    System.out.println("RMI service: " + service);
//    registerChatter(user, host, service);
//  }

//  private void registerChatter(String user, String host, String service) {
//    try{
//      ClientIF newClient = (ClientIF) Naming.lookup(
//          "rmi://" + host + "/" + service
//      );
//      chatters.addElement(new Chatter(user, newClient));
//      newClient.messageFromServer("[Server]: " + "Welcome to the chat " + user + ".\n");
//      updateUserList();
//    } catch (Exception e) {
//      e.printStackTrace();
//    }
//  }

//  @Override
//  public void leaveChat(String user) throws RemoteException {
//    Chatter leaver = getChatter(user);
//    System.out.println(leaver.getName() + " left the chat.");
//    chatters.remove(getChatter(user));
//    updateUserList();
//  }

  public Chatter getChatter(String user) {
    for (Chatter c : chatters)
      if (c.getName().equals(user))
        return c;
    return null;
  }

//  @Override
//  public void sendPM(int[] group, String message) throws RemoteException {
//    Chatter privateConversation;
//    for(int i : group){
//      privateConversation = chatters.elementAt(i);
//      privateConversation.getClient().messageFromServer(message);
//    }
//  }


}
