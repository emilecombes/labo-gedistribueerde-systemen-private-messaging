package client;

import server.ServerIF;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.server.UnicastRemoteObject;

import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class Client extends UnicastRemoteObject implements ClientIF {
  private static String hostName = "localhost";
  private static String serviceName = "chatService";
  private static String userName;
  private static String clientServiceName;

  protected static ServerIF serverIF;


  public Client(String userName) throws RemoteException {
    super();
    this.userName = userName;
    clientServiceName = "ClientListenService_" + userName;
  }


  public static void main(String[] args) throws RemoteException{
    try{
      Naming.rebind("rmi://" + hostName + "/" + serviceName, new Client("Emile"));
      serverIF = (ServerIF) Naming.lookup("rmi://" + hostName + "/" + serviceName);
      registerWithServer(userName, hostName, clientServiceName);
      System.out.println("client.Client is listening...");
    } catch (ConnectException e){
      System.err.println("Connection problem: " + e);
      e.printStackTrace();
    } catch (NotBoundException | MalformedURLException e){
      e.printStackTrace();
    }
  }

  public static void registerWithServer(String user, String host, String service){
    try{
      serverIF.registerListener(user, host, service);
    } catch (Exception e){
      e.printStackTrace();
    }
  }

  public void messageFromServer(String message) throws RemoteException {
    System.out.println(message);
  }
}
