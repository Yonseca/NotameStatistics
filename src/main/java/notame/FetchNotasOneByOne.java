package notame;

import dao.NotasDAO;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import pojo.Nota;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

public class FetchNotasOneByOne {

    private static final Logger logger = Logger.getLogger(FetchNotasOneByOne.class.getName());

    public static final String URL = "https://old.meneame.net//notame/";
    private static final NotasDAO dao = new NotasDAO();
    private Nota nota;

    public void FetchNota() {
    }

    public Nota FetchNota(long id) throws URISyntaxException, InterruptedException {
        logger.entering(FetchNotasOneByOne.class.getName(), "Main");
        id = 1;
        try {
            logger.exiting(FetchNotasOneByOne.class.getName(), Thread.currentThread().getStackTrace()[1].getMethodName());
            return requestNota(id);
        } catch (URISyntaxException e) {
            logger.log(Level.SEVERE, "Boom: ", e);
            throw e;
        } catch (InterruptedException interruptedException) {
            logger.log(Level.SEVERE, "Hilo interrumpido: ", interruptedException);
            throw interruptedException;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Hilo interrumpido: ", e);
            throw new RuntimeException(e);
        }


    }


//    private static Nota getNota(int id) throws URISyntaxException, InterruptedException {
//        logger.entering(FetchNota.class.getName(), Thread.currentThread().getStackTrace()[1].getMethodName());
//
//        try {
//            listaNotas = getListaNotas(page, ids);
//        }
//        logger.exiting(FetchNota.class.getName(), Thread.currentThread().getStackTrace()[1].getMethodName());
//
//        return insertAndGetNextPage(page, notasList, ids);
//    } catch(IOException | NullPointerException io) {
//        logger.log(Level.SEVERE, "Boom: reintentando página {0}", page);
//        logger.log(Level.SEVERE, "Boom:", io);
//        logger.exiting(FetchNota.class.getName(), Thread.currentThread().getStackTrace()[1].getMethodName());
//
//        return page;
//    }


//    @Deprecated
//    private static int insertAndGetNextPage(int page, List<Nota> notasList, long[] ids) {
//        logger.entering(FetchNota.class.getName(), Thread.currentThread().getStackTrace()[1].getMethodName());
//
//        int notasInsertadas = dao.insertNotas(notasList);
//        logger.log(Level.INFO, () -> "Notas insertadas: " + notasInsertadas);
//        logger.exiting(FetchNota.class.getName(), Thread.currentThread().getStackTrace()[1].getMethodName());
//
//        return page;
//    }

//    private static List<Nota> getListaNotas(int page, long[] ids) throws
//            URISyntaxException, IOException, InterruptedException {
//        logger.entering(FetchNota.class.getName(), Thread.currentThread().getStackTrace()[1].getMethodName());
//        logger.log(Level.INFO, () -> "Página " + page + " de " + MAX_PAGES + " (" + (double) (page * 100) / MAX_PAGES + " %)");
//
//        long tic = System.currentTimeMillis();
//        HttpResponse<InputStream> response = requestNotasFromPage(page);
//        List<Nota> listaNotas = parseNotas(response);
//        long tac = System.currentTimeMillis();
//        long delay = Math.round((tac - tic) * 1.25);
//        logger.log(Level.INFO, "Esperando {0} ms. ", delay);
//        Thread.sleep(delay);
//        logger.exiting(FetchNota.class.getName(), Thread.currentThread().getStackTrace()[1].getMethodName());
//
//        return listaNotas;
//    }

    private Nota requestNota(long id) throws
            URISyntaxException, IOException, InterruptedException {
        logger.entering(FetchNotasOneByOne.class.getName(), Thread.currentThread().getStackTrace()[1].getMethodName());

        URI uri = new URI(URL + id);
        try {
            HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                    .header("Accept-Encoding", "gzip,deflate,br")
                    .build();

            logger.exiting(FetchNotasOneByOne.class.getName(), Thread.currentThread().getStackTrace()[1].getMethodName());

            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            List<Nota> listaNotasResponse = parseNotas(response);
            assert listaNotasResponse != null;
            for (Nota n : listaNotasResponse) {
                if (n.getPostId() == id) return n;
                else return null;
            }
        } catch (Exception e) {
            logger.exiting(FetchNotasOneByOne.class.getName(), Thread.currentThread().getStackTrace()[1].getMethodName());
            throw new RuntimeException(e);
        }
        return null;
    }


    private List<Nota> parseNotas(HttpResponse<InputStream> response) {
        logger.entering(FetchNotasOneByOne.class.getName(), Thread.currentThread().getStackTrace()[1].getMethodName());

        try (GZIPInputStream g = new GZIPInputStream(response.body())) {
            String string = new String(g.readAllBytes());
            Document doc = Jsoup.parse(string);
            var elements = doc.getElementsByAttributeValueStarting("data-id", "post-");
            logger.exiting(FetchNotasOneByOne.class.getName(), Thread.currentThread().getStackTrace()[1].getMethodName());

            return elements.stream().map(Nota::new).toList();
        } catch (IOException io) {
            logger.severe(() -> "Boom: " + io);
            logger.exiting(FetchNotasOneByOne.class.getName(), Thread.currentThread().getStackTrace()[1].getMethodName());

            return null;
        }
    }

//    private static Nota parseNota(HttpResponse<InputStream> response, long id) {
//        logger.entering(FetchNota.class.getName(), Thread.currentThread().getStackTrace()[1].getMethodName());
//
//        try (GZIPInputStream g = new GZIPInputStream(response.body())) {
//            String string = new String(g.readAllBytes());
//            Document doc = Jsoup.parse(string);
//            var elements = doc.getElementsByAttributeValueStarting("data-id", "post-" + id);
//            logger.exiting(FetchNota.class.getName(), Thread.currentThread().getStackTrace()[1].getMethodName());
//
//            return elements.stream().
//
//            )
//        } catch (IOException io) {
//            logger.severe(() -> "Boom: " + io);
//            logger.exiting(FetchNota.class.getName(), Thread.currentThread().getStackTrace()[1].getMethodName());
//
//            return null;
//        }
//    }
}
