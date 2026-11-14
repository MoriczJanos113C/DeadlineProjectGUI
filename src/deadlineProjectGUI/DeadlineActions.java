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

    private static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Hiba", JOptionPane.ERROR_MESSAGE);
    }
}
