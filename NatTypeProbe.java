import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;

/**
 * @author xxkun
 * @creed Awaken the Giant Within
 * @description: Net type probe
 * @date 2021-02-05 11:56
 */
public class NatTypeProbe implements UDPReceiveLoopThread.Status {

    private DatagramSocket local;
    private DatagramSocket local2;
    private SocketAddress server;
    private boolean stop = false;

    public void start(String address) {
        server = TransferUtil.getSocketAddressFromString(address);
        try {
            local = new DatagramSocket();
            local2 = new DatagramSocket();
            new UDPReceiveLoopThread(local, this).start();
            new UDPReceiveLoopThread(local2, this).start();
            TransferUtil.udpSendMsg(local, server, Message.MessageType.NPT_START, null);
        } catch (SocketException e) {
            e.printStackTrace();
            stop = true;
        }
    }

    public void onMessage(SocketAddress from, Message msg) {
//        System.out.println("RECV FROM Client " + ClientInfo.getAddressStr(from) + ":" + msg.getType().getCode() + " -> " + msg.getBody().trim());
        switch (msg.getType()) {
            case NPT_STEP_1:
                break;
            case NPT_STEP_2:
                break;
            case NPT_STEP_3:
                break;
            case NPT_SYMMETRIC_NAT:
                break;
            case NPT_FULL_CONE_NAT:
                break;
            case NPT_FULL_OR_RESTRICTED_CONE_NAT:
                break;
            default:
                System.out.println("Unknown message: " + msg.toString());
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
