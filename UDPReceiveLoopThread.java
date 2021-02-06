import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;

/**
 * @author xxkun
 * @creed Awaken the Giant Within
 * @description: UDP receive thread
 * @date 2021-02-05 15:23
 */
public class UDPReceiveLoopThread extends Thread {
    private final DatagramSocket socket;
    private final OnMessage onMessage;

    public UDPReceiveLoopThread(DatagramSocket socket, OnMessage onMessage) {
        this.socket = socket;
        this.onMessage = onMessage;
    }

    @Override
    public void run() {
        while (true) {
            byte[] inBuff = new byte[Message.UDP_MSG_IN_BUFF_LEN];
            DatagramPacket packet = new DatagramPacket(inBuff, inBuff.length);
            try {
                socket.receive(packet);
            } catch (IOException e) {
//                e.printStackTrace();
                break;
            }
            String inDataStr = new String(packet.getData());
            Message msg = Message.msgUnpack(inDataStr);
            if (msg == null) {
                System.out.println("Invalid message from " + packet.getSocketAddress() + ":" + inDataStr);
                continue;
            }
            onMessage.onMessage(packet.getSocketAddress(), msg);
        }
    }

    public interface OnMessage {
        void onMessage(SocketAddress from, Message msg);
    }
}
