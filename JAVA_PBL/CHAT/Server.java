import java.awt.*;
import java.awt.event.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import javax.swing.*;

public class Server extends JFrame {
    private static final int CHAT_PORT = 8888;
    private static final int DISCOVERY_PORT = 9999;
    private static final String DISCOVERY_REQUEST = "CHAT_SERVER_DISCOVERY";
    private static final String DISCOVERY_RESPONSE = "CHAT_SERVER_HERE";

    private final JTextField textField;
    private final JPanel chatPanel;
    private final JScrollPane scrollPane;

    private ServerSocket serverSocket;
    private DatagramSocket discoverySocket;
    private final List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());
    private Connection dbConn;

    // Helper method to load and scale images.
    private ImageIcon loadImage(String path, int width, int height) {
        java.net.URL imgUrl = ClassLoader.getSystemResource(path);
        if (imgUrl == null) {
            System.out.println("Image not found: " + path);
            return null;
        }
        ImageIcon icon = new ImageIcon(imgUrl);
        Image scaledImg = icon.getImage().getScaledInstance(width, height, Image.SCALE_DEFAULT);
        return new ImageIcon(scaledImg);
    }

    private JLabel createLabel(String text, int x, int y, int width, int height, Color color, int fontStyle, int fontSize) {
        JLabel label = new JLabel(text);
        label.setBounds(x, y, width, height);
        label.setForeground(color);
        label.setFont(new Font("SAN_SERIF", fontStyle, fontSize));
        return label;
    }

private void nischay()
    {
        if (7 % 2 == 0) 
        {
            System.out.println("Even");
        }
        else 
        {
            System.out.println("Odd");
        }
    }
    Server() {
        setLayout(null);
        setTitle("Server Chat");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel p1 = new JPanel();
        p1.setBackground(new Color(0, 70, 140));
        p1.setBounds(0, 0, 500, 90);
        p1.setLayout(null);
        add(p1);

        ImageIcon i3 = loadImage("Icons/—Pngtree—vector left arrow icon_4184717.png", 50, 50);
        JLabel back = new JLabel(i3);
        back.setBounds(5, 20, 50, 50);
        p1.add(back);
        back.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                dispose();
                closeConnections();
            }
        });

        ImageIcon profileImg = loadImage("Icons/profile_1.png", 50, 50);
        JLabel profileLabel = new JLabel(profileImg);
        profileLabel.setBounds(40, 10, 50, 50);
        p1.add(profileLabel);

        ImageIcon videoIcon = loadImage("Icons/video.png", 50, 50);
        JLabel videoLabel = new JLabel(videoIcon);
        videoLabel.setBounds(335,10,50,50);
        p1.add(videoLabel);

        ImageIcon phoneIcon = loadImage("Icons/phone-call.png", 50, 50);
        JLabel phoneLabel = new JLabel(phoneIcon);
        phoneLabel.setBounds(390, 10, 50, 50);
        p1.add(phoneLabel);

        ImageIcon moreIcon = loadImage("Icons/more.png", 50, 50);
        JLabel moreLabel = new JLabel(moreIcon);
        moreLabel.setBounds(445, 10, 50, 50);
        p1.add(moreLabel);

        JLabel name = createLabel("SERVER", 110, 15, 140, 20, Color.WHITE, Font.BOLD, 18);
        p1.add(name);

        JLabel status = createLabel("Waiting for clients...", 110, 35, 200, 20, new Color(224, 236, 255), Font.PLAIN, 14);
        p1.add(status);

        chatPanel = new JPanel();
        chatPanel.setBackground(new Color(238, 242, 247));
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));

        scrollPane = new JScrollPane(chatPanel);
        scrollPane.setBounds(0, 90, 500, 540);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane);

        textField = new JTextField();
        textField.setBounds(12, 640, 360, 38);
        textField.setBackground(Color.WHITE);
        textField.setForeground(new Color(25, 25, 25));
        textField.setCaretColor(new Color(25, 25, 25));
        textField.addActionListener(evt -> handleSend());
        add(textField);

        JButton sendButton = new JButton("Send");
        sendButton.setBounds(380, 640, 105, 38);
        sendButton.setFont(new Font("SAN_SERIF", Font.BOLD, 14));
        sendButton.setBackground(new Color(0, 84, 166));
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);
        sendButton.addActionListener(evt -> handleSend());
        add(sendButton);

        setSize(500, 720);
        setLocation(250, 50);
        getContentPane().setBackground(Color.white);
        setVisible(true);

        initDB();
        restoreChatHistory();
        addAndSaveSystemMessage("Server started on port " + CHAT_PORT);
        startServer(status);
        startDiscoveryService();
    }
private boolean anshul()
    {
        int a=10;
        if(a==10)
        {
            return true;
        }
        else
        {
            return false;
        }
    }
    private void startDiscoveryService() {
        Thread discoveryThread = new Thread(() -> {
            try {
                discoverySocket = new DatagramSocket(DISCOVERY_PORT, InetAddress.getByName("0.0.0.0"));
                discoverySocket.setBroadcast(true);

                while (!discoverySocket.isClosed()) {
                    byte[] buffer = new byte[256];
                    DatagramPacket requestPacket = new DatagramPacket(buffer, buffer.length);
                    discoverySocket.receive(requestPacket);

                    String request = new String(
                            requestPacket.getData(),
                            0,
                            requestPacket.getLength(),
                            StandardCharsets.UTF_8
                    ).trim();

                    if (!DISCOVERY_REQUEST.equals(request)) {
                        continue;
                    }

                    byte[] responseData = DISCOVERY_RESPONSE.getBytes(StandardCharsets.UTF_8);
                    DatagramPacket responsePacket = new DatagramPacket(
                            responseData,
                            responseData.length,
                            requestPacket.getAddress(),
                            requestPacket.getPort()
                    );
                    discoverySocket.send(responsePacket);
                }
            } catch (IOException ignored) {
            }
        }, "server-discovery");
        discoveryThread.setDaemon(true);
        discoveryThread.start();
    }
private int aviral()
    {
        int sum=0;
        for(int i=0; i<10; i++)
            {
                sum=+i;
            }
        return sum;
    }
    private void startServer(JLabel statusLabel) {
        Thread serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(CHAT_PORT);
                while (true) {
                    Socket socket = serverSocket.accept();
                    ClientHandler clientHandler = new ClientHandler(socket, statusLabel);
                    Thread handlerThread = new Thread(clientHandler, "client-handler-" + socket.getPort());
                    handlerThread.setDaemon(true);
                    handlerThread.start();
                }
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> addMessage("Connection closed", false));
            } finally {
                closeConnections(); 
            }
        }, "server-listener");
        serverThread.setDaemon(true);
        serverThread.start();
    }

    private void handleSend() {
        String message = textField.getText().trim();
        if (message.isEmpty()) {
            return;
        }

        saveMessage("Server", message);
        String formattedMessage = "Server: " + message;
        addMessage(formattedMessage, true);
        textField.setText("");

        if (clients.isEmpty()) {
            addMessage("No clients connected yet", false);
            return;
        }

        broadcast(formattedMessage, null);
    }

    private void addMessage(String message, boolean sentByMe) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> addMessage(message, sentByMe));
            return;
        }

        JPanel bubble = formatLabel(message, sentByMe);

        // Override getMaximumSize so BoxLayout Y_AXIS uses the real preferred
        // height instead of stretching the row to fill remaining space.
        JPanel align = new JPanel(new BorderLayout()) {
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        align.setOpaque(false);
        align.add(bubble, sentByMe ? BorderLayout.LINE_END : BorderLayout.LINE_START);

        chatPanel.add(align);
        chatPanel.add(Box.createVerticalStrut(10));
        chatPanel.revalidate();
        chatPanel.repaint();

        // Scroll to the bottom after the layout pass finishes
        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = scrollPane.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }

    // Displays a historical message using the timestamp stored in the DB
    private void addHistoryMessage(String message, boolean sentByMe, String timestamp) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> addHistoryMessage(message, sentByMe, timestamp));
            return;
        }

        JPanel bubble = formatLabel(message, sentByMe, timestamp);

        JPanel align = new JPanel(new BorderLayout()) {
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        align.setOpaque(false);
        align.add(bubble, sentByMe ? BorderLayout.LINE_END : BorderLayout.LINE_START);

        chatPanel.add(align);
        chatPanel.add(Box.createVerticalStrut(10));
        chatPanel.revalidate();
        chatPanel.repaint();

        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = scrollPane.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }

    private void broadcast(String message, ClientHandler sender) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }

        List<ClientHandler> snapshot;
        synchronized (clients) {
            snapshot = new ArrayList<>(clients);
        }

        for (ClientHandler client : snapshot) {
            client.send(message);
        }

        if (sender != null) {
            addMessage(message, false);
        }
    }

    private void updateStatus(JLabel statusLabel) {
        int count;
        synchronized (clients) {
            count = clients.size();
        }
        String text = count == 0 ? "Waiting for clients..." : "Clients connected: " + count;
        SwingUtilities.invokeLater(() -> statusLabel.setText(text));
    }

    public static JPanel formatLabel(String out, boolean sentByMe) {
        Calendar cal = Calendar.getInstance();
        String ts = new SimpleDateFormat("HH:mm").format(cal.getTime());
        return formatLabel(out, sentByMe, ts);
    }

    // Overload: uses a provided timestamp string instead of current time
    public static JPanel formatLabel(String out, boolean sentByMe, String timestamp) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        Color bubbleColor = sentByMe ? new Color(0, 84, 166) : new Color(222, 232, 246);
        Color textColor = sentByMe ? Color.WHITE : new Color(20, 20, 20);

        JLabel output = new JLabel("<html><p style=\"width: 150px\">" + out + "</p></html>");
        output.setFont(new Font("Tahoma", Font.PLAIN, 16));
        output.setForeground(textColor);
        output.setBackground(bubbleColor);
        output.setOpaque(true);
        output.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(output);

        JLabel time = new JLabel(timestamp);
        time.setForeground(new Color(70, 70, 70));
        panel.add(time);

        return panel;
    }

    private void closeConnections() {
        List<ClientHandler> snapshot;
        synchronized (clients) {
            snapshot = new ArrayList<>(clients);
            clients.clear();
        }

        for (ClientHandler client : snapshot) {
            client.close();
        }

        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }
        if (discoverySocket != null && !discoverySocket.isClosed()) {
            discoverySocket.close();
        }
    }

    // ─── SQLite helpers ──────────────────────────────────────────────────────

    private void initDB() {
        try {
            Class.forName("org.sqlite.JDBC");
            dbConn = DriverManager.getConnection("jdbc:sqlite:chat.db");
            Statement st = dbConn.createStatement();
            st.execute("CREATE TABLE IF NOT EXISTS messages ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "username TEXT, message TEXT, timestamp TEXT)");
            st.close();
        } catch (Throwable t) {
            dbConn = null;
            System.out.println("DB init failed (chat will run without history persistence): " + t);
            if (t instanceof NoClassDefFoundError
                    || (t.getCause() != null && t.getCause() instanceof NoClassDefFoundError)) {
                System.out.println("Hint: add SLF4J jars (slf4j-api and slf4j-simple/slf4j-nop) next to sqlite-jdbc.jar.");
            }
        }
    }

    private synchronized void saveMessage(String username, String message) {
        if (dbConn == null || message == null || message.trim().isEmpty()) return;
        try {
            String ts = new SimpleDateFormat("HH:mm").format(Calendar.getInstance().getTime());
            PreparedStatement ps = dbConn.prepareStatement(
                    "INSERT INTO messages (username, message, timestamp) VALUES (?, ?, ?)");
            ps.setString(1, username);
            ps.setString(2, message);
            ps.setString(3, ts);
            ps.executeUpdate();
            ps.close();
            pruneMessages();
        } catch (Exception e) {
            System.out.println("DB save failed: " + e.getMessage());
        }
    }

    private synchronized List<String[]> fetchLast20() {
        List<String[]> result = new ArrayList<>();
        if (dbConn == null) return result;
        try {
            PreparedStatement ps = dbConn.prepareStatement(
                    "SELECT username, message, timestamp FROM messages ORDER BY id DESC LIMIT 20");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result.add(new String[]{
                    rs.getString("username"),
                    rs.getString("message"),
                    rs.getString("timestamp")
                });
            }
            rs.close();
            ps.close();
        } catch (Exception e) {
            System.out.println("DB fetch failed: " + e.getMessage());
        }
        Collections.reverse(result); // oldest first → newest last
        return result;
    }

    private void restoreChatHistory() {
        List<String[]> history = fetchLast20();
        if (history.isEmpty()) return;
        for (String[] row : history) {
            // row = [username, message, timestamp]
            // SYSTEM messages are stored without a sender prefix
            String display = "SYSTEM".equals(row[0]) ? row[1] : row[0] + ": " + row[1];
            boolean sentByMe = "Server".equals(row[0]);
            addHistoryMessage(display, sentByMe, row[2]);
        }
    }

    // Saves a system/status message to DB and displays it in the chat panel
    private void addAndSaveSystemMessage(String message) {
        saveMessage("SYSTEM", message);
        addMessage(message, false);
    }

    private synchronized void pruneMessages() {
        if (dbConn == null) return;
        try {
            Statement st = dbConn.createStatement();
            st.execute("DELETE FROM messages WHERE id NOT IN "
                    + "(SELECT id FROM messages ORDER BY id DESC LIMIT 100)");
            st.close();
        } catch (Exception e) {
            System.out.println("DB prune failed: " + e.getMessage());
        }
    }

    private class ClientHandler implements Runnable {
        private final Socket socket;
        private final JLabel statusLabel;
        private DataInputStream dis;
        private DataOutputStream dos;
        private String username = "User";

        ClientHandler(Socket socket, JLabel statusLabel) {
            this.socket = socket;
            this.statusLabel = statusLabel;
        }

        @Override
        public void run() {
            try {
                dis = new DataInputStream(socket.getInputStream());
                dos = new DataOutputStream(socket.getOutputStream());

                String receivedName = dis.readUTF().trim();
                if (!receivedName.isEmpty()) {
                    username = receivedName;
                }

                // Send chat history to the newly connected client
                List<String[]> history = fetchLast20();
                for (String[] row : history) {
                    // row = [username, message, timestamp]
                    // "Server started on port" is server-internal — skip for clients
                    if ("SYSTEM".equals(row[0]) && row[1].startsWith("Server started on port")) continue;
                    // Other SYSTEM messages (join/leave) are sent as-is (no "SYSTEM: " prefix)
                    String display = "SYSTEM".equals(row[0]) ? row[1] : row[0] + ": " + row[1];
                    send("##HIST##" + row[2] + "|" + display);
                }
                send("##HISTORY_END##");

                synchronized (clients) {
                    clients.add(this);
                }

                updateStatus(statusLabel);
                addAndSaveSystemMessage(username + " joined the chat");

                while (true) {
                    String msg = dis.readUTF();
                    if (msg == null) {
                        break;
                    }
                    msg = msg.trim();
                    if (msg.isEmpty()) {
                        continue;
                    }

                    saveMessage(username, msg);
                    broadcast(username + ": " + msg, this);
                }
            } catch (IOException ignored) {
            } finally {
                synchronized (clients) {
                    clients.remove(this);
                }
                updateStatus(statusLabel);
                addAndSaveSystemMessage(username + " left the chat");
                close();
            }
        }

        void send(String message) {
            if (dos == null) {
                return;
            }

            try {
                dos.writeUTF(message);
                dos.flush();
            } catch (IOException ex) {
                close();
                synchronized (clients) {
                    clients.remove(this);
                }
                updateStatus(statusLabel);
            }
        }

        void close() {
            try {
                if (dis != null) {
                    dis.close();
                }
            } catch (IOException ignored) {
            }
            try {
                if (dos != null) {
                    dos.close();
                }
            } catch (IOException ignored) {
            }
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException ignored) {
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Server::new);
    }
}
