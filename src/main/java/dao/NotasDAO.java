package dao;
import pojo.Nota;

import java.sql.*;
import java.util.Arrays;
import java.util.List;

public class NotasDAO {

    public static final String CON_STRING = "jdbc:sqlite:NotasDb.db";
    public static final String INSERT_NOTA = "INSERT OR IGNORE INTO Notas (post_id,\"user\",\"timestamp\",referenced_users,html,\"text\") " +
            "VALUES (?,?,?,?,?,?); ";

    public static final String INSERT_PAGE = "INSERT OR REPLACE INTO search_data (page,\"timestamp\",maxId,minId)\n" +
            "\tVALUES (?,?,?,?);";

    public int insertNotas(List<Nota> notas) {
        try (Connection connection = DriverManager.getConnection(CON_STRING)) {
            PreparedStatement ps = connection.prepareStatement(INSERT_NOTA);
            for (Nota nota : notas) {
                try {
                    ps.setLong(1, nota.getPostId());
                    ps.setString(2, nota.getUser());
                    ps.setLong(3, nota.getTimestamp());
                    ps.setString(4, String.join(" ", nota.getReferencedUsers()));
                    ps.setString(5, nota.getHtml());
                    ps.setString(6, nota.getText());
                    ps.addBatch();
                } catch (SQLException ex) {
                    System.out.println("Error al insertar notas: " + ex);
                }
            }
            System.out.println("Insertando notas");
            var resultCount = ps.executeBatch();
            Long insertedCount = Arrays.stream(resultCount).filter(result -> result == 1).count();
            Long ignoredCount = Arrays.stream(resultCount).filter(result -> result == 0).count();
            System.out.printf("%d notas insertadas y %d notas ignoradas por existir en base de datos\n", insertedCount, ignoredCount);
            ps.close();
            return insertedCount.intValue();
        } catch (SQLException e) {
            System.out.println("Error en insert: " + e);
            return -1;
        }
    }

    public long insertPagina(int page, long[] idsCurrentPage) {
        try (Connection connection = DriverManager.getConnection(CON_STRING);
             PreparedStatement ps = connection.prepareStatement(INSERT_PAGE)) {
            try {
                ps.setLong(1, page);
                ps.setLong(2, System.currentTimeMillis());
                ps.setLong(3, idsCurrentPage[0]);
                ps.setLong(4, idsCurrentPage[1]);
                System.out.printf("Insertada página %d; maxId = %d, minId = %d.\n",
                        page, idsCurrentPage[0], idsCurrentPage[1]);

            } catch (SQLException ex) {
                System.out.println("Error al insertar página: " + ex);
            }
            return ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error en insert: " + e);
            return -1;
        }
    }

    public long[] getMaxMinIdOnDatabase() {
        long[] ids = new long[2];
        try (Connection connection = DriverManager.getConnection(CON_STRING)) {
            try (Statement st = connection.createStatement()) {
                ResultSet rs = st.executeQuery("SELECT MAX(post_id), MIN(post_id) FROM Notas");
                ids[0] = rs.getLong(1); // Nota más reciente almacenada
                ids[1] = rs.getLong(2); // Nota más antigua almacenada
            }
        } catch (SQLException e) {
            System.out.println("Error al recuperar ids: " + e);
        }
        return ids;
    }
}

