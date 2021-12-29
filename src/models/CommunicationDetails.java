package models;

import javax.crypto.SecretKey;

public class CommunicationDetails {
    int idx;
    int tag;
    SecretKey secretKey;

    public CommunicationDetails(int idx, int tag, SecretKey secretKey) {
        this.idx = idx;
        this.tag = tag;
        this.secretKey = secretKey;
    }

    public SecretKey getKey(){
        return secretKey;
    }
}
