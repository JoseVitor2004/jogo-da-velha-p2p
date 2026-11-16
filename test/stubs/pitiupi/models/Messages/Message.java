package pitiupi.models.Messages;
public abstract class Message implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private final boolean isPublic = true;
    public boolean isPublic() { return isPublic; }
}
