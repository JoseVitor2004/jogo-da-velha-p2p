package pitiupi.net.tcp;
public interface TcpAttemptListener {
    void onConnecting(String endpoint);
    void onSuccess(TcpConnection connection);
    void onFailure(String message);
}
