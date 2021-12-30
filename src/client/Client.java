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

  //Group:
  private int groupIdx;
  private int groupTag;
  private Cipher groupCipher;
  //Sleutels voor groepschat
  private SecretKey groupSendSecretKey;
  private SecretKey groupReceiveSecretKey;

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

  public void start() throws RemoteException {
    try {
      Naming.rebind("rmi://" + hostName + "/" + clientServiceName, this);
      serverIF = (ServerIF) Naming.lookup("rmi://" + hostName + "/" + serviceName);

      // Initialise symmetric key for the group conversation
      KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
      groupSendSecretKey = keyGenerator.generateKey();
      groupCipher = Cipher.getInstance("AES");

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

  public static void main(String[] args) throws NoSuchAlgorithmException, RemoteException {
    Client c = new Client(new ClientUI(), "Bob");
    c.doe();
  }

  public void doe() throws NoSuchAlgorithmException {
    int idx = random.nextInt(serverSize);
    byte[] tag = new byte[tagSize];
    random.nextBytes(tag);
    KeyGenerator kg = KeyGenerator.getInstance("AES");
    SecretKey sk = kg.generateKey();
    CommunicationDetails commDet = new CommunicationDetails(idx, tag, sk, "Bob");
    sendMap.put(1, commDet);
    generateNewSendKey(1);
  }

  private SecretKey generateNewSendKey(int id){
    try {
      SecretKeyFactory kf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
      byte[] salt = "salt".getBytes();
      KeySpec spec = new PBEKeySpec(Base64.getEncoder().encodeToString(sendMap.get(id).getKey().getEncoded()).toCharArray(), salt, 1000, 16);
      SecretKey s = new SecretKeySpec(kf.generateSecret(spec).getEncoded(), "AES");
      return s;
    } catch(Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  private SecretKey generateNewReceiveKey(int id){
    try {
      SecretKeyFactory kf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
      byte[] salt = "salt".getBytes();
      KeySpec spec = new PBEKeySpec(Base64.getEncoder().encodeToString(receiveMap.get(id).getKey().getEncoded()).toCharArray(), salt, 1000, 16);
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
        SecretKey newKey = generateNewSendKey(recipient);
//        SecretKey newKey = key;
        assert newKey != null;
        sendMap.get(recipient).setIdx(nextIdx).setTag(nextTag).setKey(newKey);
        System.out.println("idx: "+sendMap.get(recipient).getIdx()+" tag: "+new String(sendMap.get(recipient).getTag())+" key: "+sendMap.get(recipient).getKey().toString());
        System.out.println();
        //Weergeven
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
        SecretKey newKey = generateNewReceiveKey(sender);
//        SecretKey newKey = receiveMap.get(sender).getKey();
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

  public String bumpUser(int id) {
    try {
      int idx = random.nextInt(serverSize);
      byte[] tag = new byte[tagSize];
      random.nextBytes(tag);
      KeyGenerator kg = KeyGenerator.getInstance("AES");
      SecretKey sk = kg.generateKey();
      String username = "nog invullen";
      sendMap.put(id, new CommunicationDetails(idx, tag, sk, username));
      return sendMap.get(id).getBumpRequest();
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
    return "Bump failed.";
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

      FileOutputStream fileOut =
              new FileOutputStream("/commDet.ser");
      ObjectOutputStream out = new ObjectOutputStream(fileOut);
      out.writeObject(commDet.setUsername(userName));
      out.close();
      fileOut.close();
    } catch (NoSuchAlgorithmException | IOException e) {
      e.printStackTrace();
    }
  }

  public String receiveBumpJson(int id){
    CommunicationDetails commDet = null;
    try {
      FileInputStream fileIn = new FileInputStream("/commDet.ser");
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

//  //Versturen van bump: genereren van values, opslaan in eigen sendMap en doorgeven naar acceptBump
//  public void sendBump(Client acceptor) {
//    int idx = random.nextInt(serverSize);
//    byte[] tag = new byte[tagSize];
//    random.nextBytes(tag);
//    SecretKey secretKey = null;
//    try {
//      KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
//      secretKey = keyGenerator.generateKey();
//    } catch (Exception e) {
//      e.printStackTrace();
//    }
//    if (secretKey != null) {
//      CommunicationDetails commDet = new CommunicationDetails(idx, tag, secretKey);
//      sendMap.put(acceptor.id, commDet);
//      acceptor.acceptBump(commDet, this);
//    }
//  }

  //Accepteren van bump: values opslaan in receiveMap
  public void acceptBump(CommunicationDetails commDet, Client sender) {
    receiveMap.put(sender.id, commDet);
  }

  private byte[] encryptToServer(byte[] toEncrypt) {
    try {
      return serverCipher.doFinal(toEncrypt);
    } catch (Exception e) {
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

  public int getId() {
    return id;
  }

}
