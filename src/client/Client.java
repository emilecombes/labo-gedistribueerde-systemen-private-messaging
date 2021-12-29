package client;

import common.ClientIF;
import common.ServerIF;
import models.CommunicationDetails;

import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.KeySpec;
import java.util.HashMap;
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
  private Map<String, CommunicationDetails> sendMap = new HashMap<>();
  private Map<String, CommunicationDetails> receiveMap = new HashMap<>();

  Cipher serverCipher;

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
    //Initialiseren symm key versturen groupchat
    try {
      KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
      groupSendSecretKey = keyGenerator.generateKey();
      groupCipher = Cipher.getInstance("AES");
    } catch(Exception e) {
      e.printStackTrace();
    }

    registerWithServer(userName, hostName, clientServiceName);

    Cipher serverCipher = null;
    try {
      serverCipher = Cipher.getInstance("RSA");
      //TODO: get server public key
//      serverCipher.init(Cipher.ENCRYPT_MODE, serverPublKey);
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    } catch (NoSuchPaddingException e) {
      e.printStackTrace();
    }


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
    generateNewSendGroupKey();
    groupIdx = idxNext;
    groupTag = tagNext;
    //Todo: sleutel veranderen
  }

  private void generateNewSendGroupKey(){
    try {
      SecretKeyFactory kf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
      KeySpec spec = new PBEKeySpec(groupSendSecretKey.toString().toCharArray());
      groupSendSecretKey = new SecretKeySpec(kf.generateSecret(spec).getEncoded(), "AES");
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  private void generateNewReceiveGroupKey(){
    try {
      SecretKeyFactory kf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
      KeySpec spec = new PBEKeySpec(groupReceiveSecretKey.toString().toCharArray());
      groupReceiveSecretKey = new SecretKeySpec(kf.generateSecret(spec).getEncoded(), "AES");
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
      CommunicationDetails commDet = new CommunicationDetails(idx, tag, secretKey);
      sendMap.put(acceptor.userName, commDet);
      acceptor.acceptBump(commDet, this);
    }
  }

  //Accepteren van bump: values opslaan in receiveMap
  public void acceptBump(CommunicationDetails commDet, Client sender){
    receiveMap.put(sender.userName, commDet);
  }

  private byte[] encryptToServer(byte[] toEncrypt){
    try {
      return serverCipher.doFinal(toEncrypt);
    } catch(Exception e){
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public void messageFromServer(String message) throws RemoteException {
    if(message.length() > 0) {
      //Todo: Decrypteren en splitsen
      generateNewReceiveGroupKey();
      String[] result = message.split("\\|");
      String decryptedMesage = "Error";
      try {
        groupCipher.init(Cipher.DECRYPT_MODE, groupReceiveSecretKey);
        byte[] decryptedByte = groupCipher.doFinal(result[1].getBytes());
        String[] decrypted = new String(decryptedByte).split("\\|");
        groupIdx = Integer.parseInt(decrypted[1]);
        groupTag = Integer.parseInt(decrypted[2]);
        decryptedMesage = decrypted[0];
      } catch(Exception e) {
        e.printStackTrace();
      }
      System.out.println(decryptedMesage);
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
