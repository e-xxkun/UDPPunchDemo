import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;

/**
 * @author xxkun
 * @creed Awaken the Giant Within
 * @description: Net type probe server
 * @date 2021-02-05 11:59
 */
public class NatTypeProbeServer {

    public static final String NPT_SPLIT = "#";

    private DatagramSocket server;
    private DatagramSocket restrictedConeServer ;
    private DatagramSocket fullConeServer;

    public void start(int port) {
        try {
            server = new DatagramSocket(port);
            restrictedConeServer = new DatagramSocket(8886);
            fullConeServer = new DatagramSocket(8887);
//            restrictedConeServer = new DatagramSocket();
//            fullConeServer = new DatagramSocket();
            System.out.println("Net type probe server start on " + server.getLocalSocketAddress());
        } catch (SocketException e) {
            e.printStackTrace();
            return;
        }
        new UDPReceiveLoopThread(server, this::onMessage).start();
        new UDPReceiveLoopThread(restrictedConeServer, this::onMessage).start();
        new UDPReceiveLoopThread(fullConeServer, this::onMessage).start();
    }

    public void onMessage(SocketAddress from, Message msg) {
        System.out.println("RECV FROM Client " + from + ": " + msg.getType().getCode() + " -> " + msg.getBody().trim());
        switch (msg.getType()) {
            case NPT_START:
                TransferUtil.udpSendMsg(server, from, Message.MessageType.NPT_STEP_1, from.toString().substring(1) + NPT_SPLIT + restrictedConeServer.getLocalPort());
                break;
            case NPT_STEP_1:
                if (msg.getBody() != null) {
                    if (msg.getBody().equals(from.toString().substring(1))) {
                        TransferUtil.udpSendMsg(fullConeServer, from, Message.MessageType.NPT_FULL_OR_RESTRICTED_CONE_NAT, null);
                        TransferUtil.udpSendMsg(server, from, Message.MessageType.NPT_STEP_2, null);
                    } else {
                        TransferUtil.udpSendMsg(server, from, Message.MessageType.NPT_SYMMETRIC_NAT, null);
                    }
                }
                break;
            default:
                System.out.println("Unknown message from " + from + ": " + msg.toString());
                break;
        }
    }

    public void stop() {
        server.close();
        fullConeServer.close();
        restrictedConeServer.close();
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java " + Server.class.getName() + " <port>\n");
            return;
        }
        new NatTypeProbeServer().start(Integer.parseInt(args[0]));
    }
}
