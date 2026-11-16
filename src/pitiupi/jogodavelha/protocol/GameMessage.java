package pitiupi.jogodavelha.protocol;
import pitiupi.models.Messages.Message;

public final class GameMessage extends Message {
    private static final long serialVersionUID = 1L;
    public static final int VERSION = 1;

    public enum Kind { INVITE, ACCEPT, DECLINE, MOVE, RESIGN }

    private int version;
    private Kind kind;
    private String gameId;
    private String senderId;
    private String senderName;
    private String senderIp;
    private int turn;
    private int cell;

    public GameMessage() { super(); }
    public GameMessage(Kind kind, String gameId, String senderId,
                       String senderName, String senderIp, int turn, int cell) {
        super();
        this.version = VERSION;
        this.kind = kind;
        this.gameId = gameId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.senderIp = senderIp;
        this.turn = turn;
        this.cell = cell;
    }

    public int getVersion() { return version; }
    public Kind getKind() { return kind; }
    public String getGameId() { return gameId; }
    public String getSenderId() { return senderId; }
    public String getSenderName() { return senderName; }
    public String getSenderIp() { return senderIp; }
    public int getTurn() { return turn; }
    public int getCell() { return cell; }
}
