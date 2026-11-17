package hatarido;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Deadline {

    public static final String PRIORITY_LOW = "Low";
    public static final String PRIORITY_NORMAL = "Normal";
    public static final String PRIORITY_HIGH = "High";
    public static final List<String> PRIORITIES = List.of(PRIORITY_LOW, PRIORITY_NORMAL, PRIORITY_HIGH);

    public enum Recurrence {
        NONE("Nincs"), DAILY("Naponta"), WEEKLY("Hetente"), MONTHLY("Havonta"), YEARLY("Évente");

        private final String label;
        Recurrence(String label) { this.label = label; }
        @Override public String toString() { return label; }

        public LocalDate next(LocalDate d) {
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

    public enum ExpiredResult { NOT_FOUND, NOT_EXPIRED, RESCHEDULED, STILL_EXPIRED }

    public static final String F_TITLE = "title";
    public static final String F_DESCRIPTION = "description";
    public static final String F_DATE = "date";
    public static final String F_PRIORITY = "priority";
    public static final String F_DB = "db";

    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("uuuu.MM.dd");
    private static final DateTimeFormatter DATE_FORMAT_ISO = DateTimeFormatter.ISO_LOCAL_DATE; // yyyy-MM-dd is jó

    /** Adattagok */

    private int id;
    private String title;
    private String description;
    private LocalDate date;
    private String priority;
    private LocalDate done;
    private Recurrence recurrence = Recurrence.NONE;

    public Deadline() { }

    public Deadline(int id, String title, String description, LocalDate date,
                    String priority, LocalDate done, Recurrence recurrence) {
        this.id = id; this.title = title; this.description = description; this.date = date;
        this.priority = priority; this.done = done;
        this.recurrence = recurrence == null ? Recurrence.NONE : recurrence;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public LocalDate getDate() { return date; }
    public String getPriority() { return priority; }
    public LocalDate getDone() { return done; }
    public Recurrence getRecurrence() { return recurrence; }
    public boolean isDone() { return done != null; }

    /** ID alapján beolvas egy határidőt */

    public static Deadline deadline_get(int id) {
        String sql = "SELECT id, title, description, deadlines_date, priority, done, recurrence "
                + "FROM deadlines WHERE id = ?";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Date d = rs.getDate("deadlines_date");
                Date dn = rs.getDate("done");
                return new Deadline(rs.getInt("id"), rs.getString("title"), rs.getString("description"),
                        d == null ? null : d.toLocalDate(), rs.getString("priority"),
                        dn == null ? null : dn.toLocalDate(),
                        Recurrence.fromDb(rs.getString("recurrence")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
    /** Adatok ellenőrzése mentés előtt, Név, dátum, priorítás */

    public static Map<String, String> deadline_validate(String title, String date, String priority) {
        Map<String, String> errors = new LinkedHashMap<>();

        if (title == null || title.isBlank()) {
            errors.put(F_TITLE, "A név kitöltése kötelező!");
        } else if (title.strip().length() > 255) {
            errors.put(F_TITLE, "A név legfeljebb 255 karakter lehet!");
        }

        if (date == null || date.isBlank()) {
            errors.put(F_DATE, "A dátum megadása kötelező!");
        } else if (parseDate(date) == null) {
            errors.put(F_DATE, "Hibás dátumformátum! Helyes: éééé.hh.nn (pl. 2026.10.15)");
        }

        if (priority == null || priority.isBlank()) {
            errors.put(F_PRIORITY, "Válassz prioritást!");
        } else if (!PRIORITIES.contains(priority)) {
            errors.put(F_PRIORITY, "Ismeretlen prioritás! (Low / Normal / High)");
        }
        return errors;
    }

    public static LocalDate parseDate(String text) {
        if (text == null) return null;
        String t = text.strip();
        for (DateTimeFormatter f : List.of(DATE_FORMAT, DATE_FORMAT_ISO)) {
            try {
                return LocalDate.parse(t, f.withResolverStyle(java.time.format.ResolverStyle.STRICT));
            } catch (DateTimeParseException ignored) { }
        }
        return null;
    }
    /** Határidő szerkesztése */

    public static Map<String, String> deadline_edit(int id, String title, String description,
                                                    String date, String priority, LocalDate done) {
        Deadline old = deadline_get(id);
        Recurrence rec = old == null ? Recurrence.NONE : old.getRecurrence();
        return deadline_edit(id, title, description, date, priority, done, rec);
    }

    public static Map<String, String> deadline_edit(int id, String title, String description,
                                                    String date, String priority, LocalDate done,
                                                    Recurrence recurrence) {
        Map<String, String> errors = deadline_validate(title, date, priority);
        if (!errors.isEmpty()) return errors;

        String sql = "UPDATE deadlines SET title = ?, description = ?, deadlines_date = ?, "
                + "priority = ?, done = ?, recurrence = ? WHERE id = ?";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, title.strip());
            ps.setString(2, description == null ? "" : description.strip());
            ps.setDate(3, Date.valueOf(parseDate(date)));
            ps.setString(4, priority);
            if (done == null) ps.setNull(5, Types.DATE); else ps.setDate(5, Date.valueOf(done));
            ps.setString(6, (recurrence == null ? Recurrence.NONE : recurrence).name());
            ps.setInt(7, id);
            if (ps.executeUpdate() == 0) {
                errors.put(F_DB, "A határidő nem található (id: " + id + "), lehet, hogy közben törölték.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            errors.put(F_DB, "Adatbázis hiba mentés közben: " + e.getMessage());
        }
        return errors;
    }
    /** Lejárt-e a határidő? */

    public static boolean deadline_is_expired(int id) {
        Deadline d = deadline_get(id);
        return d != null && isExpired(d, LocalDate.now());
    }

    static boolean isExpired(Deadline d, LocalDate today) {
        return d.getDate() != null && !d.isDone() && d.getDate().isBefore(today);
    }

    /** Lejárt határidő kezelése */

    public static ExpiredResult deadline_handle_expired(int id) {
        Deadline d = deadline_get(id);
        if (d == null) return ExpiredResult.NOT_FOUND;
        LocalDate today = LocalDate.now();
        if (!isExpired(d, today)) return ExpiredResult.NOT_EXPIRED;
        if (d.getRecurrence() == Recurrence.NONE) return ExpiredResult.STILL_EXPIRED;

        LocalDate next = d.getDate();
        do { next = d.getRecurrence().next(next); } while (next.isBefore(today));

        String sql = "UPDATE deadlines SET deadlines_date = ?, done = NULL WHERE id = ?";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(next));
            ps.setInt(2, id);
            ps.executeUpdate();
            return ExpiredResult.RESCHEDULED;
        } catch (SQLException e) {
            e.printStackTrace();
            return ExpiredResult.STILL_EXPIRED;
        }
    }
    /** Induláskor vagy frissítéskor: az összes lejárt ismétlődő határidő továbbléptetése. */

    public static int deadline_handle_all_expired() {
        int moved = 0;
        String sql = "SELECT id FROM deadlines WHERE done IS NULL AND recurrence <> 'NONE' "
                + "AND deadlines_date < CURRENT_DATE";
        List<Integer> ids = new ArrayList<>();
        try (Connection c = Database.getConnection(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) ids.add(rs.getInt(1));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        for (int id : ids) if (deadline_handle_expired(id) == ExpiredResult.RESCHEDULED) moved++;
        return moved;
    }
}