package pitiupi.models;
import javax.swing.JComponent;
import java.util.List;
import pitiupi.api.plugin.PluginWorkspace;
import pitiupi.control.ConnectionManager;
import pitiupi.models.Messages.Message;
public abstract class Plugin {
    private JComponent content;
    public abstract String getName();
    public String getDisplayName() { return getName(); }
    public abstract String getAuthor();
    public abstract String getVersion();
    public String getDescription() { return ""; }
    public abstract String getIdentifier();
    protected final void bindShellWorkspaceContent(JComponent c) { content = c; }
    public final JComponent getShellWorkspaceContent() { return content; }
    public abstract void createPlugin(ConnectionManager cm, PluginWorkspace ws);
    public abstract List<Class<? extends Message>> getMessages();
    public abstract void receiveMessage(Message m);
    public void onShellTabClosed() { }
}
