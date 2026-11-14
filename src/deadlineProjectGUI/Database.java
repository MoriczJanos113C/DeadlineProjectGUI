package deadlineProjectGUI;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {

    private static final String SERVER_URL = "jdbc:mariadb://localhost:3306/";
    private static final String DB_NAME = "hataridonaplo";
    private static final String URL = SERVER_URL + DB_NAME;
    private static final String USER = "root";
    private static final String PASSWORD = "root";

    private Database() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void init() throws SQLException {
        try (Connection c = DriverManager.getConnection(SERVER_URL, USER, PASSWORD);
             Statement st = c.createStatement()) {
            st.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB_NAME
                    + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        }
        try (Connection c = getConnection(); Statement st = c.createStatement()) {
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS deadlines (
                        id             INT          NOT NULL AUTO_INCREMENT,
                        title          VARCHAR(100) NOT NULL,
                        description    VARCHAR(255) NOT NULL,
                        deadlines_date DATETIME     NOT NULL,
                        priority       VARCHAR(10)  NOT NULL,
                        done           DATE         DEFAULT NULL,
                        PRIMARY KEY (id)
                    )
                    """);
        }
    }
}
