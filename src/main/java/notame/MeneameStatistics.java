package notame;

import dao.NotasDAO;
import pojo.Nota;

import java.net.URISyntaxException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;


public class MeneameStatistics {

    private static final Logger logger = Logger.getLogger(MeneameStatistics.class.getName());

    public static void main(String[] args) throws URISyntaxException, InterruptedException {
        logger.entering(MeneameStatistics.class.getName(), "Main");
        FetchNotasOneByOne fetchNotasOneByOne = new FetchNotasOneByOne();
        // Startup
        NotasDAO notasDAO = new NotasDAO();
        long[] minMax = notasDAO.getMaxMinIdOnDatabase();
        Set<Long> ids = notasDAO.getAllIds();

        Set<Long> missingNotas = LongStream.rangeClosed(1,minMax[0]).boxed().collect(Collectors.toSet());
        System.out.println("Ids en bbdd: " + ids.size());
        System.out.println("Ids en total: " + missingNotas.size());
        missingNotas.removeAll(ids);
        System.out.println("Ids tras quitar las de la bbdd: " + missingNotas.size());
        System.out.println(missingNotas);

        try {
            Nota nota = fetchNotasOneByOne.FetchNota(1);
            System.out.println(nota.getPostId());
        } catch (URISyntaxException e) {
            logger.log(Level.SEVERE, "Boom: ", e);
            throw e;
        } catch (InterruptedException interruptedException){
            logger.log(Level.SEVERE, "Hilo interrumpido: ", interruptedException);
            throw interruptedException;
        }

        logger.exiting(MeneameStatistics.class.getName(), Thread.currentThread().getStackTrace()[1].getMethodName());

    }

}
