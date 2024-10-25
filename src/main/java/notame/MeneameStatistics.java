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

        Set<Long> missingNotas = LongStream.rangeClosed(1,3654984).boxed().collect(Collectors.toSet());
        System.out.println("Ids en bbdd: " + ids.size());
        System.out.println("Ids en total: " + missingNotas.size());
        missingNotas.removeAll(ids);
        System.out.println("Ids tras quitar las de la bbdd: " + missingNotas.size());
        System.out.println(missingNotas);

        try {
            long restantes = missingNotas.size();
            for (Long missingNota : missingNotas){
                Nota nota = fetchNotasOneByOne.FetchNota(missingNota);
                if (nota == null){
                    nota = new Nota();
                    nota.setPostId(missingNota);
                }
                notasDAO.insertSingleNota(nota);
                restantes--;
                logger.info("Quedan " + restantes + " notas");
            }
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
