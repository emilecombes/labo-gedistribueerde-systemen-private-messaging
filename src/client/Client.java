package client;

import common.ClientIF;
import common.ServerIF;

import javax.crypto.*;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.Random;

public class Client extends UnicastRemoteObject implements ClientIF {
  ClientUI chatUI;
  private String hostName = "localhost";
  private String serviceName = "chatService";
  private String userName;
  private String clientServiceName;

  private int serverSize = 10;
  private int tagSize = 100;
  private Random random = new Random();

  //Group:
  private int groupIdx;
  private int groupTag;
  private Cipher groupCipher;
  //Sleutels voor groepschat
  private SecretKey groupSendSecretKey;
  private SecretKey groupReceiveSecretKey;

  //PM:
  private Map<String, Integer> idxSendMap;
  private Map<String, Integer> tagSendMap;
  private Map<String, Integer> idxReceiveMap;
  private Map<String, Integer> tagReceiveMap;
  //Sleutels voor PM
  private Map<String, SecretKey> pmSendKeys;
  private Map<String, SecretKey> pmReceiveKeys;

  public ServerIF serverIF;

  public Client(ClientUI chat, String userName) throws RemoteException {
    super();
    chatUI = chat;
    this.userName = userName;
    clientServiceName = "ClientListenService_" + userName;
    groupIdx = random.nextInt(serverSize);
    //Todo: moet tag String zijn?
    groupTag = random.nextInt(100);
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
    try {
      KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
      groupSendSecretKey = keyGenerator.generateKey();
      groupCipher = Cipher.getInstance("AES");
    } catch(Exception e) {
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

  public void sendMessage(String chatMessage, String name) throws RemoteException {
    //Todo: update bound to reflect server capacity
    int idxNext = random.nextInt(serverSize);
    //Todo: tag String?
    int tagNext = 0;
    String u = chatMessage + "|" + idxNext + "|" + tagNext;
    byte[] toEncrypt = u.getBytes();

    try {
      //Todo: volgende 2 lijnen moeten bij init


      groupCipher.init(Cipher.ENCRYPT_MODE, groupSendSecretKey);
      byte[] encrypted = groupCipher.doFinal(toEncrypt);
      byte[] seperator = "|".getBytes();

      byte[] totaal = new byte[Integer.BYTES*2 + encrypted.length + seperator.length*2];
      ByteBuffer buff = ByteBuffer.wrap(totaal);
      buff.putInt(groupIdx);
      buff.put(seperator);
      buff.put(encrypted);
      buff.put(seperator);
      buff.putInt(groupTag);
      byte[] combined = buff.array();
      serverIF.updateChat(name, combined);

    } catch(Exception e) {
      e.printStackTrace();
    }
    generateNewGroupKey();
    groupIdx = idxNext;
    groupTag = tagNext;
    //Todo: sleutel veranderen
  }

  private void generateNewGroupKey(){
    try {
      //TODO: KDF die nieuwe AES genereerd op basis van bestaande: HKFD bibliotheek toevoegen?
      SecretKeyFactory kf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
//      KeySpec spec = new
//      groupSendSecretKey = ;
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  public void sendPM(int[] privateList, String name, String message) throws RemoteException {
    //TODO: alles
    String privateMessage = "[PM from " + name + "] :" + message + "\n";
    serverIF.sendPM(privateList, privateMessage);
  }

  //Versturen van bump: genereren van values, opslaan in eigen sendMap en doorgeven naar acceptBump
  public void sendBump(Client acceptor){
    int idx = random.nextInt(serverSize);
    int tag = random.nextInt(tagSize);
    SecretKey secretKey = null;
    try {
      KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
      secretKey = keyGenerator.generateKey();
    } catch(Exception e) {
      e.printStackTrace();
    }
    if(secretKey != null) {
      idxSendMap.put(acceptor.userName, idx);
      tagSendMap.put(acceptor.userName, tag);
      pmSendKeys.put(acceptor.userName, secretKey);
      acceptor.acceptBump(idx, tag, secretKey, this);
    }
  }

  //Accepteren van bump: values opslaan in ontvangMap
  public void acceptBump(int idx, int tag, SecretKey secretKey, Client sender){
    idxReceiveMap.put(sender.userName, idx);
    tagReceiveMap.put(sender.userName, tag);
    pmReceiveKeys.put(sender.userName, secretKey);
  }

  @Override
  public void messageFromServer(String message) throws RemoteException {
    if(message.length() > 0) {
      //Todo: Decrypteren en splitsen

      System.out.println(message);
      chatUI.textArea.append(message);
      chatUI.textArea.setCaretPosition(chatUI.textArea.getDocument().getLength());
    }
  }

  @Override
  public void updateUserList(String[] currentUsers) throws RemoteException {
    //TODO: sleutel genereren voor elke nieuwe user
    if (currentUsers.length < 2) {
      chatUI.privateMsgButton.setEnabled(false);
    }
    chatUI.userPanel.remove(chatUI.clientPanel);
    chatUI.setClientPanel(currentUsers);
    chatUI.clientPanel.repaint();
    chatUI.clientPanel.revalidate();
  }

}
