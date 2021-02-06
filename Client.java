import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * @author xxkun
 * @creed Awaken the Giant Within
 * @description: P2P Client
 * @date 2021-01-30 20:02
 */

public class Client {

    private static final long HEARTBEAT_INTERVAL = 10 * 1000;
    private Map<String, SocketAddress> clientList;
    private SocketAddress server;
    private DatagramSocket local;
    private HeartbeatThread heartbeat;

    public void start(String address) {
        server = TransferUtil.getSocketAddressFromString(address);
        clientList = new HashMap<>();
        try {
            local = new DatagramSocket();
            System.out.println("UDP start on " + local.getLocalSocketAddress());
        } catch (SocketException e) {
            e.printStackTrace();
            return;
        }
        heartbeat = new HeartbeatThread();
        new UDPReceiveLoopThread(local, this::onMessage).start();
        new ConsoleThread().start();
    }

    private void onMessage(SocketAddress from, Message msg) {
//        System.out.println("RECV FROM Client " + from + ":" + msg.getType().getCode() + " -> " + msg.getBody());
        if (server.toString().equals(from.toString())) {
            switch (msg.getType()) {
                case MSGT_PUNCH:
                    SocketAddress peer = TransferUtil.getSocketAddressFromString(msg.getBody());
                    System.out.println("Client " + peer + " on call, replying...");
                    TransferUtil.udpSendMsg(local, peer, Message.MessageType.MSGT_REPLY, null);
                    break;
                case MSGT_REPLY:
                    System.out.println("Server " + server + ": " + msg.getBody());
                    break;
                case MSGT_LOGIN:
                    System.out.println("Login success!");
                    if (!heartbeat.isAlive()) {
                        heartbeat.start();
                    }
                    break;
            }
            return;
        }
        switch (msg.getType()) {
            case MSGT_TEXT:
                System.out.println("Peer " + from + ": " + msg.getBody());
                break;
            case MSGT_REPLY:
                System.out.println("Peer " + from + " replied, you can talk now" );
                clientList.put(from.toString(), from);
                TransferUtil.udpSendMsg(local, from, Message.MessageType.MSGT_TEXT, "connect success");
                break;
            case MSGT_PUNCH:
                TransferUtil.udpSendMsg(local, from, Message.MessageType.MSGT_TEXT, "I see you");
                break;
            default:
                break;
        }
    }

    private void stop() {
        if (heartbeat.isAlive()) {
            heartbeat.interrupt();
        }
        local.close();
    }

    public class ConsoleThread extends Thread {
        @Override
        public void run() {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String[] cmd = scanner.nextLine().split(" ");
                switch (cmd[0]) {
                    case "list":
                        TransferUtil.udpSendMsg(local, server, Message.MessageType.MSGT_LIST, null);
                        break;
                    case "peer":
                        System.out.println(clientList.values().toString());
                        break;
                    case "login":
                        TransferUtil.udpSendMsg(local, server, Message.MessageType.MSGT_LOGIN, null);
                        break;
                    case "logout":
                        TransferUtil.udpSendMsg(local, server, Message.MessageType.MSGT_LOGOUT, null);
                        heartbeat.interrupt();
                        break;
                    case "send":
                        if (cmd.length > 2) {
                            TransferUtil.udpSendMsg(local, TransferUtil.getSocketAddressFromString(cmd[1]), Message.MessageType.MSGT_TEXT, cmd[2]);
                        } else {
                            System.out.println("Parameter error");
                        }
                        break;
                    case "punch":
                        if (cmd.length > 1) {
                            SocketAddress peer = TransferUtil.getSocketAddressFromString(cmd[1]);
                            if (peer == null) {
                                System.out.println("Address format error");
                                break;
                            }
                            System.out.println("punching " + peer);
                            TransferUtil.udpSendMsg(local, peer, Message.MessageType.MSGT_PUNCH, null);
                            TransferUtil.udpSendMsg(local, server, Message.MessageType.MSGT_PUNCH, cmd[1]);
                        } else {
                            System.out.println("Parameter error");
                        }
                        break;
                    case "quit":
                        TransferUtil.udpSendMsg(local, server, Message.MessageType.MSGT_LOGOUT, null);
                        Client.this.stop();
                        return;
                    case "help":
                        print_help();
                        break;
                    default:
                        System.out.println("Unknown command");
                }
            }
        }
    }

    private class HeartbeatThread extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    sleep(HEARTBEAT_INTERVAL);
                    TransferUtil.udpSendMsg(local, server, Message.MessageType.MSGT_HEARTBEAT, null);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    }

    public static void main(String[] args) {
        String address;
        if (args.length == 1) {
            address = args[0];
        } else {
            System.out.println("Usage: java " + Client.class.getName() + " <host>:<port>");
            return;
        }
        new Client().start(address);
    }

    public static void print_help() {
        String help_message =
            "Usage:" +
            "\n\n login" +
            "\n     login to server so that other peer(s) can see you" +
            "\n\n logout" +
            "\n     logout from server" +
            "\n\n list" +
            "\n     list logined peers" +
            "\n\n peer" +
            "\n     list punched peers" +
            "\n\n punch host:port" +
            "\n     punch a hole through UDP to [host:port]" +
            "\n     host:port must have been logged in to server" +
            "\n     Example:" +
            "\n     >>> punch 9.8.8.8:53" +
            "\n\n send host:port data" +
            "\n     send [data] to peer [host:port] through UDP protocol" +
            "\n     the other peer could receive your message if UDP hole punching succeed" +
            "\n     Example:" +
            "\n     >>> send 8.8.8.8:53 hello" +
            "\n\n help" +
            "\n     print this help message" +
            "\n\n quit" +
            "\n     logout and quit this program";
        System.out.println(help_message);
    }
}
