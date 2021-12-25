package server;

import common.ClientIF;

public class Chatter {
  private String name;
  private ClientIF clientIF;

  public Chatter(String n, ClientIF c) {
    name = n;
    clientIF = c;
  }

  public String getName(){
    return name;
  }

  public ClientIF getClient(){
    return clientIF;
  }
}
