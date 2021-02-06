import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;

/**
 * @author xxkun
 * @creed Awaken the Giant Within
 * @description: Net type probe
 * @date 2021-02-05 11:56
 */
public class NatTypeProbe {

    private final static long WAIT_TIME = 2 * 1000;

    private DatagramSocket local;

    private WaitThread prcnWaitTread;
    private WaitThread connWaitTread;

    public void start(String address) {
        try {
            local = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
            return;
        }
        prcnWaitTread = new WaitThread("Port restricted cone Nat");
        connWaitTread = new WaitThread("Connect Fail");
        new UDPReceiveLoopThread(local, this::onMessage).start();
        SocketAddress server = TransferUtil.getSocketAddressFromString(address);
        TransferUtil.udpSendMsg(local, server, Message.MessageType.NPT_START, null);
        connWaitTread.start();
    }

    private void onMessage(SocketAddress from, Message msg) {
//        System.out.println("RECV FROM Client " + from + ": " + msg.getType().getCode() + " -> " + msg.getBody().trim());
        switch (msg.getType()) {
            case NPT_STEP_1:
                connWaitTread.interrupt();
                String[] pSplit = msg.getBody().split(NatTypeProbeServer.NPT_SPLIT);
                if (pSplit.length == 2) {
                    String host = ((InetSocketAddress)from).getHostString();
                    int port = Integer.parseInt(pSplit[1]);
                    SocketAddress server2 = new InetSocketAddress(host, port);
                    TransferUtil.udpSendMsg(local, server2, Message.MessageType.NPT_STEP_1, pSplit[0]);
                }
                break;
            case NPT_STEP_2:
                prcnWaitTread.start();
                break;
            case NPT_SYMMETRIC_NAT:
                System.out.println("Symmetric Nat");
                stop();
                break;
            case NPT_FULL_OR_RESTRICTED_CONE_NAT:
                prcnWaitTread.close();
                prcnWaitTread.interrupt();
                System.out.println("Full Nat OR Restricted cone Nat");
                stop();
                break;
            default:
                System.out.println("Unknown message: " + msg.toString());
                break;
        }
    }

    public void stop() {
        if (prcnWaitTread.isAlive()) {
            prcnWaitTread.interrupt();
        }
        if (connWaitTread.isAlive()) {
            connWaitTread.interrupt();
        }
        local.close();
    }

    public class WaitThread extends Thread {

        private final String natType;
        private boolean stop = false;

        public WaitThread(String natType) {
            this.natType = natType;
        }

        public void close() {
            stop = true;
        }

        @Override
        public void run() {
            if (!stop) {
                try {
                    sleep(WAIT_TIME);
                    System.out.println(natType);
                    NatTypeProbe.this.stop();
                } catch (InterruptedException ignored) {

                }
            }
        }
    }


    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java " + Server.class.getName() + " <host>:<port>\n");
            return;
        }
        new NatTypeProbe().start(args[0]);
    }
}
