import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
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
public class Server {

    private final int HEARTBEAT_INTERVAL = 10 * 1000;   // Heartbeat interval (ms)

    private Map<String, ClientInfo> clientPool;
    private boolean stop = false;
    private DatagramSocket server;

    public void start(int port) {
        clientPool = new HashMap<>();
        try {
            server = new DatagramSocket(port);
            System.out.println("Server start on " + server.getLocalSocketAddress());
            new UDPReceiveLoopThread().start();
            new HeartbeatThread().start();
        } catch (SocketException e) {
            e.printStackTrace();
            stop = true;
        }
    }

    private void onMessage(SocketAddress from, Message msg) {
//        System.out.println("RECV FROM Client " + ClientInfo.getAddressStr(from) + ":" + msg.getType().getCode() + " -> " + msg.getBody().trim());
        switch (msg.getType()) {
            case MSGT_LOGIN:
                if (!clientPool.containsKey(ClientInfo.getAddressStr(from))) {
                    clientPool.put(ClientInfo.getAddressStr(from), new ClientInfo(from));
                    System.out.println("Client " + ClientInfo.getAddressStr(from) + " logged in");
                    TransferUtil.udpSendText(server, from, Message.MessageType.MSGT_REPLY, "Login success!");
                } else {
                    System.out.println("Client " + ClientInfo.getAddressStr(from) + " failed to login");
                    TransferUtil.udpSendText(server, from, Message.MessageType.MSGT_REPLY, "Login failed");
                }
                break;
            case MSGT_LOGOUT:
                if (clientPool.remove(ClientInfo.getAddressStr(from)) != null) {
                    System.out.println("Client " + ClientInfo.getAddressStr(from) + " logged out");
                    TransferUtil.udpSendText(server, from, Message.MessageType.MSGT_REPLY, "Logout success");
                } else {
                    System.out.println("Client " + ClientInfo.getAddressStr(from) + " failed to logout");
                    TransferUtil.udpSendText(server, from, Message.MessageType.MSGT_REPLY, "Logout failed");
                }
                break;
            case MSGT_LIST:
                System.out.println("Client " + ClientInfo.getAddressStr(from) + " query list ");
                String fromStr = ClientInfo.getAddressStr(from);
                String text = clientPool.keySet().stream().filter(s -> !s.equals(fromStr)).collect(Collectors.joining(","));
                TransferUtil.udpSendText(server, from, Message.MessageType.MSGT_REPLY, "[" + text + "]");
                break;
            case MSGT_PUNCH:
                SocketAddress other = TransferUtil.getSocketAddressFromString(msg.getBody());
                if (other == null) {
                    System.out.println("Address format error");
                    break;
                }
                System.out.println("Client " + ClientInfo.getAddressStr(from) + " punching to " + ClientInfo.getAddressStr(other));
                TransferUtil.udpSendText(server, other, Message.MessageType.MSGT_PUNCH, ClientInfo.getAddressStr(from));
                TransferUtil.udpSendText(server, from, Message.MessageType.MSGT_TEXT, "punch request sent");
                break;
            case MSGT_HEARTBEAT:
                clientPool.get(ClientInfo.getAddressStr(from)).setAlive(true);
                System.out.println("Client " + ClientInfo.getAddressStr(from) + " alive");
                break;
            default:
                TransferUtil.udpSendText(server, from, Message.MessageType.MSGT_REPLY, "Unknown command");
                break;
        }
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
                while (iterator.hasNext()) {
                    ClientInfo client = iterator.next();
                    if (client.isAlive()) {
                        TransferUtil.udpSendText(server, client.getSocketAddress(), Message.MessageType.MSGT_HEARTBEAT, null);
                        client.setAlive(false);
                    } else {
                        System.out.println("Client " + ClientInfo.getAddressStr(client.getSocketAddress()) + " logout");
                        iterator.remove();
                    }
                }
            }
        }
    }

    private class UDPReceiveLoopThread extends Thread {
        @Override
        public void run() {
            while (!stop) {
                byte[] inBuff = new byte[Message.UDP_MSG_IN_BUFF_LEN];
                DatagramPacket packet = new DatagramPacket(inBuff, inBuff.length);
                try {
                    server.receive(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                    stop = true;
                    break;
                }
                String inDataStr = new String(packet.getData());
                Message msg = Message.msgUnpack(inDataStr);
                if (msg == null) {
                    System.out.println("Invalid message from Client " + ClientInfo.getAddressStr(packet.getSocketAddress()) + ":" + inDataStr);
                    continue;
                }
                onMessage(packet.getSocketAddress(), msg);
            }
        }
    }

    private static class ClientInfo {
        private final SocketAddress socketAddress;
        private boolean alive;

        public ClientInfo(SocketAddress address) {
            socketAddress = address;
            alive = true;
        }

        public SocketAddress getSocketAddress() {
            return socketAddress;
        }

        public boolean isAlive() {
            return alive;
        }

        public void setAlive(boolean alive) {
            this.alive = alive;
        }

        public static String getAddressStr(SocketAddress address) {
            return address.toString().substring(1);
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
