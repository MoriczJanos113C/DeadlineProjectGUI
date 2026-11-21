package deadlineProjectGUI;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DeadlineUpdater {

    private DeadlineUpdater() {
    }

    // ======================================================================
    //  3. TAG – Nemes Kristóf: szerkesztés, ismétlődés, lejárt kezelés, ellenőrzés
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

    public enum Recurrence {
        NONE("Nincs"), DAILY("Naponta"), WEEKLY("Hetente"), MONTHLY("Havonta"), YEARLY("Évente");

        private final String label;

        Recurrence(String label) { this.label = label; }

        @Override
        public String toString() { return label; }

        public LocalDateTime next(LocalDateTime d) {
            return switch (this) {
                case DAILY -> d.plusDays(1);
                case WEEKLY -> d.plusWeeks(1);
                case MONTHLY -> d.plusMonths(1);
                case YEARLY -> d.plusYears(1);
                case NONE -> d;
            };
        }

        public static Recurrence fromDb(String s) {
            if (s == null) return NONE;
            try { return valueOf(s); } catch (IllegalArgumentException e) { return NONE; }
        }
    }

    private static boolean columnReady = false;

    private static void ensureRecurrenceColumn() {
        if (columnReady) return;
        try (Connection c = Database.getConnection(); Statement st = c.createStatement()) {
            st.executeUpdate("ALTER TABLE deadlines ADD COLUMN IF NOT EXISTS "
                    + "recurrence VARCHAR(10) NOT NULL DEFAULT 'NONE'");
            columnReady = true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Recurrence deadline_get_recurrence(int id) {
        ensureRecurrenceColumn();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT recurrence FROM deadlines WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Recurrence.fromDb(rs.getString(1)) : Recurrence.NONE;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return Recurrence.NONE;
        }
    }

    public enum ExpiredResult { NOT_FOUND, NOT_EXPIRED, RESCHEDULED, STILL_EXPIRED }

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
        Deadline old = deadline_get(id);
        return deadline_edit(id, title, description, date, priority, done,
                old == null ? Recurrence.NONE : deadline_get_recurrence(id));
    }

    public static Map<String, String> deadline_edit(int id, String title, String description,
                                                    String date, String priority, LocalDate done,
                                                    Recurrence recurrence) {
        Map<String, String> errors = deadline_validate(title, date, priority);
        if (description == null || description.isBlank()) {
            errors.put(F_DESCRIPTION, REQUIRED_MESSAGE);
        } else if (description.strip().length() > 255) {
            errors.put(F_DESCRIPTION, "Legfeljebb 255 karakter lehet.");
        }
        if (!errors.containsKey(F_DATE)) {
            LocalDateTime newDate = parseDate(date);
            Deadline old = deadline_get(id);
            if (old != null) {
                boolean changed = !newDate.equals(old.getDate().truncatedTo(java.time.temporal.ChronoUnit.MINUTES));
                if (changed && isExpired(old, LocalDateTime.now())) {
                    errors.put(F_DATE, "Lejárt határidő dátuma nem módosítható.");
                } else if (changed && newDate.isBefore(LocalDateTime.now())) {
                    errors.put(F_DATE, "Múltbeli időpont nem adható meg.");
                }
            }
        }
        if (!errors.isEmpty()) return errors;
        ensureRecurrenceColumn();

        String sql = "UPDATE deadlines SET title = ?, description = ?, deadlines_date = ?, "
                + "priority = ?, done = ?, recurrence = ? WHERE id = ?";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, title.strip());
            ps.setString(2, description.strip());
            ps.setTimestamp(3, Timestamp.valueOf(parseDate(date)));
            ps.setString(4, priority);
            if (done == null) ps.setNull(5, Types.DATE); else ps.setDate(5, java.sql.Date.valueOf(done));
            ps.setString(6, (recurrence == null ? Recurrence.NONE : recurrence).name());
            ps.setInt(7, id);
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

    public static ExpiredResult deadline_handle_expired(int id) {
        Deadline d = deadline_get(id);
        if (d == null) return ExpiredResult.NOT_FOUND;
        LocalDateTime now = LocalDateTime.now();
        if (!isExpired(d, now)) return ExpiredResult.NOT_EXPIRED;
        Recurrence rec = deadline_get_recurrence(id);
        if (rec == Recurrence.NONE) return ExpiredResult.STILL_EXPIRED;

        LocalDateTime next = d.getDate();
        do { next = rec.next(next); } while (next.isBefore(now));

        String sql = "UPDATE deadlines SET deadlines_date = ?, done = NULL WHERE id = ?";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(next));
            ps.setInt(2, id);
            ps.executeUpdate();
            return ExpiredResult.RESCHEDULED;
        } catch (SQLException e) {
            e.printStackTrace();
            return ExpiredResult.STILL_EXPIRED;
        }
    }

    public static int deadline_handle_all_expired() {
        ensureRecurrenceColumn();
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT id FROM deadlines WHERE done IS NULL AND recurrence <> 'NONE' "
                + "AND deadlines_date < NOW()";
        try (Connection c = Database.getConnection(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) ids.add(rs.getInt(1));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        int moved = 0;
        for (int id : ids) if (deadline_handle_expired(id) == ExpiredResult.RESCHEDULED) moved++;
        return moved;
    }

    public static void deadline_set_recurrence(int id, Recurrence rec) {
        ensureRecurrenceColumn();
        String sql = "UPDATE deadlines SET recurrence = ? WHERE id = ?";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, (rec == null ? Recurrence.NONE : rec).name());
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static LocalDateTime deadline_spawn_next_if_recurring(int id) {
        Deadline d = deadline_get(id);
        if (d == null) return null;
        Recurrence rec = deadline_get_recurrence(id);
        if (rec == Recurrence.NONE) return null;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next = d.getDate();
        do { next = rec.next(next); } while (next.isBefore(now));

        try {
            int newId = Deadline.deadline_add(0, d.getTitle(), d.getDescription(), next, d.getPriority(), null);
            deadline_set_recurrence(newId, rec);
            deadline_set_recurrence(id, Recurrence.NONE);
            return next;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}