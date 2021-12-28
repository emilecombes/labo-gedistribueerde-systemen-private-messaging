package server;

import common.ClientIF;
import common.ServerIF;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Vector;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

import java.rmi.RemoteException;


public class Server extends UnicastRemoteObject implements ServerIF {
  private Vector<Chatter> chatters;
  //TODO: Object ipv byte-array?
  private ArrayList<LinkedList<byte[]>> bulletinBoard;
  private int bulletinBoardSize = 10;

  // Constructor
  public Server() throws RemoteException {
    super();
    chatters = new Vector<Chatter>(10, 1);
    bulletinBoard = new ArrayList<>(bulletinBoardSize);
    for(int i = 0; i < bulletinBoardSize; i++){
      bulletinBoard.add(new LinkedList<byte[]>());
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
  public String sayHello(String clientName) throws RemoteException {
    System.out.println(clientName + " sent a message");
    return "Hello " + clientName + " from chat server.";
  }

  public void updateChat(String name, byte[] post) throws RemoteException {
    sendToAll(name + ": " + new String(post) + "\n");
  }

  public void updateUserList() {
    String[] currentUsers = getUserList();
    for(Chatter c : chatters){
      try {
        c.getClient().updateUserList(currentUsers);
      } catch (RemoteException e){
        e.printStackTrace();
      }
    }
  }

  public String[] getUserList(){
    String[] allUsers = new String[chatters.size()];
    for(int i = 0; i < allUsers.length; i++) {
      allUsers[i] = chatters.elementAt(i).getName();
    }
    return allUsers;
  }

  public void sendToAll(String message) {
    for(Chatter c : chatters){
      try {
        c.getClient().messageFromServer(message);
      } catch (RemoteException e){
        e.printStackTrace();
      }
    }
  }

  @Override
  public void registerListener(String user, String host, String service) throws RemoteException{
    System.out.println(user + " has joined.");
    System.out.println("hostname: " + host);
    System.out.println("RMI service: " + service);
    registerChatter(user, host, service);
  }

  private void registerChatter(String user, String host, String service) {
    try{
      ClientIF newClient = (ClientIF) Naming.lookup(
          "rmi://" + host + "/" + service
      );
      chatters.addElement(new Chatter(user, newClient));
      newClient.messageFromServer("[server.Server]: " + "Welcome to the chat " + user + ".\n");
      updateUserList();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void leaveChat(String user) throws RemoteException {
    Chatter leaver = getChatter(user);
    System.out.println(leaver.getName() + " left the chat.");
    chatters.remove(getChatter(user));
    updateUserList();
  }

  public Chatter getChatter(String user){
    for(Chatter c : chatters)
      if(c.getName().equals(user))
        return c;
    return null;
  }

  @Override
  public void sendPM(int[] group, String message) throws RemoteException {
    Chatter privateConversation;
    for(int i : group){
      privateConversation = chatters.elementAt(i);
      privateConversation.getClient().messageFromServer(message);
    }
  }


}
