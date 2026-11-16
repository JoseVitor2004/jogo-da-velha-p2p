package pitiupi.jogodavelha;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import pitiupi.jogodavelha.core.GameSession;
import pitiupi.jogodavelha.protocol.GameMessage;
import pitiupi.jogodavelha.protocol.GameMessage.Kind;

public final class GameSessionTest {
    private static final String A = "192.168.100.11";
    private static final String B = "192.168.100.12";
    private static int tests;

    private static void check(boolean value, String reason) {
        tests++;
        if (!value) throw new AssertionError(reason);
    }

    private static class Players {
        final GameSession x = new GameSession();
        final GameSession o = new GameSession();
        Players() {
            x.configureLocal("Alice", A);
            o.configureLocal("Bruno", B);
            GameMessage invite = x.invite(B, "Alice", A);
            check(invite.getKind() == Kind.INVITE, "INVITE");
            check(o.receive(invite).requestConfirmation, "pedido de convite");
            GameMessage ack = o.accept("Bruno", B);
            check(x.receive(ack).handled, "ACCEPT");
            check(x.myTurn() && !o.myTurn(), "X começa");
        }
        GameMessage move(GameSession player, GameSession recipient, int cell) {
            GameMessage msg = player.play(cell);
            check(recipient.receive(msg).handled, "MOVE aceito");
            return msg;
        }
    }

    private static void testWinAndReplay() {
        Players p = new Players();
        GameMessage move = p.move(p.x, p.o, 0);
        check(!p.o.receive(move).handled && p.o.moves() == 1, "duplicata descartada");
        p.move(p.o, p.x, 3);
        p.move(p.x, p.o, 1);
        p.move(p.o, p.x, 4);
        p.move(p.x, p.o, 2);
        check(p.x.phase() == GameSession.Phase.FINISHED, "X terminou");
        check(p.o.phase() == GameSession.Phase.FINISHED, "O terminou");
        check("Você venceu!".equals(p.x.status()), "X venceu");
        check("Você perdeu.".equals(p.o.status()), "O perdeu");
    }

    private static void testOutOfOrderAndInjection() {
        Players p = new Players();
        GameMessage badSequence = new GameMessage(Kind.MOVE, p.x.gameId(), p.o.localId(),
                "Bruno", B, 2, 4);
        check(!p.x.receive(badSequence).handled && p.x.moves() == 0, "fora de ordem");
        GameMessage wrongIp = new GameMessage(Kind.MOVE, p.x.gameId(), p.o.localId(),
                "Bruno", "192.168.10.99", 1, 4);
        check(!p.x.receive(wrongIp).handled, "IP declarado divergente");
        GameMessage wrongSender = new GameMessage(Kind.MOVE, p.x.gameId(),
                java.util.UUID.randomUUID().toString(), "Bruno", B, 1, 4);
        check(!p.x.receive(wrongSender).handled, "ID de peer divergente");
        check(!p.x.receive(new GameMessage(Kind.MOVE, p.x.gameId(), p.o.localId(),
                "Bruno", B, 1, 4)).handled, "turno errado");
        check(p.x.canPlay(0), "turno original preservado");
    }

    private static void testDeclineAndBusy() {
        GameSession a = new GameSession();
        GameSession b = new GameSession();
        a.configureLocal("Alice", A);
        b.configureLocal("Bruno", B);
        GameMessage inv = a.invite(B, "Alice", A);
        check(b.receive(inv).requestConfirmation, "convite recebido");
        GameMessage denied = b.decline("Bruno", B);
        check(a.receive(denied).handled, "recusa recebida");
        check(a.canInvite() && b.canInvite(), "livre após recusa");
        Players p = new Players();
        GameSession c = new GameSession();
        GameMessage cInvite = c.invite(B, "Carol", "192.168.100.13");
        GameSession.Inbound rejected = p.o.receive(cInvite);
        check(rejected.automaticReply != null && rejected.automaticReply.getKind() == Kind.DECLINE, "ocupado recusa");
        check(p.o.phase() == GameSession.Phase.PLAYING, "partida ativa preservada");
        check(c.receive(rejected.automaticReply).handled, "Carol vê recusa");
    }

    private static void testDraw() {
        Players p = new Players();
        int[] moves = {0,1,2,4,3,5,7,6,8};
        for (int i = 0; i < moves.length; i++) {
            if (i % 2 == 0) p.move(p.x, p.o, moves[i]);
            else p.move(p.o, p.x, moves[i]);
        }
        check(p.x.phase() == GameSession.Phase.FINISHED, "empate encerra");
        check("Empate!".equals(p.x.status()) && "Empate!".equals(p.o.status()), "dois veem empate");
    }

    private static void testResignAndSerialization() throws Exception {
        Players p = new Players();
        GameMessage resignation = p.o.resign();
        check(p.x.receive(resignation).handled, "desistência informada");
        check(p.x.phase() == GameSession.Phase.FINISHED, "desistência finaliza");
        GameMessage message = new GameMessage(Kind.MOVE, java.util.UUID.randomUUID().toString(),
                java.util.UUID.randomUUID().toString(), "Alice", A, 3, 7);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) { out.writeObject(message); }
        GameMessage restored;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            restored = (GameMessage) in.readObject();
        }
        check(restored.getVersion() == 1 && restored.getKind() == Kind.MOVE
                && restored.getCell() == 7 && restored.getTurn() == 3, "serialização Java");
        check(GameSession.isValidIpv4("192.168.1.2") && !GameSession.isValidIpv4("300.2.1.1")
                && !GameSession.isValidIpv4("224.0.0.3"), "validação IPv4");
    }

    public static void main(String[] args) throws Exception {
        testWinAndReplay();
        testOutOfOrderAndInjection();
        testDeclineAndBusy();
        testDraw();
        testResignAndSerialization();
        System.out.println("OK: " + tests + " verificações do protocolo JVD/1.");
    }
}