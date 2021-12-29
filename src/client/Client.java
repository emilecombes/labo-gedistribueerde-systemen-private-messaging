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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

  private String ownPrefix = "[Sent by you]: ";

  private int serverSize = 10;
  private int tagSize = 100;
  private Random random = new Random();
  private int id;
  private static int nummer = 0;

  //PM:
  private Map<Integer, CommunicationDetails> sendMap = new HashMap<>();
  private Map<Integer, CommunicationDetails> receiveMap = new HashMap<>();

  Cipher serverCipher;

  public ServerIF serverIF;

  public Client(ClientUI chat, String userName) throws RemoteException {
    super();
    chatUI = chat;
    this.userName = userName;
    clientServiceName = "ClientListenService_" + userName;
    id = nummer;
    nummer++;
  }

  public void main(String[] args){

  }


  public void start() throws RemoteException {
    try {
      Naming.rebind("rmi://" + hostName + "/" + clientServiceName, this);
      serverIF = (ServerIF) Naming.lookup("rmi://" + hostName + "/" + serviceName);

      // Register the new client
//      serverIF.registerListener(userName, hostName, clientServiceName);

      // Get Cipher to encrypt traffic to server
      serverCipher = Cipher.getInstance("RSA");
      serverCipher.init(Cipher.ENCRYPT_MODE, serverIF.getPublicKey());

    } catch (ConnectException e) {
      System.err.println("Connection problem: " + e);
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }

    System.out.println("Client is listening...");
  }

  private SecretKey generateNewSendKey(int id){
    try {
      SecretKeyFactory kf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
      KeySpec spec = new PBEKeySpec(sendMap.get(id).getKey().toString().toCharArray());
      return new SecretKeySpec(kf.generateSecret(spec).getEncoded(), "AES");
    } catch(Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  private SecretKey generateNewReceiveKey(int id){
    try {
      SecretKeyFactory kf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
      KeySpec spec = new PBEKeySpec(receiveMap.get(id).getKey().toString().toCharArray());
      return new SecretKeySpec(kf.generateSecret(spec).getEncoded(), "AES");
    } catch(Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public void sendPM(int recipient, String message) throws RemoteException {
    if(sendMap.containsKey(recipient)) {
      String prefix = "[PM from " + userName + "]: ";
      int nextIdx = random.nextInt(serverSize);
      byte[] nextTag = new byte[tagSize];
      random.nextBytes(nextTag);
      String value = prefix + message + '|' + nextIdx + '|' + nextTag;

      try {
        // E2EE
        Cipher cipher = Cipher.getInstance("AES");
        SecretKey key = sendMap.get(recipient).getKey();
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptedValue = cipher.doFinal(value.getBytes());

        //Assymetrische encryptie naar server
        byte[] encrypted = encryptToServer(encryptedValue);

        MessageDigest hash = MessageDigest.getInstance("SHA-512");
        hash.update(sendMap.get(recipient).getTag());
        byte[] hashTag = hash.digest();

        // Write to bulletin board
        serverIF.writeToBB(sendMap.get(recipient).getIdx(),
            encrypted,
            hashTag
        );

        //Update eigen send waarden
        SecretKey newKey = generateNewSendKey(recipient);
        assert newKey != null;
        receiveMap.put(recipient, new CommunicationDetails(nextIdx, nextTag, newKey));

        //Weergeven
        System.out.println(ownPrefix + message);
        chatUI.textArea.append(ownPrefix + message);
        chatUI.textArea.setCaretPosition(chatUI.textArea.getDocument().getLength());

      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public void getPM(int sender) throws RemoteException {
    if(receiveMap.containsKey(sender)) {
      // Create request message
      byte[] separator = "|".getBytes();
      byte[] buffer = new byte[Integer.BYTES + separator.length + receiveMap.get(sender).getTag().length];
      ByteBuffer buff = ByteBuffer.wrap(buffer);
      buff.putInt(receiveMap.get(sender).getIdx());
      buff.put(separator);
      buff.put(receiveMap.get(sender).getTag());
      byte[] request = buff.array();

      // Assymetric encryption to server
      byte[] encryptedRequest = encryptToServer(request);

      // Retrieve message from server
      byte[] encryptedMessage = serverIF.getMessage(encryptedRequest);
      byte[] decryptedMessage = new byte[0];
      try {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, receiveMap.get(sender).getKey());
        decryptedMessage = cipher.doFinal(encryptedMessage);
      } catch(Exception e) {
        e.printStackTrace();
      }
      if(decryptedMessage.length > 0){
        String[] message = new String(decryptedMessage).split("\\|");

        int newIdx = Integer.parseInt(message[0]);
        byte[] newTag = message[2].getBytes();
        SecretKey newKey = generateNewReceiveKey(sender);
        assert newKey != null;
        receiveMap.put(sender, new CommunicationDetails(newIdx, newTag, newKey));

        System.out.println(message[1]);
        chatUI.textArea.append(message[1]);
        chatUI.textArea.setCaretPosition(chatUI.textArea.getDocument().getLength());
      }

    } else {
      System.out.println("No key was found for this user. Make sure bumping was successful");
    }
  }

  //Versturen van bump: genereren van values, opslaan in eigen sendMap en doorgeven naar acceptBump
  public void sendBump(Client acceptor){
    int idx = random.nextInt(serverSize);
    byte[] tag = new byte[tagSize];
    random.nextBytes(tag);
    SecretKey secretKey = null;
    try {
      KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
      secretKey = keyGenerator.generateKey();
    } catch(Exception e) {
      e.printStackTrace();
    }
    if(secretKey != null) {
      CommunicationDetails commDet = new CommunicationDetails(idx, tag, secretKey);
      sendMap.put(acceptor.id, commDet);
      acceptor.acceptBump(commDet, this);
    }
  }

  //Accepteren van bump: values opslaan in receiveMap
  public void acceptBump(CommunicationDetails commDet, Client sender){
    receiveMap.put(sender.id, commDet);
  }

  private byte[] encryptToServer(byte[] toEncrypt){
    try {
      return serverCipher.doFinal(toEncrypt);
    } catch(Exception e){
      e.printStackTrace();
    }
    return null;
  }

//  public void sendPM(int[] privateList, String name, String message) throws RemoteException {
//    //TODO: alles
//    String privateMessage = "[PM from " + name + "]: " + message + "\n";
//    serverIF.sendPM(privateList, privateMessage);
//  }

//  @Override
//  public void messageFromServer(String message) throws RemoteException {
//    if(message.length() > 0) {
//      //Todo: Decrypteren en splitsen
//      generateNewReceiveGroupKey();
//      String[] result = message.split("\\|");
//      String decryptedMesage = "Error";
//      try {
//        groupCipher.init(Cipher.DECRYPT_MODE, groupReceiveSecretKey);
//        byte[] decryptedByte = groupCipher.doFinal(result[1].getBytes());
//        String[] decrypted = new String(decryptedByte).split("\\|");
//        groupIdx = Integer.parseInt(decrypted[1]);
//        groupTag = Integer.parseInt(decrypted[2]);
//        decryptedMesage = decrypted[0];
//      } catch(Exception e) {
//        e.printStackTrace();
//      }
//      System.out.println(decryptedMesage);
//      chatUI.textArea.append(message);
//      chatUI.textArea.setCaretPosition(chatUI.textArea.getDocument().getLength());
//    }
//  }

//  @Override
//  public void updateUserList(String[] currentUsers) throws RemoteException {
//    //TODO: sleutel genereren voor elke nieuwe user
//    if (currentUsers.length < 2) {
//      chatUI.privateMsgButton.setEnabled(false);
//    }
//    chatUI.userPanel.remove(chatUI.clientPanel);
//    chatUI.setClientPanel(currentUsers);
//    chatUI.clientPanel.repaint();
//    chatUI.clientPanel.revalidate();
//  }

//  public void sendGroupMessage(String chatMessage, String name) throws RemoteException {
//    //Todo: update bound to reflect server capacity
//    int idxNext = random.nextInt(serverSize);
//    //Todo: tag String?
//    int tagNext = 0;
//    String u = chatMessage + "|" + idxNext + "|" + tagNext;
//    byte[] toEncrypt = u.getBytes();
//
//    try {
//      groupCipher.init(Cipher.ENCRYPT_MODE, groupSendSecretKey);
//      byte[] encrypted = groupCipher.doFinal(toEncrypt);
//      byte[] seperator = "|".getBytes();
//
//      byte[] totaal = new byte[Integer.BYTES*2 + encrypted.length + seperator.length*2];
//      ByteBuffer buff = ByteBuffer.wrap(totaal);
//      buff.putInt(groupIdx);
//      buff.put(seperator);
//      buff.put(encrypted);
//      buff.put(seperator);
//      buff.putInt(groupTag);
//      byte[] combined = buff.array();
//      serverIF.updateChat(name, combined);
//
//    } catch(Exception e) {
//      e.printStackTrace();
//    }
//    generateNewSendGroupKey();
//    groupIdx = idxNext;
//    groupTag = tagNext;
//    //Todo: sleutel veranderen
//  }

}
