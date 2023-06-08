package dao;

import pojo.Nota;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


public class NotasDAO {

    public static final String CON_STRING = "jdbc:sqlite:NotasDb.db";
    public static final String INSERT_NOTA = "INSERT OR IGNORE INTO Notas (post_id,\"user\",\"timestamp\",referenced_users,html,\"text\") " +
            "VALUES (?,?,?,?,?,?); ";

    public int insert(List<Nota> notas) {
        AtomicInteger count = new AtomicInteger();
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
            int insertCount = Arrays.stream(resultCount).filter(result -> result >= 0).sum();
            System.out.println(insertCount + " notas insertadas");
            ps.close();
            return insertCount;
        } catch (SQLException e) {
            System.out.println("Error en insert: " + e);
            return -1;
        }
    }


}
