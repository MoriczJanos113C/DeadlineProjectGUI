package deadlineProjectGUI;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DeadlineLoader {

    private DeadlineLoader() {
    }

    public static List<Deadline> loadFiltered(
            LocalDateTime from,
            LocalDateTime to,
            String priorityFilter,
            String statusFilter,
            String searchText) throws SQLException {

        StringBuilder sql = new StringBuilder("""
            SELECT id,
                   title,
                   description,
                   deadlines_date,
                   priority,
                   done
            FROM deadlines
            WHERE deadlines_date >= ?
              AND deadlines_date < ?
            """);

        List<Object> params = new ArrayList<>();

        params.add(Timestamp.valueOf(from));
        params.add(Timestamp.valueOf(to));

        // Prioritás szűrés

        switch (priorityFilter) {
            case "Alacsony" -> {
                sql.append(" AND LOWER(priority) = ?");
                params.add("alacsony");
            }

            case "Normál" -> {
                sql.append(" AND LOWER(priority) = ?");
                params.add("közepes");
            }

            case "Magas" -> {
                // Ha az adatbázisban "nagy" szerepel,
                // akkor ezt hagyd így!
                sql.append(" AND LOWER(priority) = ?");
                params.add("nagy");
            }
        }

        // Státusz szűrés

        switch (statusFilter) {

            case "Aktív" ->
                    sql.append(" AND done IS NULL");

            case "Kész" ->
                    sql.append(" AND done IS NOT NULL");

            case "Lejárt" ->
                    sql.append("""
                        AND done IS NULL
                        AND deadlines_date < CURRENT_TIMESTAMP
                    """);
        }

        // Keresés title alapján

        if (searchText != null && !searchText.isBlank()) {
            sql.append(" AND LOWER(title) LIKE ?");
            params.add("%" + searchText.trim().toLowerCase() + "%");
        }

        sql.append(" ORDER BY deadlines_date");

        List<Deadline> list = new ArrayList<>();

        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    java.sql.Date done = rs.getDate("done");
                    LocalDate doneDate =
                            done == null ? null : done.toLocalDate();

                    list.add(new Deadline(
                            rs.getInt("id"),
                            rs.getString("title"),
                            rs.getString("description"),
                            rs.getTimestamp("deadlines_date")
                                    .toLocalDateTime(),
                            rs.getString("priority"),
                            doneDate
                    ));
                }
            }
        }

        return list;
    }
}