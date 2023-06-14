package notame;

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
import java.util.zip.GZIPInputStream;

public class FetchNotas {

    public static final String URL = "https://old.meneame.net//notame/?page=";
    public static final int MAX_PAGES = 41727;
    public static final int NOTAS_PER_PAGE = 50;
    private static boolean recalculated = false;
    private static final NotasDAO dao = new NotasDAO();

    public static void main(String[] args) {
        try {
            int page = 1;
            while (page <= MAX_PAGES) {
                page = getNotas(page);
            }
        } catch (URISyntaxException | InterruptedException e) {
            System.out.println("Boom: " + e);
        }

    }

    private static int getNotas(int page) throws URISyntaxException, InterruptedException {
        try {
            List<Nota> notasList = new ArrayList<>();

            long[] ids = dao.getMaxMinIdOnDatabase();
            List<Nota> listaNotas;
            while (notasList.size() < 1000) {
                listaNotas = getListaNotas(page, ids);
                notasList.addAll(listaNotas);
                System.out.println("Notas para insertar: " + notasList.size());
                page++;
            }
            return insertAndGetNextPage(page, notasList, ids);
        } catch (IOException | NullPointerException io) {
            System.out.println(io.getMessage());
            System.out.println("Boom: reintentando página " + page);
            return page;
        }


    }

    private static int insertAndGetNextPage(int page, List<Nota> notasList, long[] ids) {
        int notasInsertadas = dao.insertNotas(notasList);
        System.out.println("Notas insertadas: " + notasInsertadas);
        //if (notasInsertadas == 0) {
            //return page + getNextPage(ids);
        //} else {
            return page;
        //}
    }

    private static List<Nota> getListaNotas(int page, long[] ids) throws URISyntaxException, IOException, InterruptedException {
        System.out.printf("Página %d de %d (%.2f %%)\n", page, MAX_PAGES, (double) page * 100 / MAX_PAGES);
        long tic = System.currentTimeMillis();
        HttpResponse<InputStream> response = requestNotasFromPage(page);
        List<Nota> listaNotas = parseNotas(response);
        long tac = System.currentTimeMillis();
        long delay = Math.round((tac - tic) * 1.25);
        System.out.printf("Esperando %s ms. ", delay);
        long maxIdCurrentPage = listaNotas.stream().mapToLong(Nota::getPostId).max().orElse(-1L);
        long minIdCurrentPage = listaNotas.stream().mapToLong(Nota::getPostId).min().orElse(-1L);
        dao.insertPagina(page, new long[]{maxIdCurrentPage, minIdCurrentPage});
        Thread.sleep(delay);
        return listaNotas;
    }

    private static HttpResponse<InputStream> requestNotasFromPage(int page) throws URISyntaxException, IOException, InterruptedException {
        URI uri = new URI(URL + page);
        HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                .header("Accept-Encoding", "gzip,deflate,br")
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
    }

    private static int getNextPage(long[] ids) {
        if (!recalculated) {
            long jump = (ids[0] - ids[1]) / NOTAS_PER_PAGE;
            recalculated = true;
            System.out.printf("IdMax: %d, IdMin: %d, IdMax-IdMin = %d. ", ids[0], ids[1], ids[0] - ids[1]);
            System.out.printf("Saltando %d páginas\n", jump);
            return (int) jump;
        } else {
            return 1; // Next
        }
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
