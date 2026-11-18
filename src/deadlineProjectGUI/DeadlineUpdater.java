package deadlineProjectGUI;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DeadlineUpdater {

    private DeadlineUpdater() {
    }

    // ======================================================================
    //  Nemes Kristóf: szerkesztés, lejárt kezelés, hibaüzenetek
    // ======================================================================

    public static final List<String> PRIORITIES = List.of("Alacsony", "Közepes", "Nagy");

    public static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("uuuu/MM/dd - HH:mm").withResolverStyle(ResolverStyle.STRICT);
    public static final String DATE_PATTERN_TEXT = "ÉÉÉÉ/HH/NN - ÓÓ:PP";

    public static final String F_TITLE = "title";
    public static final String F_DESCRIPTION = "description";
    public static final String F_DATE = "date";
    public static final String F_PRIORITY = "priority";
    public static final String F_DB = "db";

    private static final String REQUIRED_MESSAGE = "Kérjük, töltse ki a kötelező mezőt.";

    public static Deadline deadline_get(int id) {
        String sql = "SELECT id, title, description, deadlines_date, priority, done "
                + "FROM deadlines WHERE id = ?";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                java.sql.Date dn = rs.getDate("done");
                return new Deadline(rs.getInt("id"), rs.getString("title"), rs.getString("description"),
                        rs.getTimestamp("deadlines_date").toLocalDateTime(), rs.getString("priority"),
                        dn == null ? null : dn.toLocalDate());
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Map<String, String> deadline_validate(String title, String date, String priority) {
        Map<String, String> errors = new LinkedHashMap<>();

        if (title == null || title.isBlank()) {
            errors.put(F_TITLE, REQUIRED_MESSAGE);
        } else if (title.strip().length() > 100) {
            errors.put(F_TITLE, "Legfeljebb 100 karakter lehet.");
        }

        if (date == null || date.isBlank()) {
            errors.put(F_DATE, REQUIRED_MESSAGE);
        } else if (parseDate(date) == null) {
            errors.put(F_DATE, "Hibás formátum vagy dátum. Példa: 2026/11/20 - 14:30");
        }

        if (priority == null || priority.isBlank()) {
            errors.put(F_PRIORITY, "Kérjük, válasszon egyet.");
        } else if (!PRIORITIES.contains(priority)) {
            errors.put(F_PRIORITY, "Ismeretlen prioritás.");
        }
        return errors;
    }

    public static LocalDateTime parseDate(String text) {
        if (text == null) return null;
        try {
            return LocalDateTime.parse(text.strip(), DATE_FORMAT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public static Map<String, String> deadline_edit(int id, String title, String description,
                                                    String date, String priority, LocalDate done) {
        Map<String, String> errors = deadline_validate(title, date, priority);
        if (description == null || description.isBlank()) {
            errors.put(F_DESCRIPTION, REQUIRED_MESSAGE);
        } else if (description.strip().length() > 255) {
            errors.put(F_DESCRIPTION, "Legfeljebb 255 karakter lehet.");
        }
        if (!errors.isEmpty()) return errors;

        String sql = "UPDATE deadlines SET title = ?, description = ?, deadlines_date = ?, "
                + "priority = ?, done = ? WHERE id = ?";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, title.strip());
            ps.setString(2, description.strip());
            ps.setTimestamp(3, Timestamp.valueOf(parseDate(date)));
            ps.setString(4, priority);
            if (done == null) ps.setNull(5, Types.DATE); else ps.setDate(5, java.sql.Date.valueOf(done));
            ps.setInt(6, id);
            if (ps.executeUpdate() == 0) {
                errors.put(F_DB, "A határidő nem található, lehet, hogy közben törölték.");
            }
        } catch (SQLException e) {
            errors.put(F_DB, "Nem sikerült menteni az adatbázisba.");
            errors.put("db_detail", e.getMessage());
        }
        return errors;
    }

    public static boolean deadline_is_expired(int id) {
        Deadline d = deadline_get(id);
        return d != null && isExpired(d, LocalDateTime.now());
    }

    static boolean isExpired(Deadline d, LocalDateTime now) {
        return d.getDate() != null && !d.isDone() && d.getDate().isBefore(now);
    }
}