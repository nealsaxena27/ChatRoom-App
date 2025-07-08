package client;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;

public class ChatClient extends JFrame {
    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;
    private String username;

    private JTextArea messageArea;
    private JTextField inputField;

    public ChatClient(Socket socket, String username) {
        this.socket = socket;
        this.username = username;

        try {
            this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Send initial username
            out.write(this.username);
            out.newLine();
            out.flush();
        } catch (IOException e) {
            closeEverything();
        }

        setupGUI();
        listenForMessage();
    }

    private void setupButton(JButton button, Color bg, Color fg){
        button.setBackground(bg);
        button.setForeground(fg);
        button.setFont(new Font("Times New Roman", Font.BOLD, 20));
        button.setFocusable(false);
    }

    private void setupGUI() {
        Color black = new Color(0x222831);
        Color grey = new Color(0x31363F);
        Color blue = new Color(0x76ABAE);
        Color white = new Color(0xEEEEEE);
        setTitle("Chat App");
        setSize(640, 480);
        setLayout(new BorderLayout());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        System.out.println(new File("./src/main/resources/logo.png").getAbsolutePath());
        ImageIcon logo = new ImageIcon("./src/main/resources/logo.png");
        setIconImage(logo.getImage());

        JPanel topPanel = new JPanel();
        topPanel.setPreferredSize(new Dimension(getWidth(), 60));
        topPanel.setBackground(blue);
        topPanel.setLayout(new BorderLayout());
        JButton usernameButton = new JButton("Username");
        JButton quitButton = new JButton("Quit");
        setupButton(usernameButton, blue, white);
        setupButton(quitButton, blue, white);

        topPanel.add(usernameButton, BorderLayout.WEST);
        topPanel.add(quitButton, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        JPanel leftPanel = new JPanel();
        leftPanel.setBackground(black);
        leftPanel.setLayout(new GridLayout(4, 1));
        JButton roomsButton = new JButton("Rooms");
        JButton usersButton = new JButton("Users");
        JButton joinButton = new JButton("Join");
        JButton leaveButton = new JButton("Leave");
        setupButton(roomsButton, black, white);
        setupButton(usersButton, black, white);
        setupButton(joinButton, black, white);
        setupButton(leaveButton, black, white);

        leftPanel.add(roomsButton);
        leftPanel.add(usersButton);
        leftPanel.add(joinButton);
        leftPanel.add(leaveButton);
        add(leftPanel, BorderLayout.WEST);

        messageArea = new JTextArea();
        messageArea.setBackground(grey);
        messageArea.setForeground(white);
        messageArea.setFont(new Font("Times New Roman", Font.PLAIN, 20));
        messageArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(messageArea);
        add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setPreferredSize(new Dimension(getWidth(), 40));
        ImageIcon sendIcon = new ImageIcon("./src/main/resources/send.png");
        Image scaledImage = sendIcon.getImage().getScaledInstance(32, 32, Image.SCALE_SMOOTH);
        JLabel iconLabel = new JLabel(new ImageIcon(scaledImage));

        inputField = new JTextField();
        inputField.setBackground(white);
        inputField.setForeground(black);
        inputField.setFont(new Font("Times New Roman", Font.PLAIN, 20));
        
        bottomPanel.add(iconLabel, BorderLayout.WEST);
        bottomPanel.add(inputField, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        usernameButton.addActionListener(_ -> {
            String newUsername = JOptionPane.showInputDialog(this, "Enter new username:");
            if (newUsername != null && !newUsername.isBlank()) {
                sendMessage("/nick " + newUsername);
                username = newUsername;
            }
        });

        quitButton.addActionListener(_ -> {
            sendMessage("/quit");
            closeEverything();
            System.exit(0);
        });

        roomsButton.addActionListener(_ -> sendMessage("/rooms"));
        
        usersButton.addActionListener(_ -> sendMessage("/users"));

        joinButton.addActionListener(_ -> {
            String room = JOptionPane.showInputDialog(this, "Enter room name:");
            if (room != null && !room.isBlank()) {
                sendMessage("/join " + room);
            }
        });

        leaveButton.addActionListener(_ -> sendMessage("/leave"));

        inputField.addActionListener(_ -> {
            String msg = inputField.getText();
            if (!msg.isBlank()) {
                sendMessage(msg);
                inputField.setText("");
            }
        });

        setVisible(true);
    }

    private void sendMessage(String message) {
        try {
            out.write(message);
            out.newLine();
            out.flush();
        } catch (IOException e) {
            closeEverything();
        }
    }

    private void listenForMessage() {
        new Thread(() -> {
            String messageFromChat;
            while (socket.isConnected()) {
                try {
                    messageFromChat = in.readLine();
                    if (messageFromChat != null) {
                        messageArea.append(messageFromChat + "\n");
                    }
                } catch (IOException e) {
                    closeEverything();
                    break;
                }
            }
        }).start();
    }

    private void closeEverything() {
        try {
            if (socket != null) socket.close();
            if (in != null) in.close();
            if (out != null) out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        String username = JOptionPane.showInputDialog("Enter your username:");
        if (username != null && !username.isBlank()) {
            Socket socket = new Socket("localhost", 3001);
            new ChatClient(socket, username);
        }
    }
}
