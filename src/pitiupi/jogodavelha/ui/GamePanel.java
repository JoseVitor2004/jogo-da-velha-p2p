package pitiupi.jogodavelha.ui;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import pitiupi.jogodavelha.core.GameSession;

public final class GamePanel extends JPanel {
    private final JTextField peerIp = new JTextField(15);
    private final JButton invite = new JButton("Convidar");
    private final JButton end = new JButton("Encerrar partida");
    private final JButton[] cells = new JButton[9];
    private final JLabel status = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel context = new JLabel(" ", SwingConstants.CENTER);

    public GamePanel() {
        super(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        JPanel header = new JPanel(new BorderLayout(8, 8));
        JPanel actions = new JPanel();
        actions.add(new JLabel("IPv4 do colega:"));
        actions.add(peerIp);
        actions.add(invite);
        actions.add(end);
        header.add(actions, BorderLayout.NORTH);
        header.add(context, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        JPanel board = new JPanel(new GridLayout(3, 3, 7, 7));
        for (int i = 0; i < cells.length; i++) {
            JButton button = new JButton(" ");
            button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 44));
            button.setFocusable(false);
            cells[i] = button;
            board.add(button);
        }
        add(board, BorderLayout.CENTER);
        status.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        add(status, BorderLayout.SOUTH);
    }

    public void onInvite(Runnable task) { invite.addActionListener(e -> task.run()); }
    public void onEnd(Runnable task) { end.addActionListener(e -> task.run()); }
    public void onCell(java.util.function.IntConsumer task) {
        for (int i = 0; i < cells.length; i++) {
            final int pos = i;
            cells[i].addActionListener(e -> task.accept(pos));
        }
    }
    public String peerIp() { return peerIp.getText().trim(); }
    public void focusPeer(String ip) { peerIp.setText(ip); }

    public void render(GameSession game, String localIp) {
        invite.setEnabled(game.canInvite());
        end.setEnabled(game.canEnd());
        peerIp.setEnabled(game.canInvite());
        for (int i = 0; i < cells.length; i++) {
            char value = game.cell(i);
            cells[i].setText(value == ' ' ? " " : Character.toString(value));
            cells[i].setEnabled(game.canPlay(i));
        }
        String side = game.ownMark() == '\0' ? "—" : Character.toString(game.ownMark());
        String destination = game.opponentIp() == null ? "—" : game.opponentIp();
        context.setText("Seu IP: " + localIp + "   |   Você: " + side + "   |   Peer: " + destination);
        status.setText("<html><div style='text-align:center'>" + htmlEscape(game.status()) + "</div></html>");
    }

    private static String htmlEscape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}