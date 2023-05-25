import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.time.Instant;
import java.util.ArrayList;

public class Nota implements Comparable<Nota>{

    public Nota(){}

    public Nota(Element e){
        System.out.println("Nueva nota" + this.toString());
        getNotaHeaderData(e);
        getNotaTextData(e);

    }
    public Nota(Instant timestamp, int postId, String user, ArrayList<String> referencedUsers, String text) {
        this.timestamp = timestamp;
        this.postId = postId;
        this.user = user;
        this.referencedUsers = referencedUsers;
        this.text = text;
        this.elementToString = elementToString;
    }

    private Instant timestamp;
    private int postId;
    private String user;
    private ArrayList<String> referencedUsers;
    private String text;

    public String getElementToString() {
        return elementToString;
    }

    public void setElementToString(String elementToString) {
        this.elementToString = elementToString;
    }

    private String elementToString;

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public long getPostId() {
        return postId;
    }

    public void setPostId(int postId) {
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

    @Override
    public int compareTo(Nota o) {
        return o.postId - this.postId;
    }

    private void getNotaHeaderData(Element notaElement) {
        Elements header = notaElement.getElementsByClass("comment-header");
        if (header.size() > 0) {
            Elements headerContent = header.get(0).getAllElements();
            for (var t : headerContent) {
                if (t.hasClass("username")) {
                    this.user = t.text();
                }
                if (t.hasAttr("data-ts")) {
                    long ts = Long.parseLong(t.attr("data-ts"));
                    this.timestamp = Instant.ofEpochSecond(ts);
                }
            }
        }
    }

    private void getNotaTextData(Element notaElement) {
        Elements body = notaElement.getElementsByClass("comment-text");
        if (body.size() > 0) {
            Element text = body.get(0);
            this.text = text.wholeText();
            elementToString = text.toString();
            postId = Integer.parseInt(text.id().substring(4));
        }
    }
}
