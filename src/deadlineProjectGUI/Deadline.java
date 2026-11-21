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

public class Deadline {

    private int id;
    private String title;
    private String description;
    private LocalDateTime date;
    private String priority;
    private LocalDate done;

    public Deadline(int id, String title, String description,
                    LocalDateTime date, String priority, LocalDate done) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.date = date;
        this.priority = priority;
        this.done = done;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public LocalDate getDone() { return done; }
    public void setDone(LocalDate done) { this.done = done; }
    public boolean isDone() { return done != null; }

    public static int deadline_add(int id, String title, String description,
                                   LocalDateTime date, String priority, LocalDate done)
            throws SQLException {
        String sql = (id > 0)
                ? "INSERT INTO deadlines (id, title, description, deadlines_date, priority, done) VALUES (?, ?, ?, ?, ?, ?)"
                : "INSERT INTO deadlines (title, description, deadlines_date, priority, done) VALUES (?, ?, ?, ?, ?)";

        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int i = 1;
            if (id > 0) {
                ps.setInt(i++, id);
            }
            ps.setString(i++, title);
            ps.setString(i++, description);
            ps.setTimestamp(i++, Timestamp.valueOf(date));
            ps.setString(i++, priority);
            if (done == null) {
                ps.setNull(i, Types.DATE);
            } else {
                ps.setDate(i, java.sql.Date.valueOf(done));
            }
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            return id;
        }
    }

    //törlés
    public static boolean deadline_delete(int id) throws SQLException {
        String sql = "DELETE FROM deadlines WHERE id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    //szerkesztés
    public static boolean deadline_done(int id, boolean isDone) throws SQLException {
        String sql = "UPDATE deadlines SET done = ? WHERE id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (isDone) {
                ps.setDate(1, java.sql.Date.valueOf(LocalDate.now()));
            } else {
                ps.setNull(1, Types.DATE);
            }
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
    }


}
