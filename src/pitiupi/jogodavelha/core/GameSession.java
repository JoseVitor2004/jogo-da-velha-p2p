package pitiupi.jogodavelha.core;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.UUID;
import pitiupi.jogodavelha.protocol.GameMessage;
import pitiupi.jogodavelha.protocol.GameMessage.Kind;

public final class GameSession {
    public enum Phase { IDLE, OUTGOING, INCOMING, PLAYING, FINISHED }

    public static final class Inbound {
        public final boolean requestConfirmation;
        public final GameMessage automaticReply;
        public final boolean handled;

        private Inbound(boolean confirmation, GameMessage reply, boolean handled) {
            this.requestConfirmation = confirmation;
            this.automaticReply = reply;
            this.handled = handled;
        }
        public static Inbound ignored() { return new Inbound(false, null, false); }
        public static Inbound changed() { return new Inbound(false, null, true); }
        public static Inbound prompt() { return new Inbound(true, null, true); }
        public static Inbound reply(GameMessage msg) { return new Inbound(false, msg, true); }
    }

    private final String localId = UUID.randomUUID().toString();
    private final char[] board = new char[9];
    private Phase phase = Phase.IDLE;
    private String gameId;
    private String opponentId;
    private String opponentIp;
    private String opponentName;
    private String localIp;
    private String localName;
    private char ownMark;
    private int moves;
    private String status = "Pronto. Informe o IPv4 do adversário e clique em Convidar.";

    public GameSession() { Arrays.fill(board, ' '); }

    public String localId() { return localId; }
    public Phase phase() { return phase; }
    public String gameId() { return gameId; }
    public String opponentName() { return opponentName; }
    public String opponentIp() { return opponentIp; }
    public String status() { return status; }
    public char ownMark() { return ownMark; }
    public int moves() { return moves; }
    public char cell(int i) { return board[i]; }
    public boolean canInvite() { return phase == Phase.IDLE || phase == Phase.FINISHED; }
    public boolean canRespond() { return phase == Phase.INCOMING; }
    public boolean canEnd() { return phase == Phase.INCOMING || phase == Phase.OUTGOING || phase == Phase.PLAYING; }
    public char nextMark() { return moves % 2 == 0 ? 'X' : 'O'; }
    public boolean myTurn() { return phase == Phase.PLAYING && nextMark() == ownMark; }
    public boolean canPlay(int cell) { return myTurn() && cell >= 0 && cell < 9 && board[cell] == ' '; }

    /** Valida IPv4 numérico (não faz DNS nem permite multicast/0.0.0.0). */
    public static boolean isValidIpv4(String text) {
        if (text == null || !text.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}")) return false;
        String[] octets = text.split("\\.");
        for (String part : octets) {
            if (Integer.parseInt(part) > 255) return false;
        }
        try {
            InetAddress a = InetAddress.getByName(text);
            return a instanceof Inet4Address && !a.isAnyLocalAddress()
                    && !a.isMulticastAddress() && !"255.255.255.255".equals(text);
        } catch (Exception ex) { return false; }
    }

    private static void requireIdentity(String name, String ip) {
        if (name == null || name.isBlank() || name.length() > 40 || !isValidIpv4(ip)) {
            throw new IllegalArgumentException("Nome ou IP local inválido. Verifique a interface de rede do Pitiupi.");
        }
    }

    public void configureLocal(String name, String ip) {
        requireIdentity(name, ip);
        localName = name;
        localIp = ip;
    }

    private void reset(String name, String ip) {
        configureLocal(name, ip);
        Arrays.fill(board, ' ');
        moves = 0;
    }

    public GameMessage invite(String peerIp, String name, String ip) {
        if (!canInvite()) throw new IllegalStateException("Encerre a partida atual primeiro.");
        if (!isValidIpv4(peerIp)) throw new IllegalArgumentException("Informe um endereço IPv4 válido.");
        reset(name, ip);
        phase = Phase.OUTGOING;
        gameId = UUID.randomUUID().toString();
        opponentId = null; // só é conhecido quando o destinatário responde.
        opponentIp = peerIp;
        opponentName = peerIp;
        ownMark = 'X';
        status = "Convite enviado para " + peerIp + ". Aguardando resposta...";
        return message(Kind.INVITE, 0, -1);
    }

    public Inbound receive(GameMessage msg) {
        if (!validEnvelope(msg) || localId.equals(msg.getSenderId())) return Inbound.ignored();
        if (msg.getKind() == Kind.INVITE) {
            if (!canInvite()) {
                // Recusa automaticamente enquanto outra partida/convite estiver ativo.
                return Inbound.reply(new GameMessage(Kind.DECLINE, msg.getGameId(),
                        localId, safeLocalName(), safeLocalIp(), 0, -1));
            }
            // O receptor é 'O'; o criador sempre começa como 'X'.
            Arrays.fill(board, ' ');
            moves = 0;
            gameId = msg.getGameId();
            opponentId = msg.getSenderId();
            opponentIp = msg.getSenderIp();
            opponentName = msg.getSenderName();
            ownMark = 'O';
            phase = Phase.INCOMING;
            status = "Convite de " + opponentName + " (" + opponentIp + "). Aceitar?";
            return Inbound.prompt();
        }
        if (!match(msg)) return Inbound.ignored();
        switch (msg.getKind()) {
            case ACCEPT:
                if (phase != Phase.OUTGOING || msg.getTurn() != 0 || msg.getCell() != -1) return Inbound.ignored();
                opponentId = msg.getSenderId();
                opponentName = msg.getSenderName();
                phase = Phase.PLAYING;
                status = "Convite aceito por " + opponentName + ". Sua vez (X).";
                return Inbound.changed();
            case DECLINE:
                if (phase != Phase.OUTGOING || msg.getTurn() != 0 || msg.getCell() != -1) return Inbound.ignored();
                phase = Phase.FINISHED;
                status = "Convite recusado por " + opponentName + ".";
                return Inbound.changed();
            case MOVE:
                if (phase != Phase.PLAYING || !validRemoteMove(msg)) return Inbound.ignored();
                placeInternal(msg.getCell(), otherMark());
                return Inbound.changed();
            case RESIGN:
                if (phase != Phase.PLAYING && phase != Phase.OUTGOING) return Inbound.ignored();
                phase = Phase.FINISHED;
                status = "O adversário encerrou a partida.";
                return Inbound.changed();
            default:
                return Inbound.ignored();
        }
    }

    private String safeLocalName() { return localName == null ? "Jogador" : localName; }
    private String safeLocalIp() { return localIp == null ? "127.0.0.1" : localIp; }

    private boolean validEnvelope(GameMessage m) {
        if (m == null || m.getVersion() != GameMessage.VERSION || m.getKind() == null) return false;
        try {
            UUID.fromString(m.getGameId());
            UUID.fromString(m.getSenderId());
        } catch (Exception ex) { return false; }
        if (m.getSenderName() == null || m.getSenderName().isBlank()
                || m.getSenderName().length() > 40 || !isValidIpv4(m.getSenderIp())) return false;
        switch (m.getKind()) {
            case INVITE: case ACCEPT: case DECLINE:
                return m.getTurn() == 0 && m.getCell() == -1;
            case MOVE:
                return m.getTurn() >= 1 && m.getTurn() <= 9 && m.getCell() >= 0 && m.getCell() < 9;
            case RESIGN:
                return m.getTurn() >= 0 && m.getTurn() <= 9 && m.getCell() == -1;
            default:
                return false;
        }
    }

    private boolean match(GameMessage m) {
        if (gameId == null || !gameId.equals(m.getGameId()) || !m.getSenderIp().equals(opponentIp)) return false;
        // Ao aguardar ACCEPT, ainda não se conhece o UUID do convidado.
        return opponentId == null ? phase == Phase.OUTGOING : opponentId.equals(m.getSenderId());
    }

    private boolean validRemoteMove(GameMessage m) {
        return m.getTurn() == moves + 1 && m.getCell() >= 0 && m.getCell() < 9
                && board[m.getCell()] == ' ' && nextMark() == otherMark();
    }

    public GameMessage accept(String name, String ip) {
        if (phase != Phase.INCOMING) throw new IllegalStateException("Nenhum convite pendente.");
        configureLocal(name, ip);
        phase = Phase.PLAYING;
        status = "Partida iniciada contra " + opponentName + ". Aguarde X.";
        return message(Kind.ACCEPT, 0, -1);
    }

    public GameMessage decline(String name, String ip) {
        if (phase != Phase.INCOMING) throw new IllegalStateException("Nenhum convite pendente.");
        configureLocal(name, ip);
        phase = Phase.FINISHED;
        status = "Convite recusado.";
        return message(Kind.DECLINE, 0, -1);
    }

    public GameMessage play(int cell) {
        if (!canPlay(cell)) throw new IllegalStateException("Jogada inválida ou não é sua vez.");
        GameMessage m = message(Kind.MOVE, moves + 1, cell);
        placeInternal(cell, ownMark);
        return m;
    }

    public GameMessage resign() {
        if (!canEnd() || phase == Phase.INCOMING) throw new IllegalStateException("Não há partida para encerrar.");
        GameMessage m = message(Kind.RESIGN, moves, -1);
        phase = Phase.FINISHED;
        status = "Partida encerrada por você.";
        return m;
    }

    public void connectionFailed(String targetGameId, String details) {
        if (targetGameId != null && targetGameId.equals(gameId)) {
            phase = Phase.FINISHED;
            status = "Falha de envio: " + details + ". Partida local encerrada.";
        }
    }

    private char otherMark() { return ownMark == 'X' ? 'O' : 'X'; }
    private GameMessage message(Kind kind, int turn, int cell) {
        return new GameMessage(kind, gameId, localId, safeLocalName(), safeLocalIp(), turn, cell);
    }

    private void placeInternal(int cell, char mark) {
        board[cell] = mark;
        moves++;
        if (winner(mark)) {
            phase = Phase.FINISHED;
            status = mark == ownMark ? "Você venceu!" : "Você perdeu.";
        } else if (moves == 9) {
            phase = Phase.FINISHED;
            status = "Empate!";
        } else {
            status = nextMark() == ownMark ? "Sua vez (" + ownMark + ")." : "Vez do adversário (" + otherMark() + ").";
        }
    }

    private boolean winner(char m) {
        int[][] lines = {{0,1,2},{3,4,5},{6,7,8},{0,3,6},{1,4,7},{2,5,8},{0,4,8},{2,4,6}};
        for (int[] l : lines) {
            if (board[l[0]] == m && board[l[1]] == m && board[l[2]] == m) return true;
        }
        return false;
    }
}