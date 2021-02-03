import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * @author xxkun
 * @creed Awaken the Giant Within
 * @description: UDP消息
 * @date 2021-01-30 16:50
 */
public class Message {

    public MessageHead head = new MessageHead();
    public String body;

    public static final String MSG_MAGIC = "XXK";

    public static Message msgUnpack(byte[] buf) {
        Message m = new Message();
        String inData = new String(buf);
        String[] pSplit = inData.split("-");
        m.head.magic = pSplit[0];
        if (!m.head.magic.equals(MSG_MAGIC)) {
            return m;
        }
        m.head.type = MessageType.create(pSplit[1]);
        m.body = pSplit[2];
        return m;
    }

    public static void udpSendText(DatagramSocket sock, DatagramPacket peer, MessageType type, String text) {
        Message m = new Message();
        m.head.magic = MSG_MAGIC;
        m.head.type = type;
        m.body = text == null ? "NULL" : text;
        udpSendMsg(sock, peer, m);
    }

    public static void udpSendMsg(DatagramSocket sock, DatagramPacket peer, Message m) {
        peer.setData(m.toString().getBytes());
        try {
            sock.send(peer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return head.magic + "-" + head.type.getCode() + "-" + body + "-";
    }

    public class MessageHead {
        public String magic;
        public MessageType type;
    }

    public enum MessageType {
        MTYPE_LOGIN("LOGIN"),
        MTYPE_LOGOUT("LOGOUT"),
        MTYPE_LIST("LIST"),
        MTYPE_PUNCH("PUNCH"),
        MTYPE_PING("PING"),
        MTYPE_PONG("PONG"),
        MTYPE_REPLY("REPLY"),
        MTYPE_TEXT("TEXT"),
        MTYPE_UNKNOW("UNKNOW"),
        MTYPE_END("END");

        String code;

        MessageType(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        public static MessageType create(String val) {
            MessageType[] units = MessageType.values();
            for (MessageType unit : units) {
                if (unit.getCode().equals(val)) {
                    return unit;
                }
            }
            return MTYPE_UNKNOW;
        }
    }
}
