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

    public static List<Deadline> loadBetween(LocalDateTime from, LocalDateTime to) throws SQLException {
        String sql = "SELECT id, title, description, deadlines_date, priority, done "
                + "FROM deadlines WHERE deadlines_date >= ? AND deadlines_date < ? "
                + "ORDER BY deadlines_date";
        List<Deadline> list = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(from));
            ps.setTimestamp(2, Timestamp.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    java.sql.Date done = rs.getDate("done");
                    LocalDate doneDate = (done == null) ? null : done.toLocalDate();
                    list.add(new Deadline(
                            rs.getInt("id"),
                            rs.getString("title"),
                            rs.getString("description"),
                            rs.getTimestamp("deadlines_date").toLocalDateTime(),
                            rs.getString("priority"),
                            doneDate));
                }
            }
        }
        return list;
    }
}
