import com.opencsv.*;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;

public class FetchNotas {

    public static final String URL = "https://old.meneame.net//notame/?page=";
    public static final int MAX_PAGES = 41710;
    public static final int NOTAS_PER_PAGE = 50;
    public static final String FILE_NAME = "patata.csv";
    private static final ConcurrentHashMap<Long, Nota> notas = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        try {
            loadFromCSV();
            for (int maxPages = 1; maxPages <= MAX_PAGES; maxPages++) {
                maxPages = requestNotas(maxPages);
                writeToCSV();
            }
        } catch (URISyntaxException | IOException | InterruptedException | CsvException e) {
            System.out.println("Boom: " + e);
        }

    }

    private static int requestNotas(int maxPages) throws URISyntaxException, IOException, InterruptedException {

        int notasBeforeRequest = notas.size();
        System.out.println("Página: " + maxPages);
        long tic = System.currentTimeMillis();
        URI uri = new URI(URL + maxPages);
        HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                .header("Accept-Encoding", "gzip,deflate,br")
                .build();

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        parseNotas(response);
        long tac = System.currentTimeMillis();
        long runTime = tac - tic;
        int notasRecopiladas = notas.size() - notasBeforeRequest;
        System.out.println("Tiempo de ejecución: " + runTime);
        System.out.println("Esperando " + Math.round(runTime * 1.25) + " ms.");
        System.out.println("Notas recopiladas: " + notasRecopiladas);
        System.out.println("Notas totales: " + notas.size());

        Thread.sleep(Math.round(runTime * 1.25));

        if (notasRecopiladas == 0) {
            maxPages = Math.floorDivExact(notas.size(), NOTAS_PER_PAGE);
            System.out.println("Saltando a la página " + (maxPages + 1));
            return maxPages;
        }
        return maxPages;
    }

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


    }

    private static void writeToCSV() throws IOException, CsvDataTypeMismatchException, CsvRequiredFieldEmptyException {
        Writer writer = new FileWriter(FILE_NAME);
        StatefulBeanToCsv<Nota> notaToCsv = new StatefulBeanToCsvBuilder<Nota>(writer)
                .withSeparator(';')
                .withApplyQuotesToAll(true).
                build();
        notaToCsv.write(notas.values().parallelStream());
        writer.flush();
        writer.close();
    }

    private static void parseNotas(HttpResponse<InputStream> response) {
        try (GZIPInputStream g = new GZIPInputStream(response.body())) {
            String string = new String(g.readAllBytes());
            Document doc = Jsoup.parse(string);
            var elements = doc.getElementsByAttributeValueStarting("data-id", "post-");
            elements.forEach(e -> {
                Nota nuevaNota = new Nota(e);
                notas.putIfAbsent(nuevaNota.getPostId(), nuevaNota);
            });

        } catch (IOException io) {
            System.out.println("Boom: " + io);
        }
    }


}
