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
            JOptionPane.showMessageDialog(parent,
                    "A határidő törölve lett.",
                    "Törlés", JOptionPane.INFORMATION_MESSAGE);
            if (onDone != null) {
                onDone.run();
            }
        } catch (java.sql.SQLException ex) {
            showError(parent, "Nem sikerült törölni a határidőt!\n" + ex.getMessage());
        }
    }

    public static void setDone(Component parent, Deadline d, boolean isDone, Runnable onDone) {
        if (!isDone) {
            return; // kész állapot nem vonható vissza
        }
        int choice = JOptionPane.showConfirmDialog(parent,
                "Biztosan kész lett?\n\"" + d.getTitle() + "\"\n"
                        + "A kész határidő később nem szerkeszthető.",
                "Késznek jelölés",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            if (onDone != null) {
                onDone.run(); // újrarajzolás, a pipa visszaáll üresre
            }
            return;
        }
        try {
            Deadline.deadline_done(d.getId(), true);
            java.time.LocalDateTime next = DeadlineUpdater.deadline_spawn_next_if_recurring(d.getId());
            if (next != null) {
                JOptionPane.showMessageDialog(parent,
                        "Kész! Az ismétlődő határidő következő alkalma létrejött:\n"
                                + next.format(DeadlineUpdater.DATE_FORMAT),
                        "Ismétlődés", JOptionPane.INFORMATION_MESSAGE);
            }
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