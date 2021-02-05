import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;

/**
 * @author xxkun
 * @creed Awaken the Giant Within
 * @description: Net type probe server
 * @date 2021-02-05 11:59
 */
public class NatTypeProbeServer implements UDPReceiveLoopThread.Status {

    private DatagramSocket server;
    private DatagramSocket restrictedConeServer ;
    private DatagramSocket fullConeServer;
    private boolean stop = false;

    public void start(int port) {
        try {
            server = new DatagramSocket(port);
            restrictedConeServer = new DatagramSocket();
            fullConeServer = new DatagramSocket();
            System.out.println("Net type probe server start on " + server.getLocalSocketAddress());
            new UDPReceiveLoopThread(server, this).start();
            new UDPReceiveLoopThread(restrictedConeServer, this).start();
            new UDPReceiveLoopThread(fullConeServer, this).start();
        } catch (SocketException e) {
            e.printStackTrace();
            stop = true;
        }
    }

    public void onMessage(SocketAddress from, Message msg) {
//        System.out.println("RECV FROM Client " + ClientInfo.getAddressStr(from) + ":" + msg.getType().getCode() + " -> " + msg.getBody().trim());
        switch (msg.getType()) {
            case NPT_START:
                break;
            case NPT_STEP_1:
                break;
            case NPT_STEP_2:
                break;
            case NPT_STEP_3:
                break;
            default:
                System.out.println("Unknown message from " + from + ": " + msg.toString());
                break;
        }
    }

    @Override
    public boolean isStop() {
        return stop;
    }

    public static void main(String[] args) {

    }
}
