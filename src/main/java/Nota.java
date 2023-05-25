import java.time.Instant;
import java.util.ArrayList;

public class Nota {

    public Nota(){}

    public Nota(Instant timestamp, long postId, String user, ArrayList<String> referencedUsers, String text) {
        this.timestamp = timestamp;
        this.postId = postId;
        this.user = user;
        this.referencedUsers = referencedUsers;
        this.text = text;
    }

    Instant timestamp;
    long postId;
    String user;
    ArrayList<String> referencedUsers;
    String text;

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public long getPostId() {
        return postId;
    }

    public void setPostId(long postId) {
        this.postId = postId;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public ArrayList<String> getReferencedUsers() {
        return referencedUsers;
    }

    public void setReferencedUsers(ArrayList<String> referencedUsers) {
        this.referencedUsers = referencedUsers;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
