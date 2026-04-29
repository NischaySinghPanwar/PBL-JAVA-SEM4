import java.awt.*;
import java.awt.event.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.*;


public class Client extends JFrame {
    private static final int CHAT_PORT = 8888;
    private static final int DISCOVERY_PORT = 9999;
    private static final String DISCOVERY_REQUEST = "CHAT_SERVER_DISCOVERY";
    private static final String DISCOVERY_RESPONSE = "CHAT_SERVER_HERE";

    private final JTextField textField;
    private final JPanel chatPanel;
    private final JScrollPane scrollPane;
    private final String username;

    private DataInputStream dis;
    private DataOutputStream dos;
    private Socket socket;
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

    private JLabel createLabel(String text, int x, int y, int width, int height, Color color, int fontStyle,
            int fontSize) {
        JLabel label = new JLabel(text);
        label.setBounds(x, y, width, height);
        label.setForeground(color);
        label.setFont(new Font("SAN_SERIF", fontStyle, fontSize));
        return label;
    }

    Client() {
        username = askUsername();

        setLayout(null);
        setTitle("Client Chat");
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

        ImageIcon profileImg = loadImage("Icons/img2.png", 50, 50);
        JLabel profileLabel = new JLabel(profileImg);
        profileLabel.setBounds(40, 10, 50, 50);
        p1.add(profileLabel);

        ImageIcon videoIcon = loadImage("Icons/video.png", 50, 50);
        JLabel videoLabel = new JLabel(videoIcon);
        videoLabel.setBounds(335, 10, 50, 50);
        p1.add(videoLabel);

        ImageIcon phoneIcon = loadImage("Icons/phone-call.png", 50, 50);
        JLabel phoneLabel = new JLabel(phoneIcon);
        phoneLabel.setBounds(390, 10, 50, 50);
        p1.add(phoneLabel);

        ImageIcon moreIcon = loadImage("Icons/more.png", 50, 50);
        JLabel moreLabel = new JLabel(moreIcon);
        moreLabel.setBounds(445, 10, 50, 50);
        p1.add(moreLabel);

        JLabel name = createLabel(username, 110, 15, 180, 20, Color.WHITE, Font.BOLD, 18);
        p1.add(name);

        JLabel status = createLabel("Connecting...", 110, 35, 170, 20, new Color(224, 236, 255), Font.PLAIN, 14);
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
        setLocation(780, 50);
        getContentPane().setBackground(Color.white);
        setVisible(true);

        initDB();
        restoreClientHistory();
        connectToServer(status);
    }

    private String askUsername() {
        String name = JOptionPane.showInputDialog(
                null,
                "Enter your username:",
                "Username",
                JOptionPane.PLAIN_MESSAGE);

        if (name == null || name.trim().isEmpty()) {
            return "User" + (System.currentTimeMillis() % 1000);
        }

        return name.trim();
    }

    private void connectToServer(JLabel statusLabel) {
        Thread clientThread = new Thread(() -> {
            try {
                String discoveredServerIP = discoverServerIP();
                boolean autoDetected = discoveredServerIP != null;

                if (discoveredServerIP == null) {
                    SwingUtilities.invokeLater(() -> addMessage("Server not found on this network", false));
                    discoveredServerIP = askManualServerIP();
                    if (discoveredServerIP == null) {
                        SwingUtilities.invokeLater(() -> statusLabel.setText("Server not found"));
                        return;
                    }
                }

                socket = new Socket(discoveredServerIP, CHAT_PORT);
                dis = new DataInputStream(socket.getInputStream());
                dos = new DataOutputStream(socket.getOutputStream());

                dos.writeUTF(username);
                dos.flush();

                SwingUtilities.invokeLater(() -> statusLabel.setText("Connected"));

                // Always silently consume server-sent history.
                // Client only restores from its own local DB (client_<username>.db).
                // New clients start with a blank screen and build their own history as they chat.
                String histLine;
                while (true) {
                    histLine = dis.readUTF();
                    if (histLine.equals("##HISTORY_END##")) break;
                }

                // Fresh connection messages — saved to client DB and shown at bottom
                SwingUtilities.invokeLater(() -> {
                    addMessage("Searching for server on local network...", false);
                    String connectionMsg = (autoDetected ? "Server detected automatically" : "Connected using manual server IP")
                                         + ".<br/>Now, connected to server.";
                    addMessage(connectionMsg, false);
                });

                // Live message loop
                while (true) {
                    String msg = dis.readUTF();
                    boolean sentByMe = msg.startsWith(username + ": ");
                    SwingUtilities.invokeLater(() -> addMessage(msg, sentByMe));
                }
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Disconnected");
                    addMessage("Unable to connect / connection closed", false);
                });
            } finally {
                closeConnections();
            }
        }, "client-listener");
        clientThread.setDaemon(true);
        clientThread.start();
    }

    private String askManualServerIP() {
        String ip = JOptionPane.showInputDialog(
                this,
                "Server not found automatically.\nEnter Server IP manually:",
                "Manual Server IP",
                JOptionPane.PLAIN_MESSAGE);

        if (ip == null || ip.trim().isEmpty()) {
            return null;
        }

        return ip.trim();
    }

    private String discoverServerIP() {
        String serverIp = discoverViaBroadcast();
        if (serverIp != null) {
            return serverIp;
        }

        return discoverViaSubnetUnicast();
    }

    private String discoverViaBroadcast() {
        try (DatagramSocket discoverySocket = new DatagramSocket()) {
            discoverySocket.setBroadcast(true);
            discoverySocket.setSoTimeout(1500);

            byte[] requestData = DISCOVERY_REQUEST.getBytes(StandardCharsets.UTF_8);

            DatagramPacket globalBroadcast = new DatagramPacket(
                    requestData,
                    requestData.length,
                    InetAddress.getByName("255.255.255.255"),
                    DISCOVERY_PORT);
            discoverySocket.send(globalBroadcast);

            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (!ni.isUp() || ni.isLoopback()) {
                    continue;
                }

                for (InterfaceAddress interfaceAddress : ni.getInterfaceAddresses()) {
                    InetAddress broadcast = interfaceAddress.getBroadcast();
                    if (broadcast == null) {
                        continue;
                    }

                    DatagramPacket packet = new DatagramPacket(
                            requestData,
                            requestData.length,
                            broadcast,
                            DISCOVERY_PORT);
                    discoverySocket.send(packet);
                }
            }

            return waitForDiscoveryReply(discoverySocket);
        } catch (IOException ignored) {
            return null;
        }
    }

    private String discoverViaSubnetUnicast() {
        Set<String> prefixes = getLocalSubnetPrefixes();
        if (prefixes.isEmpty()) {
            return null;
        }

        try (DatagramSocket discoverySocket = new DatagramSocket()) {
            discoverySocket.setSoTimeout(1500);

            byte[] requestData = DISCOVERY_REQUEST.getBytes(StandardCharsets.UTF_8);

            for (String prefix : prefixes) {
                for (int host = 1; host <= 254; host++) {
                    String targetIp = prefix + host;
                    DatagramPacket packet = new DatagramPacket(
                            requestData,
                            requestData.length,
                            InetAddress.getByName(targetIp),
                            DISCOVERY_PORT);
                    discoverySocket.send(packet);
                }
            }

            return waitForDiscoveryReply(discoverySocket);
        } catch (IOException ignored) {
            return null;
        }
    }

    private String waitForDiscoveryReply(DatagramSocket socket) {
        long deadline = System.currentTimeMillis() + 1800;
        while (System.currentTimeMillis() < deadline) {
            try {
                byte[] responseBuffer = new byte[256];
                DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
                socket.receive(responsePacket);

                String response = new String(
                        responsePacket.getData(),
                        0,
                        responsePacket.getLength(),
                        StandardCharsets.UTF_8).trim();

                if (DISCOVERY_RESPONSE.equals(response)) {
                    return responsePacket.getAddress().getHostAddress();
                }
            } catch (SocketTimeoutException ignored) {
                return null;
            } catch (IOException ignored) {
                return null;
            }
        }
        return null;
    }

    private Set<String> getLocalSubnetPrefixes() {
        Set<String> prefixes = new HashSet<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (!ni.isUp() || ni.isLoopback()) {
                    continue;
                }

                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (!(address instanceof Inet4Address) || address.isLoopbackAddress()) {
                        continue;
                    }

                    String ip = address.getHostAddress();
                    int lastDot = ip.lastIndexOf('.');
                    if (lastDot > 0) {
                        prefixes.add(ip.substring(0, lastDot + 1));
                    }
                }
            }
        } catch (IOException ignored) {
        }

        return prefixes;
    }

    private void handleSend() {
        String message = textField.getText().trim();
        if (message.isEmpty()) {
            return;
        }
        textField.setText("");

        if (dos != null) {
            try {
                dos.writeUTF(message);
                dos.flush();
            } catch (IOException ex) {
                addMessage("Failed to send message", false);
            }
        } else {
            addMessage("Not connected to server", false);
        }
    }

    private void addMessage(String message, boolean sentByMe) {
        // Save every displayed message to the client's own DB
        saveMessage(sentByMe ? username : "OTHER", message);

        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> addMessageUI(message, sentByMe));
            return;
        }
        addMessageUI(message, sentByMe);
    }

    // UI-only helper — does not save to DB
    private void addMessageUI(String message, boolean sentByMe) {
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

    // Overload: uses a provided timestamp string instead of current time
    public static JPanel formatLabel(String out, boolean sentByMe, String timestamp) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        Color bubbleColor = sentByMe ? new Color(0, 84, 166) : new Color(222, 232, 246);
        Color textColor   = sentByMe ? Color.WHITE : new Color(20, 20, 20);

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

    public static JPanel formatLabel(String out, boolean sentByMe) {
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

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        JLabel time = new JLabel(sdf.format(cal.getTime()));
        time.setForeground(new Color(70, 70, 70));
        panel.add(time);

        return panel;
    }

    private void closeConnections() {
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
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }

    // ─── Client-side SQLite helpers ───────────────────────────────────────────

    private void initDB() {
        try {
            Class.forName("org.sqlite.JDBC");
            // Each username gets its own history file
            dbConn = DriverManager.getConnection("jdbc:sqlite:client_" + username + ".db");
            java.sql.Statement st = dbConn.createStatement();
            st.execute("CREATE TABLE IF NOT EXISTS messages ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "username TEXT, message TEXT, timestamp TEXT)");
            st.close();
        } catch (Throwable t) {
            dbConn = null;
            System.out.println("Client DB init failed (will run without local history): " + t);
        }
    }

    private synchronized void saveMessage(String who, String message) {
        if (dbConn == null || message == null || message.trim().isEmpty()) return;
        try {
            String ts = new SimpleDateFormat("HH:mm").format(Calendar.getInstance().getTime());
            PreparedStatement ps = dbConn.prepareStatement(
                    "INSERT INTO messages (username, message, timestamp) VALUES (?, ?, ?)");
            ps.setString(1, who);
            ps.setString(2, message);
            ps.setString(3, ts);
            ps.executeUpdate();
            ps.close();
            pruneMessages();
        } catch (Exception e) {
            System.out.println("Client DB save failed: " + e.getMessage());
        }
    }

    private synchronized void saveMessageWithTimestamp(String who, String message, String timestamp) {
        if (dbConn == null || message == null || message.trim().isEmpty()) return;
        try {
            PreparedStatement ps = dbConn.prepareStatement(
                    "INSERT INTO messages (username, message, timestamp) VALUES (?, ?, ?)");
            ps.setString(1, who);
            ps.setString(2, message);
            ps.setString(3, timestamp);
            ps.executeUpdate();
            ps.close();
            pruneMessages();
        } catch (Exception e) {
            System.out.println("Client DB save failed: " + e.getMessage());
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
            System.out.println("Client DB fetch failed: " + e.getMessage());
        }
        Collections.reverse(result); // oldest first → newest last
        return result;
    }

    private synchronized void pruneMessages() {
        if (dbConn == null) return;
        try {
            java.sql.Statement st = dbConn.createStatement();
            st.execute("DELETE FROM messages WHERE id NOT IN "
                    + "(SELECT id FROM messages ORDER BY id DESC LIMIT 100)");
            st.close();
        } catch (Exception e) {
            System.out.println("Client DB prune failed: " + e.getMessage());
        }
    }

    private void restoreClientHistory() {
        List<String[]> history = fetchLast20();
        if (history.isEmpty()) return;
        for (String[] row : history) {
            // row = [username, message, timestamp]
            boolean sentByMe = username.equals(row[0]);
            // Show message as-is (it was already formatted when first saved)
            addHistoryMessage(row[1], sentByMe, row[2]);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Client::new);
    }
}
