import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author xxkun
 * @creed Awaken the Giant Within
 * @description: relay server
 * @date 2021-01-30 10:23
 */
public class Server implements UDPReceiveLoopThread.Status {

    private static final long HEARTBEAT_INTERVAL = 10 * 1000;   // Heartbeat interval (ms)

    private Map<String, ClientInfo> clientPool;
    private boolean stop = false;
    private DatagramSocket server;

    public void start(int port) {
        clientPool = new HashMap<>();
        try {
            server = new DatagramSocket(port);
            System.out.println("Server start on " + server.getLocalSocketAddress());
            new UDPReceiveLoopThread(server, this).start();
            new HeartbeatThread().start();
        } catch (SocketException e) {
            e.printStackTrace();
            stop = true;
        }
    }

    public void onMessage(SocketAddress from, Message msg) {
//        System.out.println("RECV FROM Client " + getAddressStr(from) + ":" + msg.getType().getCode() + " -> " + msg.getBody().trim());
        switch (msg.getType()) {
            case MSGT_LOGIN:
                if (!clientPool.containsKey(getAddressStr(from))) {
                    clientPool.put(getAddressStr(from), new ClientInfo(from));
                    System.out.println("Client " + getAddressStr(from) + " logged in");
                    TransferUtil.udpSendMsg(server, from, Message.MessageType.MSGT_LOGIN, null);
                } else {
                    System.out.println("Client " + getAddressStr(from) + " failed to login");
                    TransferUtil.udpSendMsg(server, from, Message.MessageType.MSGT_REPLY, "Login failed");
                }
                break;
            case MSGT_LOGOUT:
                if (clientPool.remove(getAddressStr(from)) != null) {
                    System.out.println("Client " + getAddressStr(from) + " logged out");
                    TransferUtil.udpSendMsg(server, from, Message.MessageType.MSGT_REPLY, "Logout success");
                } else {
                    System.out.println("Client " + getAddressStr(from) + " failed to logout");
                    TransferUtil.udpSendMsg(server, from, Message.MessageType.MSGT_REPLY, "Logout failed");
                }
                break;
            case MSGT_LIST:
                System.out.println("Client " + getAddressStr(from) + " query list ");
                String fromStr = getAddressStr(from);
                String text = clientPool.keySet().stream().filter(s -> !s.equals(fromStr)).collect(Collectors.joining(","));
                TransferUtil.udpSendMsg(server, from, Message.MessageType.MSGT_REPLY, "[" + text + "]");
                break;
            case MSGT_PUNCH:
                SocketAddress other = TransferUtil.getSocketAddressFromString(msg.getBody());
                if (other == null) {
                    System.out.println("Address format error");
                    break;
                }
                System.out.println("Client " + getAddressStr(from) + " punching to " + getAddressStr(other));
                TransferUtil.udpSendMsg(server, other, Message.MessageType.MSGT_PUNCH, getAddressStr(from));
                TransferUtil.udpSendMsg(server, from, Message.MessageType.MSGT_TEXT, "punch request sent");
                break;
            case MSGT_HEARTBEAT:
                ClientInfo client = clientPool.get(getAddressStr(from));
                if (client != null) {
                    client.setLastConnectDate(new Date());
                    System.out.println("Client " + getAddressStr(from) + " alive");
                }
                break;
            default:
                TransferUtil.udpSendMsg(server, from, Message.MessageType.MSGT_REPLY, "Unknown command");
                break;
        }
    }

    @Override
    public boolean isStop() {
        return stop;
    }

    private class HeartbeatThread extends Thread {
        @Override
        public void run() {
            while (!stop) {
                try {
                    sleep(HEARTBEAT_INTERVAL);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    stop = true;
                    break;
                }
                Iterator<ClientInfo> iterator = clientPool.values().iterator();
                Date now = new Date();
                while (iterator.hasNext()) {
                    ClientInfo client = iterator.next();
                    if (now.getTime() - client.getLastConnectDate().getTime() > HEARTBEAT_INTERVAL) {
                        System.out.println("Client " + getAddressStr(client.getSocketAddress()) + " logout");
                        iterator.remove();
                    }
                }
            }
        }
    }

    public String getAddressStr(SocketAddress address) {
        return address.toString().substring(1);
    }

    private class ClientInfo {
        private final SocketAddress socketAddress;

        private Date lastConnectDate;

        public ClientInfo(SocketAddress address) {
            socketAddress = address;
            lastConnectDate = new Date();
        }

        public void setLastConnectDate(Date lastConnectDate) {
            this.lastConnectDate = lastConnectDate;
        }

        public Date getLastConnectDate() {
            return lastConnectDate;
        }

        public SocketAddress getSocketAddress() {
            return socketAddress;
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java " + Server.class.getName() + " <port>\n");
            return;
        }
        int port = Integer.parseInt(args[0]);
        new Server().start(port);
    }
}
