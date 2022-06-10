package dev.morling.demos.loom;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class TodoRepository {

    private static HikariDataSource ds;

    private static final String GET_TODO_BY_ID_QUERY = "select id, title from todo where id = ?";

    static {
        HikariConfig config = new HikariConfig();

        config.setJdbcUrl("jdbc:postgresql://localhost:5432/tododb?currentSchema=todo");
        config.setUsername("todouser");
        config.setPassword("todopw");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        ds = new HikariDataSource(config);
    }

    public String getTodo(long todoId) {
        try (Connection con = getConnection();
                PreparedStatement pst = con.prepareStatement(GET_TODO_BY_ID_QUERY)) {
            pst.setLong(1, todoId);

            try (ResultSet rs = pst.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                String response = """
                        {
                          "id" : %s,
                          "title" : "%s"
                        }
                        """
                        .formatted(rs.getInt(1), rs.getString(2));

                return response;
            }
        }
        catch (SQLException e) {
            throw new RuntimeException("Query failed", e);
        }
    }

    private static Connection getConnection() throws SQLException {
        return ds.getConnection();
    }
}
