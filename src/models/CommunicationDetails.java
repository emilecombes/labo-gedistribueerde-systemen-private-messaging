package models;

import javax.crypto.SecretKey;
import java.util.Arrays;

public class CommunicationDetails {
  int idx;
  byte[] tag;
  SecretKey secretKey;

  public CommunicationDetails(int idx, byte[] tag, SecretKey secretKey) {
    this.idx = idx;
    this.tag = tag;
    this.secretKey = secretKey;
  }

  public int getIdx() {
    return idx;
  }

  public void setIdx(int idx) {
    this.idx = idx;
  }

  public byte[] getTag() {
    return tag;
  }

  public void setTag(byte[] tag) {
    this.tag = tag;
  }

  public void setKey(SecretKey secretKey) {
    this.secretKey = secretKey;
  }

  public SecretKey getKey() {
    return secretKey;
  }

  public String getBumpRequest() {
    return "key: " + Arrays.toString(secretKey.getEncoded()) + "\nindex: " + idx + "\ntag: " +
        Arrays.toString(tag);
  }
}
