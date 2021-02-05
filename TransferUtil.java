import com.sun.istack.internal.NotNull;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * @author xxkun
 * @creed Awaken the Giant Within
 * @description: UDP Transfer Utils
 * @date 2021-01-30 20:25
 */
public class TransferUtil {

    public static void udpSendMsg(DatagramSocket socket, SocketAddress address, Message.MessageType type, String text) {
        Message msg = new Message(text, type);
        byte[] buff = msg.toString().getBytes();
        DatagramPacket packet = new DatagramPacket(buff, buff.length, address);
        try {
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static SocketAddress getSocketAddressFromString(String body) {
        String[] pSplit = body.split(":");
        if (pSplit.length == 2) {
            String host = pSplit[0];
            int port = Integer.parseInt(pSplit[1]);
            return new InetSocketAddress(host, port);
        }
        return null;
    }
}
