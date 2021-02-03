import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;

/**
 * @author xxkun
 * @creed Awaken the Giant Within
 * @description: P2P客户端
 * @date 2021-01-30 20:02
 */
public class Client {

    private static final byte[] UDP_BUF_TMP = new byte[1];
    public static Map<String, DatagramPacket> clientList;
    public static DatagramPacket server;
    public static DatagramSocket client;

    public static boolean quit = false;

    public static void main(String[] args) {

//        String host = "106.14.249.225";
        String host = "127.0.0.1";
        int port = 8888;
        byte[] buf = new byte[1024];
        SocketAddress receiveAddress = new InetSocketAddress(host, port);
        server = new DatagramPacket(buf, buf.length, receiveAddress);
        clientList = new HashMap<>();

        try {
            client = new DatagramSocket();
            System.out.println("UDP start");

        } catch (SocketException e) {
            e.printStackTrace();
        }

        new HeartbeatThread().start();
        new ReceiveThread().start();
        new ConsoleThread().start();
    }

    public static class HeartbeatThread extends Thread {

        @Override
        public void run() {
            Message ping = new Message();
            ping.head.magic = Message.MSG_MAGIC;
            ping.head.type = Message.MessageType.MTYPE_PING;
            ping.body = null;
            while (!quit) {
                try {
                    sleep(10 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Message.udpSendMsg(client, server, ping);
                for (DatagramPacket peer : clientList.values()) {
                    Message.udpSendMsg(client, peer, ping);
                }
            }
            System.out.println("HeartbeatThread quit");
        }
    }

    public static class ReceiveThread extends Thread {


        private static void onMessage(DatagramPacket from, Message msg) {
//            System.out.println("RECV FROM " + from.getSocketAddress() + ":" + msg.head.type.getCode() + " -> " + msg.body);
            if (server.getSocketAddress().toString().equals(from.getSocketAddress().toString())) {
                switch (msg.head.type) {
                    case MTYPE_PUNCH:
                        DatagramPacket peer = dataPacketFromString(msg.body);
                        System.out.println(peer.getSocketAddress() + " on call, replying...");
                        Message.udpSendText(client, peer, Message.MessageType.MTYPE_REPLY, null);
                        break;
                    case MTYPE_REPLY:
                        System.out.println("SERVER: " + msg.body);
                        break;
                }
                return;
            }
            switch (msg.head.type) {
                case MTYPE_TEXT:
                    System.out.println("Peer " + from.getSocketAddress() + ": " + msg.body);
                    break;
                case MTYPE_REPLY:
                    System.out.println("Peer replied, you can talk now" + from.getSocketAddress());
                    clientList.put(from.getSocketAddress().toString(), from);
                    break;
                case MTYPE_PUNCH:
                    Message.udpSendText(client, from, Message.MessageType.MTYPE_TEXT, "I SEE YOU");
                    break;
                case MTYPE_PING:
                    Message.udpSendText(client, from, Message.MessageType.MTYPE_PONG, null);
                    break;
                default:
                    break;
            }
        }

        @Override
        public void run() {
            while (!quit) {
                try {
                    byte[] buf = new byte[1024];
                    DatagramPacket peer = new DatagramPacket(buf, buf.length);;
                    client.receive(peer);
                    Message msg = Message.msgUnpack(peer.getData());
                    if (!msg.head.magic.equals(Message.MSG_MAGIC) || msg.body == null) {
                        System.out.println("Invalid message" + msg.head.magic + msg.head.type + msg.body);
                        continue;
                    }
                    onMessage(peer, msg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("ReceiveThread quit");
        }
    }

    public static class ConsoleThread extends Thread {
        @Override
        public void run() {
            Scanner scanner = new Scanner(System.in);
            while (!quit) {
                String[] line = scanner.nextLine().split(" ");
                switch (line[0]) {
                    case "list":
                        Message.udpSendText(client, server, Message.MessageType.MTYPE_LIST, null);
                        break;
                    case "login":
                        Message.udpSendText(client, server, Message.MessageType.MTYPE_LOGIN, null);
                        break;
                    case "logout":
                        Message.udpSendText(client, server, Message.MessageType.MTYPE_LOGOUT, null);
                        break;
                    case "punch":
                        DatagramPacket peer = dataPacketFromString(line[1]);
                        System.out.println("punching " + peer.getSocketAddress());
                        Message.udpSendText(client, peer, Message.MessageType.MTYPE_PUNCH, null);
                        Message.udpSendText(client, server, Message.MessageType.MTYPE_PUNCH, line[1]);
                        break;
                    case "send":
                        Message.udpSendText(client, dataPacketFromString(line[1]), Message.MessageType.MTYPE_TEXT, line[2]);
                        break;
                    case "quit":
                        Message.udpSendText(client, server, Message.MessageType.MTYPE_LOGOUT, null);
                        quit = true;
                        break;
                    default:
                        System.out.println("Unknown command");
                }
            }
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
}
