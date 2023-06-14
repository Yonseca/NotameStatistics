package pojo;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Nota {

    public Nota(){}

    public Nota(Element e){
        getNotaHeaderData(e);
        getNotaTextData(e);
    }

    public Nota(int timestamp, int postId, String user, ArrayList<String> referencedUsers, String text, String html) {
        this.timestamp = timestamp;
        this.postId = postId;
        this.user = user;
        this.referencedUsers = referencedUsers;
        this.text = text;
        this.html = html;
    }

    @Deprecated
    public Nota(String[] csvLine) {
        this.timestamp = Integer.parseInt(csvLine[4]);
        this.postId = Integer.parseInt(csvLine[1]);
        this.user = csvLine[5];
        this.referencedUsers = Arrays.stream(csvLine[2].split(" ")).toList();
        this.text = csvLine[3];
        this.html = csvLine[0];
    }

    private long timestamp;
    private long postId;
    private String user;
    private List<String> referencedUsers;
    private String text;
    private String html;

    public String getHtml() {
        return html;
    }

    public void setHtml(String html) {
        this.html = html;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
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

    public List<String> getReferencedUsers() {
        return referencedUsers;
    }

    public void setReferencedUsers(List<String> referencedUsers) {
        this.referencedUsers = referencedUsers;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
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
                    this.timestamp = Integer.parseInt(t.attr("data-ts"));
                }
            }
        }
    }

    private void getNotaTextData(Element notaElement) {
        Elements body = notaElement.getElementsByClass("comment-text");
        if (body.size() > 0) {
            Element text = body.get(0);
            this.text = text.wholeText();
            this.html = text.html();
            this.referencedUsers = Arrays.stream(this.text.split(" "))
                    .filter(word -> word.startsWith("@")).toList();
            postId = Integer.parseInt(text.id().substring(4));
        }

    }
}
