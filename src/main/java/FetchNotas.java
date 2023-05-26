import com.opencsv.CSVReader;
import com.opencsv.bean.*;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.zip.GZIPInputStream;

public class FetchNotas {

    public static final String URL = "https://old.meneame.net//notame/?page=";
    private static final HashSet<Nota> notas = new HashSet<>();
    public static final String FILE_NAME = "patata.csv";

    public static void main(String[] args) {
        loadFromCSV(FetchNotas.FILE_NAME);
        try {
            for (int maxPages = 1; maxPages <= 550; maxPages++) {
                System.out.println("Página: " + maxPages);
                long tic = System.currentTimeMillis();
                URI uri = new URI(URL + maxPages);
                HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

                HttpRequest request = HttpRequest.newBuilder(uri)
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                        .header("Accept-Encoding", "gzip,deflate,br")
                        .build();

                HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
                getNotas(response);
                long tac = System.currentTimeMillis();
                long runTime = tac-tic;
                System.out.println("Tiempo de ejecución: " + runTime);
                System.out.println("Esperando " + Math.round(runTime*1.25) + " ms.");
                Thread.sleep(Math.round(runTime*1.25));

            }

            writeToCSV();

        } catch (URISyntaxException | IOException | InterruptedException | CsvRequiredFieldEmptyException |
                 CsvDataTypeMismatchException e) {
            System.out.println("Boom: " + e.getMessage());
        }
    }

    private static void loadFromCSV(String fileName) {
        try(CSVReader csvReader = new CSVReader(new FileReader (FetchNotas.FILE_NAME))){

            HeaderColumnNameMappingStrategy<Nota> mapping = new HeaderColumnNameMappingStrategyBuilder<Nota>().build();
            CsvToBean<Nota> csvToNota = new CsvToBeanBuilder<Nota>(csvReader)
                    .withMappingStrategy(mapping).withStrictQuotes(true).withSeparator(';').build();
            csvToNota.forEach(notas::add);
            System.out.println("patata");

        } catch (FileNotFoundException f){
            System.out.println("Boom: " + f);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    private static void writeToCSV() throws IOException, CsvDataTypeMismatchException, CsvRequiredFieldEmptyException {
        Writer writer = new FileWriter(FILE_NAME);
        StatefulBeanToCsv<Nota> notaToCsv = new StatefulBeanToCsvBuilder<Nota>(writer)
                .withSeparator(';')
                .withApplyQuotesToAll(true).
                build();
        notaToCsv.write(notas.stream());
        writer.flush();
        writer.close();
    }

    private static void getNotas(HttpResponse<InputStream> response) {
        try (GZIPInputStream g = new GZIPInputStream(response.body())) {
            String string = new String(g.readAllBytes());
            Document doc = Jsoup.parse(string);
            var elements = doc.getElementsByAttributeValueStarting("data-id", "post-");
            elements.forEach(e -> notas.add(new Nota(e)));
        } catch (IOException io) {
            System.out.println("Boom: " + io.getMessage());
        }
//        notas.forEach((v) -> System.out.println(v.getUser() + ":" + v.getTimestamp() +  ":" + v.getText() + "\n"));

    }


}
