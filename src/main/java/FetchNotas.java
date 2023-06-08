import com.opencsv.*;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
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
        System.out.println("Página: " + page);

        while (notasList.size() < 5000){
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

    /*private static int getNextPage(int page, int notasRecopiladas) {
        if (notasRecopiladas == 0 && !recalculated) {
            recalculated = true;
            page = Math.floorDiv(notas.size(), NOTAS_PER_PAGE);
            System.out.println("Saltando a la página " + page);
            return page;
        }
        return page;
    }*/
/*    @Deprecated(forRemoval = true)
    private static void loadFromCSV() throws IOException, CsvException {
        try (CSVReader csvReader = new CSVReaderBuilder(
                new FileReader(FetchNotas.FILE_NAME)).withCSVParser(
                        new RFC4180ParserBuilder().withSeparator(';').build())
                .build()) {

            csvReader.skip(1);
            List<String[]> all = csvReader.readAll();
            all.parallelStream().forEach(line -> notas.putIfAbsent(line[1].transform(Long::parseLong), new Nota(line)));
        } catch (FileNotFoundException f) {
            System.out.println("No hay fichero (todavía)");
        } catch (IOException | CsvException e) {
            System.out.println("Boom: " + e);
            throw e;
        }


    }*/

    /* @Deprecated(forRemoval = true)
    private static void writeToCSV() throws IOException, CsvDataTypeMismatchException, CsvRequiredFieldEmptyException {
        Writer writer = new FileWriter(FILE_NAME);
        StatefulBeanToCsv<Nota> notaToCsv = new StatefulBeanToCsvBuilder<Nota>(writer)
                .withSeparator(';')
                .withApplyQuotesToAll(true).
                build();
        notaToCsv.write(notas.values().parallelStream());
        writer.flush();
        writer.close();
    } */

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
