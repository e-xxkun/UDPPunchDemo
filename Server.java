import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author xxkun
 * @creed Awaken the Giant Within
 * @description: 中转服务器
 * @date 2021-01-30 10:23
 */
public class Server {

    private static final int UDP_MSG_INBUFF_LEN = 10240;
    private static final byte[] UDP_BUF_TMP = new byte[1];
    public static Map<String, DatagramPacket> clientPool;

    public static void UDPReceiveLoop(DatagramSocket sock) throws IOException {
        while (true) {
            byte[] inBuff = new byte[UDP_MSG_INBUFF_LEN];
            DatagramPacket inPacket = new DatagramPacket(inBuff, inBuff.length);
            sock.receive(inPacket);
//            System.out.println("UDP Check " + new Date().toString());

            Message msg = Message.msgUnpack(inPacket.getData());
            if (!msg.head.magic.equals(Message.MSG_MAGIC) || msg.body == null) {
                System.out.println("Invalid message: " + msg.head.magic + msg.head.type + msg.body);
                continue;
            }
            onMessage(sock, inPacket, msg);
        }
    }

    private static void onMessage(DatagramSocket sock, DatagramPacket from, Message msg) {
        System.out.println("RECV FROM " + from.getSocketAddress() + ":" + msg.head.type.getCode() + " -> " + msg.body);
        switch (msg.head.type) {
            case MTYPE_LOGIN:
                if (!clientPool.containsKey(from.getSocketAddress())) {
                    System.out.println("logged in " + from.getSocketAddress());
                    Message.udpSendText(sock, from, Message.MessageType.MTYPE_REPLY, "Login success!");
                } else {
                    System.out.println("failed to login " + from.getSocketAddress());
                    Message.udpSendText(sock, from, Message.MessageType.MTYPE_REPLY, "Login failed");
                }
                break;
            case MTYPE_LOGOUT:
                if (clientPool.remove(from.getSocketAddress()) != null) {
                    System.out.println("logged out " + from.getSocketAddress());
                    Message.udpSendText(sock, from, Message.MessageType.MTYPE_REPLY, "Logout success");
                } else {
                    System.out.println("failed to logout " + from.getSocketAddress());
                    Message.udpSendText(sock, from, Message.MessageType.MTYPE_REPLY, "Logout failed");
                }
                break;
            case MTYPE_LIST:
                System.out.println("quering list " + from.getSocketAddress());
                String text = clientPool.keySet().toString();
                Message.udpSendText(sock, from, Message.MessageType.MTYPE_REPLY, text);
                break;
            case MTYPE_PUNCH:
                DatagramPacket other = dataPacketFromString(msg.body);
                System.out.println("punching to " + other.getSocketAddress());
                Message.udpSendText(sock, other, Message.MessageType.MTYPE_PUNCH, from.getSocketAddress().toString().substring(1));
                Message.udpSendText(sock, from, Message.MessageType.MTYPE_TEXT, "punch request sent");
                break;
            case MTYPE_PING:
                Message.udpSendText(sock, from, Message.MessageType.MTYPE_PONG, null);
                break;
            case MTYPE_PONG:
                break;
            default:
                Message.udpSendText(sock, from, Message.MessageType.MTYPE_REPLY, "Unkown command");
                break;
        }
    }

    public static DatagramPacket dataPacketFromString(String body) {
        String[] pSplit = body.split(":");
        String host = pSplit[0];
        int port = Integer.valueOf(pSplit[1]);
        SocketAddress receiveAddress = new InetSocketAddress(host, port);
        byte[] buf = UDP_BUF_TMP;
        return new DatagramPacket(buf, buf.length, receiveAddress);
    }

    public static void main(String[] args) {
        int port = 8888;
        DatagramSocket server;
        clientPool = new HashMap<>();
        try {
            server = new DatagramSocket(port);
            System.out.println("Server start on " + server.getLocalSocketAddress());
            UDPReceiveLoop(server);
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        clientPoolDestroy(clientPool);
    }

    private static void clientPoolDestroy(Map<String, DatagramPacket> clientPool) {
    }
}
