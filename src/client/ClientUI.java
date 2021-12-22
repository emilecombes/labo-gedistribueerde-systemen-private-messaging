package client;

import client.Client;

import java.rmi.RemoteException;

public class ClientUI {
  private String message;
  private static Client client;

  public static void main(String[] args){
    System.out.println("Enter your username to join the chat:");
    String name = System.in.toString();
    try{
      client = new Client(name);
    } catch(RemoteException e){
      e.printStackTrace();
    }
  }

}
