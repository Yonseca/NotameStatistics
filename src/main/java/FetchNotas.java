import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Flow;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class FetchNotas {

    public FetchNotas() {


    }

    public static void main(String[] args) {
        String url = "https://old.meneame.net//notame/?page="; // añadir página
        String maxPages = "1";


        try {
            URI uri = new URI(url + maxPages);
            HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

            HttpRequest request = HttpRequest.newBuilder(uri)
                    //.header("Accept-Language", "*")
                    .header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:109.0) Gecko/20100101 Firefox/113.0")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                    .header("Accept-Encoding", "gzip,deflate,br")
                    .build();

            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            InputStream gzip = response.body();
           try (GZIPInputStream g = new GZIPInputStream(response.body())){
                String result = g.toString();
                byte[] puke = g.readAllBytes();
                String string = new String(puke);
               Document doc = Jsoup.parse(string);
               var elements = doc.getElementsByAttributeValueStarting("data-id", "post-");
               elements.parallelStream().forEach(e -> {
                   Nota nota = new Nota();
                   Elements header = e.getElementsByClass("comment-header");
                   Elements text = e.getElementsByClass("comment-text");
                   Elements footer = e.getElementsByClass("comment-footer");
                   nota.setText(text.first().text());

               });


               System.out.println(string);
            }
            System.out.println(response.body());

        } catch (URISyntaxException | IOException | InterruptedException e) {
            System.out.println("Boom: " + e.getMessage());
        }
    }
}
