package java.notame;

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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

public class FetchNotas {

    private static final Logger logger = Logger.getLogger(FetchNotas.class.getName());

    public static final String URL = "https://old.meneame.net//notame/?page=";
    public static final int MAX_PAGES = 41727;
    public static final int NOTAS_PER_PAGE = 50;
    private static boolean recalculated = false;
    private static final NotasDAO dao = new NotasDAO();

    public static void main(String[] args) throws URISyntaxException, InterruptedException {
        logger.entering(FetchNotas.class.getName(), FetchNotas.class.getEnclosingMethod().getName());

        try {
            int page = 1;
            while (page <= MAX_PAGES) {
                page = getNotas(page);
            }
        } catch (URISyntaxException e) {
            logger.log(Level.SEVERE, "Boom: ", e);
            throw e;
        } catch (InterruptedException interruptedException){
            logger.log(Level.SEVERE, "Hilo interrumpido: ", interruptedException);
            throw interruptedException;
        }

        logger.exiting(FetchNotas.class.getName(), FetchNotas.class.getEnclosingMethod().getName());

    }

    private static int getNotas(int page) throws URISyntaxException, InterruptedException {
        logger.entering(FetchNotas.class.getName(), FetchNotas.class.getEnclosingMethod().getName());

        try {
            List<Nota> notasList = new ArrayList<>();

            long[] ids = dao.getMaxMinIdOnDatabase();
            List<Nota> listaNotas;
            while (notasList.size() < 1000) {
                listaNotas = getListaNotas(page, ids);
                notasList.addAll(listaNotas);
                logger.log(Level.INFO, "Notas para insertar: {}", notasList.size());
                page++;
            }
            logger.exiting(FetchNotas.class.getName(), FetchNotas.class.getEnclosingMethod().getName());

            return insertAndGetNextPage(page, notasList, ids);
        } catch (IOException | NullPointerException io) {
            logger.log(Level.SEVERE, "Boom: reintentando página {}", page);
            logger.exiting(FetchNotas.class.getName(), FetchNotas.class.getEnclosingMethod().getName());

            return page;
        }


    }

    private static int insertAndGetNextPage(int page, List<Nota> notasList, long[] ids) {
        logger.entering(FetchNotas.class.getName(), FetchNotas.class.getEnclosingMethod().getName());

        int notasInsertadas = dao.insertNotas(notasList);
        logger.log(Level.INFO, () -> "Notas insertadas: " + notasInsertadas);

        //if (notasInsertadas == 0) {
            //return page + getNextPage(ids);
        //} else {
        logger.exiting(FetchNotas.class.getName(), FetchNotas.class.getEnclosingMethod().getName());

        return page;
        //}
    }

    private static List<Nota> getListaNotas(int page, long[] ids) throws URISyntaxException, IOException, InterruptedException {
        logger.entering(FetchNotas.class.getName(), FetchNotas.class.getEnclosingMethod().getName());
        logger.log(Level.INFO, () -> "Página " + page + " de " + MAX_PAGES + " (" +  (double)(page * 100) / MAX_PAGES + " %)");

        long tic = System.currentTimeMillis();
        HttpResponse<InputStream> response = requestNotasFromPage(page);
        List<Nota> listaNotas = parseNotas(response);
        long tac = System.currentTimeMillis();
        long delay = Math.round((tac - tic) * 1.25);
        logger.log(Level.INFO, "Esperando {} ms. ", delay);
        long maxIdCurrentPage = listaNotas.stream().mapToLong(Nota::getPostId).max().orElse(-1L);
        long minIdCurrentPage = listaNotas.stream().mapToLong(Nota::getPostId).min().orElse(-1L);
        dao.insertPagina(page, new long[]{maxIdCurrentPage, minIdCurrentPage});
        Thread.sleep(delay);
        logger.exiting(FetchNotas.class.getName(), FetchNotas.class.getEnclosingMethod().getName());

        return listaNotas;
    }

    private static HttpResponse<InputStream> requestNotasFromPage(int page) throws URISyntaxException, IOException, InterruptedException {
        logger.entering(FetchNotas.class.getName(), FetchNotas.class.getEnclosingMethod().getName());

        URI uri = new URI(URL + page);
        HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                .header("Accept-Encoding", "gzip,deflate,br")
                .build();
        logger.exiting(FetchNotas.class.getName(), FetchNotas.class.getEnclosingMethod().getName());

        return httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
    }

    private static int getNextPage(long[] ids) {
        logger.entering(FetchNotas.class.getName(), FetchNotas.class.getEnclosingMethod().getName());

        if (!recalculated) {
            long jump = (ids[0] - ids[1]) / NOTAS_PER_PAGE;
            recalculated = true;
            logger.log(Level.INFO, () ->
                    "IdMax: " + ids[0] + ", " +
                    "IdMin: " + ids[1] + ", " +
                    "IdMax-IdMin = " + (ids[0] - ids[1]) + " . ");
            logger.log(Level.INFO, "Saltando {} páginas", jump);
            logger.exiting(FetchNotas.class.getName(), FetchNotas.class.getEnclosingMethod().getName());

            return (int) jump;
        } else {
            logger.exiting(FetchNotas.class.getName(), FetchNotas.class.getEnclosingMethod().getName());

            return 1; // Next
        }
    }

    private static List<Nota> parseNotas(HttpResponse<InputStream> response) {
        logger.entering(FetchNotas.class.getName(), FetchNotas.class.getEnclosingMethod().getName());

        try (GZIPInputStream g = new GZIPInputStream(response.body())) {
            String string = new String(g.readAllBytes());
            Document doc = Jsoup.parse(string);
            var elements = doc.getElementsByAttributeValueStarting("data-id", "post-");
            logger.exiting(FetchNotas.class.getName(), FetchNotas.class.getEnclosingMethod().getName());

            return elements.stream().map(Nota::new).toList();
        } catch (IOException io) {
            logger.severe(() -> "Boom: " + io);
            logger.exiting(FetchNotas.class.getName(), FetchNotas.class.getEnclosingMethod().getName());

            return null;
        }
    }
}
