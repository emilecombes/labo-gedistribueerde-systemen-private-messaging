package server;

import client.ClientIF;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

import java.rmi.RemoteException;


public class Server extends UnicastRemoteObject implements ServerIF {

  // Constructor
  public Server() throws RemoteException {
    super();
  }

  // Local Methods
  public static void main(String[] args){
    startRMIRegistry();
    String hostName = "localhost";
    String serviceName = "chatService";

    try{
      ServerIF server = new Server();
      Naming.rebind("rmi://" + hostName + "/" + serviceName, server);
      System.out.println("Chat server.Server is running...");
    } catch (Exception e) {
      System.err.println("server.Server exception: " + e);
      e.printStackTrace();
    }
  }

  public static void startRMIRegistry(){
    try{
      LocateRegistry.createRegistry(1099);
      System.out.println("RMI server.Server is ready");
    }catch (RemoteException e){
      e.printStackTrace();
    }
  }


  // Remote Methods
  public String sayHello(String clientName) throws RemoteException {
    System.out.println(clientName + " sent a message");
    return "Hello " + clientName + " from chat server.";
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
          "rmi://" + user + "/" + host + "/" + service
      );

      newClient.messageFromServer("[server.Server]: " + "Welcome to the chat " + user + ".\n");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


}
