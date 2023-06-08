import dao.NotasDAO;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import pojo.Nota;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.zip.GZIPInputStream;

public class FetchNotas {

    public static final String URL = "https://old.meneame.net//notame/?page=";
    public static final int MAX_PAGES = 41710;
    public static final int NOTAS_PER_PAGE = 50;
    //public static final String FILE_NAME = "patata.csv";
    //private static final ConcurrentHashMap<Long, pojo.Nota> notas = new ConcurrentHashMap<>();

    private static boolean recalculated = false;

    private static final NotasDAO dao = new NotasDAO();


    public static void main(String[] args) {
        try {
            //loadFromCSV();
            for (int page = 1; page <= MAX_PAGES; page++) {
                page = getNotas(page);
            }
        } catch (URISyntaxException | IOException | InterruptedException e) {
            System.out.println("Boom: " + e);
        }

    }

    private static int getNotas(int page) throws URISyntaxException, IOException, InterruptedException {
        List<Nota> notasList = new ArrayList<>();


        while (notasList.size() < 2500){
            System.out.println("Página: " + page);
            long tic = System.currentTimeMillis();
            HttpResponse<InputStream> response = requestNotasFromPage(page);
            long tac = System.currentTimeMillis();
            long runTime = tac - tic;
            System.out.println("Tiempo de ejecución: " + runTime);
            System.out.println("Esperando " + Math.round(runTime * 1.25) + " ms.");
            Thread.sleep(Math.round(runTime * 1.25));

            notasList.addAll(Objects.requireNonNull(parseNotas(response)));
            System.out.println("Notas para insertar: " + notasList.size());

            page++;
        }
        dao.insert(notasList);


        int notasInsertadas = dao.insert(notasList);


        return page; //getNextPage(page, notasInsertadas);
    }

    private static HttpResponse<InputStream> requestNotasFromPage(int page) throws URISyntaxException, IOException, InterruptedException {
        URI uri = new URI(URL + page);
        HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                .header("Accept-Encoding", "gzip,deflate,br")
                .build();

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        return response;
    }

    private static int getNextPage(int page, int notasRecopiladas) {
        if (notasRecopiladas == 0 && !recalculated) {
            recalculated = true;
            page = Math.floorDiv(notas.size(), NOTAS_PER_PAGE);
            System.out.println("Saltando a la página " + page);
            return page;
        }
        return page;
    }

    private static List<Nota> parseNotas(HttpResponse<InputStream> response) {
        try (GZIPInputStream g = new GZIPInputStream(response.body())) {
            String string = new String(g.readAllBytes());
            Document doc = Jsoup.parse(string);
            var elements = doc.getElementsByAttributeValueStarting("data-id", "post-");

            return elements.stream().map(Nota::new).toList();
        } catch (IOException io) {
            System.out.println("Boom: " + io);
            return null;
        }
    }


}
