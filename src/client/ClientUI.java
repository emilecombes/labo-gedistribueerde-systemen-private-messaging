package client;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serial;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;

public class ClientUI extends JFrame implements ActionListener {
  @Serial
  private static final long serialVersionUID = 1L;
  private String name, message;
  private Client chatClient;

  private final Map<String, Integer> currentUsers = new HashMap<>();

  private final Border blankBorder = BorderFactory.createEmptyBorder(10, 10, 20, 10);//top,r,b,l
  private JList<String> list;
  private JTextField textField;
  protected JTextArea textArea;
  protected JFrame mainFrame;
  protected JButton privateMsgButton, startButton, sendButton,
      requestBumpButton, acceptBumpButton, refreshButton;
  protected JPanel clientPanel, userPanel;
  protected JLabel idLabel;

  public static void main(String[] args) {
    new ClientUI();
  }

  public ClientUI() {
    mainFrame = new JFrame("Client Chat Console");
    mainFrame.addWindowListener(new java.awt.event.WindowAdapter() {
      @Override
      public void windowClosing(java.awt.event.WindowEvent windowEvent) {
        System.exit(0);
      }
    });

    Container c = getContentPane();
    JPanel outerPanel = new JPanel(new BorderLayout());
    outerPanel.add(getInputPanel(), BorderLayout.CENTER);
    outerPanel.add(getTextPanel(), BorderLayout.NORTH);
    c.setLayout(new BorderLayout());
    c.add(outerPanel, BorderLayout.CENTER);
    c.add(getUsersPanel(), BorderLayout.WEST);

    mainFrame.add(c);
    mainFrame.pack();
    mainFrame.setLocation(150, 150);
    textField.requestFocus();

    mainFrame.setDefaultCloseOperation(EXIT_ON_CLOSE);
    mainFrame.setVisible(true);
  }

  public JPanel getTextPanel() {
    String welcome = "Welcome, enter your name and press Start to begin\n";
    textArea = new JTextArea(welcome, 14, 34);
    textArea.setMargin(new Insets(10, 10, 10, 10));
    textArea.setLineWrap(true);
    textArea.setWrapStyleWord(true);
    textArea.setEditable(false);
    JScrollPane scrollPane = new JScrollPane(textArea);
    JPanel textPanel = new JPanel();
    textPanel.add(scrollPane);
    return textPanel;
  }

  public JPanel getInputPanel() {
    JPanel inputPanel = new JPanel(new GridLayout(1, 1, 5, 5));
    inputPanel.setBorder(blankBorder);
    textField = new JTextField();
    inputPanel.add(textField);
    return inputPanel;
  }

  public JPanel getUsersPanel() {
    userPanel = new JPanel(new BorderLayout());
    JLabel userLabel = new JLabel("Current Users", JLabel.CENTER);
    userPanel.add(userLabel, BorderLayout.NORTH);
    idLabel = new JLabel("", JLabel.CENTER);
    userPanel.add(idLabel, BorderLayout.NORTH);

    setClientPanel(new String[]{"No other users"});
    userPanel.add(makeButtonPanel(), BorderLayout.SOUTH);
    userPanel.setBorder(blankBorder);

    return userPanel;
  }

  public void setClientPanel(String[] currClients) {
    clientPanel = new JPanel(new BorderLayout());
    DefaultListModel<String> listModel = new DefaultListModel<>();
    for (String s : currClients) listModel.addElement(s);
    if (currClients.length > 1) privateMsgButton.setEnabled(true);

    list = new JList<>(listModel);
    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    list.setVisibleRowCount(8);
    list.addListSelectionListener(ListSelectionEvent::getFirstIndex);
    JScrollPane listScrollPane = new JScrollPane(list);

    clientPanel.add(listScrollPane, BorderLayout.CENTER);
    userPanel.add(clientPanel, BorderLayout.CENTER);
  }

  public JPanel makeButtonPanel() {
    sendButton = new JButton("Send");
    sendButton.addActionListener(this);
    sendButton.setEnabled(false);

    privateMsgButton = new JButton("Send PM");
    privateMsgButton.addActionListener(this);
    privateMsgButton.setEnabled(false);

    startButton = new JButton("Start");
    startButton.addActionListener(this);

    requestBumpButton = new JButton("Bump");
    requestBumpButton.addActionListener(this);

    acceptBumpButton = new JButton("Accept Bump");
    acceptBumpButton.addActionListener(this);

    refreshButton = new JButton("Refresh");
    refreshButton.addActionListener(this);

    JPanel buttonPanel = new JPanel(new GridLayout(3, 2));
    buttonPanel.add(privateMsgButton);
    buttonPanel.add(startButton);
    buttonPanel.add(sendButton);
    buttonPanel.add(requestBumpButton);
    buttonPanel.add(acceptBumpButton);
    buttonPanel.add(refreshButton);

    return buttonPanel;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    try {
      if (startButton.equals(e.getSource()))
        enterChat();
      else if (privateMsgButton.equals(e.getSource()))
        sendPrivateMessage();
      else if (requestBumpButton.equals(e.getSource()))
        requestBump();
      else if (acceptBumpButton.equals(e.getSource()))
        acceptBump();
      else if (refreshButton.equals(e.getSource()))
        refresh();
    } catch (RemoteException remoteExc) {
      remoteExc.printStackTrace();
    }
  }

  private void enterChat() throws RemoteException {
    name = textField.getText();
    if (name.length() != 0) {
      mainFrame.setTitle(name + "'s console ");
      textField.setText("");
      textArea.append(name + " connecting to chat...\n");
      getConnected(name);
      idLabel.setText("Your id: " + chatClient.getId());
      startButton.setEnabled(false);
      requestBumpButton.setEnabled(true);
      textArea.append("Enter the id,username and press Bump to add a contact\n");
    } else {
      JOptionPane.showMessageDialog(mainFrame, "Enter your name to Start");
    }
  }

  private void sendPrivateMessage() throws RemoteException {
    int receiver = chatClient.getUserId(list.getSelectedValue());
    message = textField.getText();
    textField.setText("");
    sendPrivate(receiver);
  }

  private void requestBump() {
    String[] input = textField.getText().split(",");
    int receiver = Integer.parseInt(input[0]);
    String username = input[1];
    chatClient.bumpJson(receiver, username);
    currentUsers.put(username, receiver);
    textArea.setText(
        "Wait for the other user to accept the bump by typing your id and pressing accept bump \n");
    updateUserList();
    privateMsgButton.setEnabled(true);
  }

  private void acceptBump() {
    int sender = Integer.parseInt(textField.getText());
    String username = chatClient.receiveBumpJson(sender);
    if (!chatClient.canSendTo(sender)) {
      chatClient.bumpJson(sender, username);
      currentUsers.put(username, sender);
      textArea.setText("Bump from " + username + " accepted. " + username + " must now enter your" +
          " id in the text field and press 'accept bump'. \n");
      updateUserList();
      privateMsgButton.setEnabled(true);
    } else {
      textArea.setText("Bumping process with " + username + " is finished. You can now " +
          "communicate.\n");
    }
  }

  private void refresh() throws RemoteException {
    int receiver = chatClient.getUserId(list.getSelectedValue());
    getMessage(receiver);
  }

  private void updateUserList() {
    userPanel.remove(clientPanel);
    setClientPanel(currentUsers.keySet().toArray(new String[0]));
    clientPanel.repaint();
    clientPanel.revalidate();
  }

  private void sendPrivate(int receiver) throws RemoteException {
    chatClient.sendPM(receiver, message);
  }

  private void getMessage(int sender) throws RemoteException {
    chatClient.getPM(sender);
  }

  private void getConnected(String userName) {
    String cleanedUserName = userName.replaceAll("\\W+", "_");

    try {
      chatClient = new Client(this, cleanedUserName);
      chatClient.start();
    } catch (RemoteException e) {
      e.printStackTrace();
    }
  }

}
