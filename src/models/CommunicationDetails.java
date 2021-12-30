package models;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Arrays;

public class CommunicationDetails implements Serializable {
  int idx;
  byte[] tag;
  SecretKey secretKey;
  String username;

  public CommunicationDetails(int idx, byte[] tag, SecretKey secretKey, String username) {
    this.idx = idx;
    this.tag = tag;
    this.secretKey = secretKey;
    this.username = username;
  }

  public int getIdx() {
    return idx;
  }

  public CommunicationDetails setIdx(int idx) {
    this.idx = idx;
    return this;
  }

  public byte[] getTag() {
    return tag;
  }

  public CommunicationDetails setTag(byte[] tag) {
    this.tag = tag;
    return this;
  }

  public void setKey(SecretKey secretKey) {
    this.secretKey = secretKey;
  }

  public SecretKey getKey() {
    return secretKey;
  }

  public String getUsername() {
    return username;
  }

  public CommunicationDetails setUsername(String username) {
    this.username = username;
    return this;
  }

  public String getBumpRequest() {
    return "key: " + Arrays.toString(secretKey.getEncoded()) + "\nindex: " + idx + "\ntag: " +
        Arrays.toString(tag);
  }
}
