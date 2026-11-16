package pitiupi.jogodavelha;
import java.net.InetAddress;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import pitiupi.api.plugin.PluginWorkspace;
import pitiupi.control.ConnectionManager;
import pitiupi.jogodavelha.core.GameSession;
import pitiupi.jogodavelha.core.GameSession.Inbound;
import pitiupi.jogodavelha.protocol.GameMessage;
import pitiupi.jogodavelha.protocol.GameMessage.Kind;
import pitiupi.jogodavelha.ui.GamePanel;
import pitiupi.models.Messages.Message;
import pitiupi.models.Plugin;
import pitiupi.net.tcp.TcpAttemptListener;
import pitiupi.net.tcp.TcpConnection;
import pitiupi.presentation.shell.LocalNetworkInfo;

public final class TicTacToePlugin extends Plugin {
    private final GameSession session = new GameSession();
    private ConnectionManager connections;
    private PluginWorkspace workspace;
    private GamePanel panel;

    @Override public String getName() { return "Jogo da Velha P2P"; }
    @Override public String getDisplayName() { return "Jogo da Velha"; }
    @Override public String getAuthor() { return "Eu"; }
    @Override public String getVersion() { return "1.0.0"; }
    @Override public String getIdentifier() { return "br.edu.ufsj.pitiupi.jogodavelha"; }
    @Override public String getDescription() { return "Jogo da Velha entre dois peers via TCP unicast, com protocolo JVD/1."; }

    @Override
    public void createPlugin(ConnectionManager cm, PluginWorkspace ws) {
        this.connections = cm;
        this.workspace = ws;
        this.panel = new GamePanel();
        panel.onInvite(this::invite);
        panel.onEnd(this::end);
        panel.onCell(this::play);
        bindShellWorkspaceContent(panel);
        refresh();
    }

    @Override
    public List<Class<? extends Message>> getMessages() {
        return List.of(GameMessage.class);
    }

    @Override
    public void receiveMessage(Message msg) {
        if (!(msg instanceof GameMessage)) return;
        GameMessage gameMessage = (GameMessage) msg;
        SwingUtilities.invokeLater(() -> receiveOnEdt(gameMessage));
    }

    private void receiveOnEdt(GameMessage msg) {
        if (panel == null || connections == null) return;
        try {
            session.configureLocal(connections.getUserName(), localIp());
        } catch (IllegalArgumentException ex) {
            workspace.setStatusMessage("Jogo da Velha: " + ex.getMessage());
            return;
        }
        Inbound outcome = session.receive(msg);
        if (!outcome.handled) return;
        workspace.openOrFocusTab(getIdentifier(), getDisplayName());
        if (outcome.automaticReply != null) {
            send(outcome.automaticReply, msg.getSenderIp());
        }
        refresh();
        if (outcome.requestConfirmation) {
            int answer = JOptionPane.showConfirmDialog(panel,
                    "Aceitar Jogo da Velha de " + session.opponentName()
                            + " (" + session.opponentIp() + ")?",
                    "Convite P2P", JOptionPane.YES_NO_OPTION);
            try {
                GameMessage response = answer == JOptionPane.YES_OPTION
                        ? session.accept(connections.getUserName(), localIp())
                        : session.decline(connections.getUserName(), localIp());
                send(response, session.opponentIp());
            } catch (IllegalStateException | IllegalArgumentException ex) {
                showError(ex.getMessage());
            }
            refresh();
        }
    }

    private void invite() {
        try {
            GameMessage m = session.invite(panel.peerIp(), connections.getUserName(), localIp());
            send(m, session.opponentIp());
        } catch (IllegalStateException | IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
        refresh();
    }

    private void play(int cell) {
        try {
            GameMessage m = session.play(cell);
            send(m, session.opponentIp());
        } catch (IllegalStateException ex) {
            showError(ex.getMessage());
        }
        refresh();
    }

    private void end() {
        try {
            GameMessage m = session.phase() == GameSession.Phase.INCOMING
                    ? session.decline(connections.getUserName(), localIp())
                    : session.resign();
            send(m, session.opponentIp());
        } catch (IllegalStateException | IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
        refresh();
    }

    private void send(GameMessage msg, String ip) {
        if (connections == null || msg == null) return;
        final String sentGameId = msg.getGameId();
        connections.sendTcpAsync(msg, ip, new TcpAttemptListener() {
            @Override public void onConnecting(String endpoint) {
                if (workspace != null) workspace.setStatusMessage("Jogo da Velha: conectando a " + endpoint);
            }
            @Override public void onSuccess(TcpConnection connection) {
                if (workspace != null) workspace.setStatusMessage("Jogo da Velha: mensagem " + msg.getKind() + " enviada.");
            }
            @Override public void onFailure(String reason) {
                // Falhas de envio de resposta automática a uma partida ocupada não cancelam outra partida.
                if (msg.getKind() != Kind.DECLINE) {
                    session.connectionFailed(sentGameId, reason);
                }
                if (workspace != null) workspace.setStatusMessage("Jogo da Velha: " + reason);
                refresh();
            }
        });
    }

    private void refresh() {
        if (panel != null) panel.render(session, localIp());
    }

    private String localIp() { return LocalNetworkInfo.bestLocalIpv4(); }

    private void showError(String message) {
        JOptionPane.showMessageDialog(panel, message, "Jogo da Velha", JOptionPane.WARNING_MESSAGE);
    }

    @Override
    public void onShellTabClosed() {
    }
}