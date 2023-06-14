package dao;
import pojo.Nota;

import java.sql.*;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NotasDAO {
    Logger logger = Logger.getLogger(NotasDAO.class.getName());
    public static final String ERROR_INSERT_NOTAS = "Error al insertar notas";

    public static final String CON_STRING = "jdbc:sqlite:NotasDb.db";
    public static final String INSERT_NOTA = "INSERT OR IGNORE INTO Notas (post_id,\"user\",\"timestamp\",referenced_users,html,\"text\") " +
            "VALUES (?,?,?,?,?,?); ";

    public static final String INSERT_PAGE = "INSERT OR REPLACE INTO search_data (page,\"timestamp\",maxId,minId)\n" +
            "\tVALUES (?,?,?,?);";

    public int insertNotas(List<Nota> notas) {
        logger.entering(getClass().getName(), getClass().getEnclosingMethod().getName());
        try (Connection connection = DriverManager.getConnection(CON_STRING);
             PreparedStatement ps = connection.prepareStatement(INSERT_NOTA)) {
            for (Nota nota : notas) {
                addInsertToPreparedStatement(ps, nota);
            }
            logger.info("Insertando notas");
            var resultCount = ps.executeBatch();
            long insertedCount = Arrays.stream(resultCount).filter(result -> result == 1).count();
            long ignoredCount = Arrays.stream(resultCount).filter(result -> result == 0).count();

            logger.log(Level.INFO, "{} notas insertadas y {} notas ignoradas por existir en base de datos", new Long[]{insertedCount, ignoredCount});
            logger.exiting(getClass().getName(), getClass().getEnclosingMethod().getName());

            return (int) insertedCount;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, ERROR_INSERT_NOTAS, e);
            logger.exiting(getClass().getName(), getClass().getEnclosingMethod().getName());
            return -1;
        }

    }

    private void addInsertToPreparedStatement(PreparedStatement ps, Nota nota) {
        logger.entering(getClass().getName(), getClass().getEnclosingMethod().getName());

        try {
            ps.setLong(1, nota.getPostId());
            ps.setString(2, nota.getUser());
            ps.setLong(3, nota.getTimestamp());
            ps.setString(4, String.join(" ", nota.getReferencedUsers()));
            ps.setString(5, nota.getHtml());
            ps.setString(6, nota.getText());
            ps.addBatch();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, ERROR_INSERT_NOTAS, e);
        }
        logger.exiting(getClass().getName(), getClass().getEnclosingMethod().getName());

    }

    public long insertPagina(int page, long[] idsCurrentPage) {
        logger.entering(getClass().getName(), getClass().getEnclosingMethod().getName());

        try (Connection connection = DriverManager.getConnection(CON_STRING);
             PreparedStatement ps = connection.prepareStatement(INSERT_PAGE)) {
            setInsertPageParameters(page, idsCurrentPage, ps);
            logger.exiting(getClass().getName(), getClass().getEnclosingMethod().getName());

            return ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, ERROR_INSERT_NOTAS, e);
            logger.exiting(getClass().getName(), getClass().getEnclosingMethod().getName());
            return -1;
        }
    }

    private void setInsertPageParameters(int page, long[] idsCurrentPage, PreparedStatement ps) {
        logger.entering(getClass().getName(), getClass().getEnclosingMethod().getName());

        try {
            ps.setLong(1, page);
            ps.setLong(2, System.currentTimeMillis());
            ps.setLong(3, idsCurrentPage[0]);
            ps.setLong(4, idsCurrentPage[1]);
            logger.log(Level.INFO, "Insertada página {}; maxId = {}, minId = {}.",  new long[] {page, idsCurrentPage[0], idsCurrentPage[1]});

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al insertar página", e);
        }
        logger.exiting(getClass().getName(), getClass().getEnclosingMethod().getName());

    }

    public long[] getMaxMinIdOnDatabase() {
        logger.entering(getClass().getName(), getClass().getEnclosingMethod().getName());

        long[] ids = new long[2];
        try (Connection connection = DriverManager.getConnection(CON_STRING)) {
            try (Statement st = connection.createStatement()) {
                ResultSet rs = st.executeQuery("SELECT MAX(post_id), MIN(post_id) FROM Notas");
                ids[0] = rs.getLong(1); // Nota más reciente almacenada
                ids[1] = rs.getLong(2); // Nota más antigua almacenada
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al recuperar ids", e);
        }
        logger.exiting(getClass().getName(), getClass().getEnclosingMethod().getName());

        return ids;
    }
}

