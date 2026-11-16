package pitiupi.control;
import pitiupi.models.Messages.Message;
import pitiupi.net.tcp.TcpAttemptListener;
public class ConnectionManager {
    public String getUserName() { return "Teste"; }
    public void sendTcpAsync(Message m, String ip, TcpAttemptListener l) { l.onSuccess(null); }
}
