package pitiupi.api.plugin;
import javax.swing.JComponent;
public interface PluginWorkspace {
    void openOrFocusTab(String id, String title, JComponent content);
    default void openOrFocusTab(String id, String title) { openOrFocusTab(id,title,null); }
    void setStatusMessage(String message);
}
