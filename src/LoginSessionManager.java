import java.util.HashMap;
import java.util.UUID;

public class LoginSessionManager {
    private HashMap<UUID, User> sessionMap;

    public LoginSessionManager() {
        sessionMap = new HashMap<>();
    }

    public UUID createSession(User user) {
        UUID sessionID = UUID.randomUUID();
        sessionMap.put(sessionID, user);
        return sessionID;
    }

    public User getUser(UUID sessionID) {
        return sessionMap.get(sessionID);
    }

    public void removeSession(UUID sessionID) {
        sessionMap.remove(sessionID);
    }


}
