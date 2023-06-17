package dao;
import pojo.Nota;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.LongStream;

public class NotasDAO {
    Logger logger = Logger.getLogger(NotasDAO.class.getName());
    public static final String ERROR_INSERT_NOTAS = "Error al insertar notas";

    public static final String INSERT_NOTA = """
        INSERT INTO public.notas (post_id,"user","timestamp",referenced_users,html,"text")
         VALUES (?,?,?,?,?,?)
         ON CONFLICT (post_id) DO NOTHING;
        """;

    //public static final String INSERT_PAGE = "INSERT OR REPLACE INTO search_data (page,\"timestamp\",maxId,minId)\n" +
    //        "\tVALUES (?,?,?,?);";

    public int insertNotas(List<Nota> notas) {
        logger.entering(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName());
        long minId = notas.stream().min(Comparator.comparing(Nota::getPostId)).orElseThrow().getPostId();
        long maxId = notas.stream().max(Comparator.comparing(Nota::getPostId)).orElseThrow().getPostId();
        List<Long> nullIds = new ArrayList<>(LongStream.rangeClosed(minId, maxId).boxed().toList());
        //nullIds.removeIf(nullId -> notas.stream().mapToLong(Nota::getPostId).anyMatch(id -> id == nullId));

        try (Connection connection = DriverManager.getConnection(DbConstants.CON_STRING, DbConstants.CON_USER, DbConstants.CON_PASS);
             PreparedStatement ps = connection.prepareStatement(INSERT_NOTA)) {
            for (Nota nota : notas) {
                addInsertToPreparedStatement(ps, nota);
                nullIds.removeIf(nullId -> nullId == nota.getPostId());
            }
            for (Long nullId: nullIds) {
                Nota notaNull = new Nota();
                notaNull.setPostId(nullId);
                addInsertToPreparedStatement(ps, notaNull);
            }

            logger.info("Insertando notas");
            var resultCount = ps.executeBatch();
            long insertedCount = Arrays.stream(resultCount).filter(result -> result == 1).count();
            long ignoredCount = Arrays.stream(resultCount).filter(result -> result == 0).count();
            long deletedCount = nullIds.size();

            logger.log(Level.INFO, () ->
                    insertedCount + " notas nuevas, "
                    + ignoredCount + " notas ignoradas y "
                    + deletedCount + " notas no recuperables.");
            logger.exiting(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName());

            return (int) insertedCount;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, ERROR_INSERT_NOTAS, e);
            logger.exiting(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName());
            return -1;
        }

    }

    private void addInsertToPreparedStatement(PreparedStatement ps, Nota nota) {
        logger.entering(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName());

        try {
            ps.setLong(1, nota.getPostId());
            if (nota.getUser() == null) {
                ps.setNull(2, Types.NULL);
            } else {
                ps.setString(2, nota.getUser());
            }
            ps.setLong(3, nota.getTimestamp());
            if (nota.getReferencedUsers() == null) {
                ps.setNull(4, Types.NULL);
            } else {
                ps.setString(4, String.join(" ", nota.getReferencedUsers()));
            }
            if (nota.getHtml() == null) {
                ps.setNull(5, Types.NULL);
            } else {
                ps.setString(5, nota.getHtml());
            }
            if (nota.getText() == null) {
                ps.setNull(6, Types.NULL);
            } else {
                ps.setString(6, nota.getText());
            }
            ps.addBatch();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, ERROR_INSERT_NOTAS, e);
        }
        logger.exiting(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName());

    }

/*    public long insertPagina(int page, long[] idsCurrentPage) {
        logger.entering(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName());

        try (Connection connection = DriverManager.getConnection(Constants.CON_STRING);
             PreparedStatement ps = connection.prepareStatement(INSERT_PAGE)) {
            setInsertPageParameters(page, idsCurrentPage, ps);
            logger.exiting(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName());

            return ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, ERROR_INSERT_NOTAS, e);
            logger.exiting(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName());
            return -1;
        }
    }*/

    private void setInsertPageParameters(int page, long[] idsCurrentPage, PreparedStatement ps) {
        logger.entering(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName());

        try {
            ps.setLong(1, page);
            ps.setLong(2, System.currentTimeMillis());
            ps.setLong(3, idsCurrentPage[0]);
            ps.setLong(4, idsCurrentPage[1]);
            logger.log(Level.INFO, () -> "Insertada página " 
                    + page + "; maxId = " 
                    + idsCurrentPage[0] + ", minId = "
                    + idsCurrentPage[1] +  ".");

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al insertar página", e);
        }
        logger.exiting(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName());

    }

    public long[] getMaxMinIdOnDatabase() {
        logger.entering(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName());

        long[] ids = new long[2];
        try (Connection connection = DriverManager.getConnection(DbConstants.CON_STRING, DbConstants.CON_USER, DbConstants.CON_PASS);) {
            try (Statement st = connection.createStatement()) {
                ResultSet rs = st.executeQuery("SELECT MAX(post_id), MIN(post_id) FROM Notas");
                rs.next();
                ids[0] = rs.getLong(1); // Nota más reciente almacenada
                ids[1] = rs.getLong(2); // Nota más antigua almacenada
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al recuperar ids", e);
        }
        logger.exiting(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName());

        return ids;
    }
}

