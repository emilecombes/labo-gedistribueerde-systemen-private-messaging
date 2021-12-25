package client;

import common.ClientIF;
import common.ServerIF;

import java.net.MalformedURLException;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;

public class Client extends UnicastRemoteObject implements ClientIF {
  ClientUI chatUI;
  private String hostName = "localhost";
  private String serviceName = "chatService";
  private String userName;
  private String clientServiceName;

  public ServerIF serverIF;

  public Client(ClientUI chat, String userName) throws RemoteException {
    super();
    chatUI = chat;
    this.userName = userName;
    clientServiceName = "ClientListenService_" + userName;
  }


  public void start() throws RemoteException {
    try {
      Naming.rebind("rmi://" + hostName + "/" + clientServiceName, this);
      serverIF = (ServerIF) Naming.lookup("rmi://" + hostName + "/" + serviceName);
    } catch (ConnectException e) {
      System.err.println("Connection problem: " + e);
      e.printStackTrace();
    } catch (NotBoundException | MalformedURLException e) {
      e.printStackTrace();
    }

    registerWithServer(userName, hostName, clientServiceName);
    System.out.println("Client is listening...");
  }

  public void registerWithServer(String user, String host, String service) {
    try {
      serverIF.registerListener(user, host, service);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void messageFromServer(String message) throws RemoteException {
    System.out.println(message);
    chatUI.textArea.append(message);
    chatUI.textArea.setCaretPosition(chatUI.textArea.getDocument().getLength());
  }

  @Override
  public void updateUserList(String[] currentUsers) throws RemoteException {
    if (currentUsers.length < 2) {
      chatUI.privateMsgButton.setEnabled(false);
    }
    chatUI.userPanel.remove(chatUI.clientPanel);
    chatUI.setClientPanel(currentUsers);
    chatUI.clientPanel.repaint();
    chatUI.clientPanel.revalidate();
  }

}
