package deadlineProjectGUI;

import javax.swing.*;
import java.sql.SQLException;


public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                Database.init();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(null,
                        "Nem sikerült csatlakozni a MariaDB-hez!\n" + ex.getMessage(),
                        "Adatbázis hiba", JOptionPane.ERROR_MESSAGE);
                return;
            }
            new MainFrame().setVisible(true);
        });
    }
}
