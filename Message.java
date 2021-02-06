/**
 * @author xxkun
 * @creed Awaken the Giant Within
 * @description: UDP Message
 * @date 2021-01-30 16:50
 */

public class Message {

    public static final int UDP_MSG_IN_BUFF_LEN = 1024;
    private static final String MSG_HEADER = "UDP";
    private static final String MSG_SPLIT = "-";

    private final String body;
    private final MessageType type;

    public Message(String body, MessageType type) {
        this.body = body;
        this.type = type;
    }

    public static Message msgUnpack(String msgStr) {
        String[] pSplit = msgStr.split(MSG_SPLIT);
        if (pSplit.length >= 2) {
            String header = pSplit[0];
            if (MSG_HEADER.equals(header)) {
                MessageType type = MessageType.create(Integer.parseInt(pSplit[1]));
                String body = null;
                if (pSplit.length > 2) {
                    body = pSplit[2];
                }
                return new Message(body, type);
            }
        }
        return null;
    }

    public String getBody() {
        return body;
    }

    public MessageType getType() {
        return type;
    }

    @Override
    public String toString() {
        return MSG_HEADER + MSG_SPLIT + type.getCode() + MSG_SPLIT + (body == null ? "" : body + MSG_SPLIT);
    }

    public enum MessageType {

        MSGT_LOGIN(0),
        MSGT_LOGOUT(1),
        MSGT_LIST(2),
        MSGT_PUNCH(3),
        MSGT_HEARTBEAT(4),
        MSGT_REPLY(5),
        MSGT_TEXT(6),
        MSGT_UNKNOWN(7),


        NPT_FULL_OR_RESTRICTED_CONE_NAT(8),
        NPT_SYMMETRIC_NAT(9),
        NPT_START(10),
        NPT_STEP_1(11),
        NPT_STEP_2(12);

        int code;

        MessageType(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static MessageType create(int val) {
            MessageType[] types = MessageType.values();
            for (MessageType type : types) {
                if (type.getCode() == val) {
                    return type;
                }
            }
            return MSGT_UNKNOWN;
        }
    }
}
