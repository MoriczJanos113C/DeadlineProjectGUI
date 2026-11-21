package deadlineProjectGUI;

import javax.swing.*;
import java.awt.Component;
import java.awt.Frame;

public class DeadlineActions {

    private DeadlineActions() {
    }

    public static void showAddDialog(Frame owner, Runnable onSaved) {
        AddDeadlineDialog dialog = new AddDeadlineDialog(owner);
        dialog.setVisible(true);
        if (dialog.isSaved() && onSaved != null) {
            onSaved.run();
        }
    }


    public static void deleteDeadline(Component parent, Deadline d, Runnable onDone) {
        int choice = JOptionPane.showConfirmDialog(parent,
                "Biztosan törölni szeretnéd ezt a határidőt?\n\"" + d.getTitle() + "\"",
                "Törlés megerősítése",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            Deadline.deadline_delete(d.getId());
            if (onDone != null) {
                onDone.run();
            }
        } catch (java.sql.SQLException ex) {
            showError(parent, "Nem sikerült törölni a határidőt!\n" + ex.getMessage());
        }
    }

    public static void setDone(Component parent, Deadline d, boolean isDone, Runnable onDone) {
        try {
            Deadline.deadline_done(d.getId(), isDone);
        } catch (java.sql.SQLException ex) {
            showError(parent, "Nem sikerült menteni az állapotot!\n" + ex.getMessage());
        }
        if (onDone != null) {
            onDone.run();
        }
    }

    private static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Hiba", JOptionPane.ERROR_MESSAGE);
    }
}
