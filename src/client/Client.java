package client;

import common.ServerIF;
import models.CommunicationDetails;

import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.ByteBuffer;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Client extends UnicastRemoteObject implements Remote {
  private final String userName;
  private int userId;
  private final String clientServiceName;
  ClientUI chatUI;

  private final int serverSize = 10;
  private final int tagSize = 100;
  private final Random random = new Random();
  Cipher serverCipher;

  // Communication Details
  private final Map<Integer, CommunicationDetails> sendMap = new HashMap<>();
  private final Map<Integer, CommunicationDetails> receiveMap = new HashMap<>();
  private final Map<String, Integer> userMap = new HashMap<>();

  public ServerIF serverIF;

  public Client(ClientUI chat, String name) throws RemoteException {
    super();
    chatUI = chat;
    userName = name;
    clientServiceName = "ClientListenService_" + userName;
  }

  public int getId() {
    return userId;
  }

  public int getUserId(String recipient){
    return userMap.get(recipient);
  }

  public void start() throws RemoteException {
    try {
      String hostName = "localhost";
      Naming.rebind("rmi://" + hostName + "/" + clientServiceName, this);
      String serviceName = "chatService";
      serverIF = (ServerIF) Naming.lookup("rmi://" + hostName + "/" + serviceName);

      // Get Cipher to encrypt traffic to server
      serverCipher = Cipher.getInstance("RSA");
      serverCipher.init(Cipher.ENCRYPT_MODE, serverIF.getPublicKey());

      userId = serverIF.getUserId();

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
      byte[] salt = "salt".getBytes();
      KeySpec spec = new PBEKeySpec(Base64.getEncoder().encodeToString(sendMap.get(id).getKey().getEncoded()).toCharArray());
      return new SecretKeySpec(kf.generateSecret(spec).getEncoded(), "AES");
    } catch(Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  private SecretKey generateNewReceiveKey(int id){
    try {
      SecretKeyFactory kf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
      byte[] salt = "salt".getBytes();
      KeySpec spec = new PBEKeySpec(Base64.getEncoder().encodeToString(receiveMap.get(id).getKey().getEncoded()).toCharArray());
      return new SecretKeySpec(kf.generateSecret(spec).getEncoded(), "AES");
    } catch(Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public void sendPM(int recipient, String message) throws RemoteException {
    System.out.println("--------------sendPM--------------");
    System.out.println("recipient: " + recipient + ", message: " + message);

    if(sendMap.containsKey(recipient)) {
      String prefix = "[PM from " + userName + "]: ";
      int nextIdx = random.nextInt(serverSize);
      byte[] nextTag = new byte[tagSize];
      random.nextBytes(nextTag);
      String value = prefix + message + '|' + nextIdx + '|' + new String(nextTag);

      System.out.println("value: " + value);
      System.out.println();

      try {
        // E2EE
        Cipher cipher = Cipher.getInstance("AES");
        SecretKey key = sendMap.get(recipient).getKey();
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptedValue = cipher.doFinal(value.getBytes());

        System.out.println("encryptedValue: " + new String(encryptedValue));
        System.out.println();

        //Assymetrische encryptie naar server
        //TODO
//        byte[] encrypted = encryptToServer(encryptedValue);
        byte[] encrypted = encryptedValue;

        MessageDigest hash = MessageDigest.getInstance("SHA-512");
        hash.update(sendMap.get(recipient).getTag());
        byte[] hashTag = hash.digest();
//        byte[] encryptedTag = encryptToServer(hashTag);
        byte[] encryptedTag = hashTag;

        System.out.println("encryptedTag: " + new String(encryptedTag));
        System.out.println();

        System.out.println("idx: "+sendMap.get(recipient).getIdx());
        System.out.println();
        System.out.println(" u: "+new String(encrypted));
        System.out.println();
        System.out.println(" encryptedTag: "+new String(encryptedTag));
        System.out.println();
        // Write to bulletin board
        serverIF.writeToBB(sendMap.get(recipient).getIdx(),
            encrypted,
            encryptedTag
        );

        //Update eigen send waarden
//        SecretKey newKey = generateNewSendKey(recipient);
        SecretKey newKey = key;
        assert newKey != null;
        sendMap.get(recipient).setIdx(nextIdx).setTag(nextTag).setKey(newKey);
        System.out.println("idx: "+sendMap.get(recipient).getIdx()+" tag: "+new String(sendMap.get(recipient).getTag())+" key: "+sendMap.get(recipient).getKey().toString());
        System.out.println();
        //Weergeven
        String ownPrefix = "[Sent by you]: ";
        System.out.println(ownPrefix + message);
        chatUI.textArea.append(ownPrefix + message);
        chatUI.textArea.setCaretPosition(chatUI.textArea.getDocument().getLength());

      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    System.out.println("--------------EXIT sendPM--------------");
  }

  public void getPM(int sender) throws RemoteException {
    System.out.println("--------------getPM--------------");
    System.out.println("sender: " + sender);

    if (receiveMap.containsKey(sender)) {
      // Create request message
//      byte[] separator = "|".getBytes();
//      byte[] buffer =
//          new byte[Integer.BYTES + separator.length + receiveMap.get(sender).getTag().length];
//      ByteBuffer buff = ByteBuffer.wrap(buffer);
////      buff.putInt(receiveMap.get(sender).getIdx());
////      buff.put(separator);
//      buff.put(receiveMap.get(sender).getTag());
//      byte[] request = buff.array();

//      System.out.println(new String(request));


      // Assymetric encryption to server
      //TODO
//      byte[] encryptedRequest = encryptToServer(request);
//      byte[] encryptedRequest = request;

      // Retrieve message from server
      byte[] encryptedMessage = serverIF.getMessage(receiveMap.get(sender).getIdx(), receiveMap.get(sender).getTag());
      System.out.println("EncryptedMessage from server: " + new String(encryptedMessage));
      byte[] decryptedMessage = new byte[0];
      try {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, receiveMap.get(sender).getKey());
        decryptedMessage = cipher.doFinal(encryptedMessage);
        System.out.println("decryptedMessage: " + new String(decryptedMessage));
      } catch (Exception e) {
        e.printStackTrace();
      }
      if (decryptedMessage.length > 0) {
        String[] message = new String(decryptedMessage).split("\\|");
        System.out.println("message[0]: " + message[0] + "message[1]: " + message[1] + "message[2]: " + message[2]);
        int newIdx = Integer.parseInt(message[0]);
        byte[] newTag = message[2].getBytes();
//        SecretKey newKey = generateNewReceiveKey(sender);
        SecretKey newKey = receiveMap.get(sender).getKey();
        assert newKey != null;
        receiveMap.get(sender).setIdx(newIdx).setTag(newTag).setKey(newKey);

        System.out.println(message[1]);
        chatUI.textArea.append(message[1]);
        chatUI.textArea.setCaretPosition(chatUI.textArea.getDocument().getLength());
      }

    } else {
      System.out.println("No key was found for this user. Make sure bumping was successful");
    }
    System.out.println("--------------EXIT getPM--------------");
  }

  public void bumpJson(int id, String username) {
    try {
      int idx = random.nextInt(serverSize);
      byte[] tag = new byte[tagSize];
      random.nextBytes(tag);
      KeyGenerator kg = KeyGenerator.getInstance("AES");
      SecretKey sk = kg.generateKey();
      CommunicationDetails commDet = new CommunicationDetails(idx, tag, sk, username);
      sendMap.put(id, commDet);
      userMap.put(username, id);

      FileOutputStream fileOut =
              new FileOutputStream("/tmp/commDet.ser");
      ObjectOutputStream out = new ObjectOutputStream(fileOut);
      out.writeObject(commDet.setUsername(userName));
      out.close();
      fileOut.close();
    } catch (NoSuchAlgorithmException | IOException e) {
      e.printStackTrace();
    }
  }

  public String receiveBumpJson(int id){
    CommunicationDetails commDet;
    try {
      FileInputStream fileIn = new FileInputStream("/tmp/commDet.ser");
      ObjectInputStream in = new ObjectInputStream(fileIn);
      commDet = (CommunicationDetails) in.readObject();
      in.close();
      fileIn.close();
    } catch (IOException | ClassNotFoundException i) {
      i.printStackTrace();
      return "";
    }
    receiveMap.put(id, commDet);
    return commDet.getUsername();
  }

  private byte[] encryptToServer(byte[] toEncrypt) {
    try {
      return serverCipher.doFinal(toEncrypt);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }


}
