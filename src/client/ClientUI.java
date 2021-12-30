package client;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class ClientUI extends JFrame implements ActionListener {
  private static final long serialVersionUID = 1L;
  private JPanel textPanel, inputPanel;
  private JTextField textField;
  private String name, message;
  private Border blankBorder = BorderFactory.createEmptyBorder(10, 10, 20, 10);//top,r,b,l
  private Client chatClient;
  private JList<String> list;
  private DefaultListModel<String> listModel;

  private Map<String, Integer> currentUsers = new HashMap<>();

  protected JTextArea textArea, userArea;
  protected JFrame frame;
  protected JButton privateMsgButton, startButton, sendButton, requestBumpButton, acceptBumpButton, refreshButton;
  protected JPanel clientPanel, userPanel;
  protected JLabel idLabel;

  public static void main(String[] args) {
    new ClientUI();
  }


  public ClientUI() {
    frame = new JFrame("Client Chat Console");
    frame.addWindowListener(new java.awt.event.WindowAdapter() {
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

    frame.add(c);
    frame.pack();
    frame.setLocation(150, 150);
    textField.requestFocus();

    frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
    frame.setVisible(true);
  }


  public JPanel getTextPanel() {
    String welcome = "Welcome, enter your name and press Start to begin\n";
    textArea = new JTextArea(welcome, 14, 34);
    textArea.setMargin(new Insets(10, 10, 10, 10));
    textArea.setLineWrap(true);
    textArea.setWrapStyleWord(true);
    textArea.setEditable(false);
    JScrollPane scrollPane = new JScrollPane(textArea);
    textPanel = new JPanel();
    textPanel.add(scrollPane);
    return textPanel;
  }

  public JPanel getInputPanel() {
    inputPanel = new JPanel(new GridLayout(1, 1, 5, 5));
    inputPanel.setBorder(blankBorder);
    textField = new JTextField();
    inputPanel.add(textField);
    return inputPanel;
  }

  public JPanel getUsersPanel() {
    userPanel = new JPanel(new BorderLayout());
    String userStr = "Current Users      ";

    JLabel userLabel = new JLabel(userStr, JLabel.CENTER);
    userPanel.add(userLabel, BorderLayout.NORTH);

    String idStr = "";
    idLabel = new JLabel(idStr, JLabel.CENTER);
    userPanel.add(idLabel, BorderLayout.NORTH);

    String[] noClientsYet = {"No other users"};
    setClientPanel(noClientsYet);

    userPanel.add(makeButtonPanel(), BorderLayout.SOUTH);
    userPanel.setBorder(blankBorder);

    return userPanel;
  }

  public void setClientPanel(String[] currClients) {
    clientPanel = new JPanel(new BorderLayout());
    listModel = new DefaultListModel<>();

    for (String s : currClients) listModel.addElement(s);

    if (currClients.length > 1) {
      privateMsgButton.setEnabled(true);
      requestBumpButton.setEnabled(true);
      acceptBumpButton.setEnabled(true);
    }

    //Create the list and put it in a scroll pane.
    list = new JList<>(listModel);
    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    list.setVisibleRowCount(8);
    list.addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        e.getFirstIndex();
      }
    });
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
//    requestBumpButton.setEnabled(false);

    acceptBumpButton = new JButton("Accept Bump");
    acceptBumpButton.addActionListener(this);
//    acceptBumpButton.setEnabled(false);

    refreshButton = new JButton("Refresh");
    refreshButton.addActionListener(this);

    JPanel buttonPanel = new JPanel(new GridLayout(4, 1));
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
      // Get Connected
      if (e.getSource() == startButton) {
        name = textField.getText();
        if (name.length() != 0) {
          frame.setTitle(name + "'s console ");
          textField.setText("");
          textArea.append(name + " connecting to chat...\n");
          getConnected(name);
          idLabel.setText("Your id: " + chatClient.getId());
          startButton.setEnabled(false);
          requestBumpButton.setEnabled(true);
          textArea.append("Enter the id,username and press Bump to add a contact\n");
        } else {
          JOptionPane.showMessageDialog(frame, "Enter your name to Start");
        }
      }

      // Send Message
      else if (e.getSource() == sendButton) {
        message = textField.getText();
        textField.setText("");
//        sendMessage(message);
        System.out.println("Sending message: " + message);
      }

      // Send PM
      else if (e.getSource() == privateMsgButton) {
        int selection = list.getSelectedIndex();
        message = textField.getText();
        textField.setText("");
        sendPrivate(selection);
      }

      // Bump
      else if (e.getSource() == requestBumpButton) {
//        int receiver = list.getSelectedIndex();
        String[] input = textField.getText().split(",");
        int receiver = Integer.parseInt(input[0]);
        String username = input[1];
        chatClient.bumpJson(receiver, username);
        currentUsers.put(username, receiver);
        textArea.setText("Wait for the other user to accept the bump by typing your id and pressing accept bump \n");
        updateUserList();
        privateMsgButton.setEnabled(true);
//        String bumpRequest = chatClient.bumpUser(receiver);
//        JOptionPane.showMessageDialog(frame, "Show these private communication attributes to your" + " new contact.\n" + bumpRequest);
      }

      // Accept Bump
      else if (e.getSource() == acceptBumpButton) {
        int bumpee = Integer.parseInt(textField.getText());
        String username = chatClient.receiveBumpJson(bumpee);
        chatClient.bumpJson(bumpee, username);
        currentUsers.put(username, bumpee);
        textArea.setText("Bump from " + username + " accepted, wait for him to accept your bump \n");
        updateUserList();
        privateMsgButton.setEnabled(true);
//        String bumpRequest = chatClient.bumpUser(bumpee);
//        JOptionPane.showMessageDialog(frame, "Show these private communication attributes to your" + " new contact.\n" + bumpRequest);
//        showBumpPane();
      }

      else if (e.getSource() == refreshButton) {
        int selection = list.getSelectedIndex();
       getMessage(selection);
      }

    } catch (RemoteException remoteExc) {
      remoteExc.printStackTrace();
    }

  }

  private void updateUserList() {
    userPanel.remove(clientPanel);
    setClientPanel(currentUsers.keySet().toArray(new String[0]));
    clientPanel.repaint();
    clientPanel.revalidate();
  }

  private void showBumpPane(){
//    final JPanel bumpPanel = new JPanel(new GridLayout(3, 1, 5, 5));
//    JPanel keyPanel = new JPanel(new GridLayout(1, 1, 5, 5));
//    JTextField keyField = new JTextField("Key");
//    keyPanel.add(keyField);
//    JTextField tagField = new JTextField("tag");
//    JTextField idxField = new JTextField("index");
//
//    bumpPanel.add(keyPanel);
//    bumpPanel.add(tagField);
//    bumpPanel.add(idxField);
//
//    bumpFrame.add(bumpPanel);
//    bumpFrame.setSize(300, 300);
//    bumpFrame.setLayout(null);
//    bumpFrame.setVisible(true);




    final JFrame bumpFrame = new JFrame("Accept Bump");
    Container c = getContentPane();
    JPanel bumpPanel = new JPanel(new BorderLayout());

    JPanel keyPanel = new JPanel(new GridLayout(1, 1, 5, 5));
    JTextField keyField = new JTextField("Key");
    keyPanel.add(keyField);
    bumpPanel.add(keyPanel);

//    outerPanel.add(getInputPanel(), BorderLayout.CENTER);
//    outerPanel.add(getTextPanel(), BorderLayout.NORTH);
//    c.setLayout(new BorderLayout());
//    c.add(outerPanel, BorderLayout.CENTER);
//    c.add(getUsersPanel(), BorderLayout.WEST);

    bumpFrame.add(c);
    bumpFrame.pack();
    bumpFrame.setLocation(150, 150);

    bumpFrame.setDefaultCloseOperation(EXIT_ON_CLOSE);
    bumpFrame.setVisible(true);





//    final JFrame f= new JFrame("PopupMenu Example");
//    final JPopupMenu popupmenu = new JPopupMenu("Edit");
//    JMenuItem cut = new JMenuItem("Cut");
//    JMenuItem copy = new JMenuItem("Copy");
//    JMenuItem paste = new JMenuItem("Paste");
//    popupmenu.add(cut); popupmenu.add(copy); popupmenu.add(paste);
//    f.addMouseListener(new MouseAdapter() {
//      public void mouseClicked(MouseEvent e) {
//        popupmenu.show(f , e.getX(), e.getY());
//      }
//    });
//    f.add(popupmenu);
//    f.setSize(300,300);
//    f.setLayout(null);
//    f.setVisible(true);
  }

//  private void sendMessage(String chatMessage) throws RemoteException {
//    chatClient.sendGroupMessage(chatMessage, name);
//  }
//
  private void sendPrivate(int receiver) throws RemoteException {
    chatClient.sendPM(receiver, message);
  }

  private void getMessage(int sender) throws RemoteException {
    chatClient.getPM(sender);
  }

  private void getConnected(String userName) throws RemoteException {
    String cleanedUserName = userName.replaceAll("\\W+", "_");
    try {
      chatClient = new Client(this, cleanedUserName);
      chatClient.start();
    } catch (RemoteException e) {
      e.printStackTrace();
    }
  }

}
