package deadlineProjectGUI;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

public class DeadlineEditDialog extends JDialog {

    private static final Color WINDOW_BG = new Color(0xB0B0B0);
    private static final Color TITLE_BAR = new Color(0x3E5063);
    private static final Color FIELD_BG = new Color(0x2B3A4A);
    private static final Color TEXT_DARK = new Color(0x2B3A4A);
    private static final Color HINT = new Color(0x8FA0B0);
    private static final Color ERROR = new Color(0xC8102E);
    private static final Color EXPIRED = new Color(0xE65100);   // lejárt: narancssárga
    private static final Color BLUE = new Color(0x4A7AAB);
    private static final Color GRAY = new Color(0x7B8D9E);

    private static final Border FIELD_BORDER = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(FIELD_BG, 2), BorderFactory.createEmptyBorder(5, 7, 5, 7));
    private static final Border FIELD_ERROR_BORDER = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ERROR, 2), BorderFactory.createEmptyBorder(5, 7, 5, 7));
    private static final Border COMBO_BORDER = BorderFactory.createLineBorder(FIELD_BG, 2);
    private static final Border COMBO_ERROR_BORDER = BorderFactory.createLineBorder(ERROR, 2);

    private final int deadlineId;
    private Deadline current;
    private boolean saved = false;
    private boolean openable = true;

    private final JTextField titleField = new JTextField(28);
    private final JTextField descriptionField = new JTextField(28);
    private final JTextField dateField = new JTextField(18);
    private final JComboBox<String> priorityBox = new JComboBox<>(DeadlineUpdater.PRIORITIES.toArray(new String[0]));

    private final JLabel titleError = createErrorLabel();
    private final JLabel descriptionError = createErrorLabel();
    private final JLabel dateError = createErrorLabel();
    private final JLabel priorityError = createErrorLabel();
    private final JLabel generalError = createErrorLabel();
    private final JLabel expiredInfo = new JLabel(" ");
    private FlatButton expiredButton;

    public DeadlineEditDialog(Frame owner, int deadlineId) {
        super(owner, "Határidő szerkesztése", true);
        this.deadlineId = deadlineId;

        current = DeadlineUpdater.deadline_get(deadlineId);
        if (current == null) {
            openable = false;
            JOptionPane.showMessageDialog(owner,
                    "A határidő nem található, lehet, hogy közben törölték.",
                    "Hiba", JOptionPane.ERROR_MESSAGE);
            return;
        }

        buildUi();
        fillFields(current);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    public static void open(Frame owner, int deadlineId, Runnable onSaved) {
        DeadlineEditDialog dialog = new DeadlineEditDialog(owner, deadlineId);
        if (!dialog.isOpenable()) {
            return;
        }
        dialog.setVisible(true);
        if (dialog.isSaved() && onSaved != null) {
            onSaved.run();
        }
    }

    public boolean isOpenable() {
        return openable;
    }

    public boolean isSaved() {
        return saved;
    }

    // ---------------------------------------------------------------- felépítés

    private void buildUi() {
        setUndecorated(true);

        styleTextField(titleField);
        styleTextField(descriptionField);
        styleTextField(dateField);
        styleComboBox(priorityBox, 185);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(WINDOW_BG);
        root.setBorder(BorderFactory.createLineBorder(TITLE_BAR, 1));
        root.add(buildTitleBar(), BorderLayout.NORTH);
        root.add(buildForm(), BorderLayout.CENTER);
        root.add(buildButtons(), BorderLayout.SOUTH);
        setContentPane(root);

        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private JComponent buildTitleBar() {
        JPanel bar = new JPanel();
        bar.setBackground(TITLE_BAR);
        bar.setPreferredSize(new Dimension(0, 28));

        final Point[] start = new Point[1];
        MouseAdapter drag = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                start[0] = e.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                setLocation(e.getXOnScreen() - start[0].x, e.getYOnScreen() - start[0].y);
            }
        };
        bar.addMouseListener(drag);
        bar.addMouseMotionListener(drag);
        return bar;
    }

    private JComponent buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(BorderFactory.createEmptyBorder(12, 22, 4, 22));

        expiredInfo.setFont(new Font("Segoe UI", Font.BOLD, 14));
        expiredInfo.setForeground(EXPIRED);
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.gridwidth = 2;
        gc.anchor = GridBagConstraints.WEST;
        form.add(expiredInfo, gc);

        addRow(form, 1, "Feladat neve", titleField, titleError, true);
        addRow(form, 2, "Leírás", descriptionField, descriptionError, true);
        addRow(form, 3, "Határidő dátuma", dateField, dateError, false);
        addRow(form, 4, "Prioritás", priorityBox, priorityError, false);
        return form;
    }

    private void addRow(JPanel form, int row, String labelText, JComponent field, JLabel error, boolean wide) {
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = row;

        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.BOLD, 15));
        label.setForeground(TEXT_DARK);
        gc.gridx = 0;
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.insets = new Insets(14, 0, 0, 20);
        form.add(label, gc);

        JPanel cell = new JPanel(new BorderLayout(0, 2));
        cell.setOpaque(false);
        if (wide) {
            cell.add(field, BorderLayout.CENTER);
        } else {
            JPanel holder = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            holder.setOpaque(false);
            holder.add(field);
            cell.add(holder, BorderLayout.CENTER);
        }
        cell.add(error, BorderLayout.SOUTH);

        gc.gridx = 1;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(8, 0, 0, 0);
        form.add(cell, gc);
    }

    private JComponent buildButtons() {
        FlatButton cancel = new FlatButton("Mégse", GRAY);
        FlatButton save = new FlatButton("Mentés", BLUE);
        expiredButton = new FlatButton("Lejárt kezelése", EXPIRED);
        expiredButton.setPreferredSize(new Dimension(140, 34));
        expiredButton.setToolTipText("Ismétlődő lejárt határidőt a következő alkalomra léptet");
        cancel.addActionListener(e -> dispose());
        save.addActionListener(e -> onSave());
        expiredButton.addActionListener(e -> onHandleExpired());
        getRootPane().setDefaultButton(save);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(expiredButton);
        buttons.add(cancel);
        buttons.add(save);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createEmptyBorder(0, 22, 16, 22));
        bottom.add(generalError, BorderLayout.WEST);
        bottom.add(buttons, BorderLayout.EAST);
        return bottom;
    }

    // ---------------------------------------------------------------- stílus

    private void styleTextField(JTextField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBackground(FIELD_BG);
        field.setForeground(Color.WHITE);
        field.setCaretColor(Color.WHITE);
        field.setBorder(FIELD_BORDER);
    }

    private void styleComboBox(JComboBox<?> box, int width) {
        box.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        box.setBackground(FIELD_BG);
        box.setForeground(Color.WHITE);
        box.setBorder(COMBO_BORDER);
        box.setPreferredSize(new Dimension(width, 34));

        box.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                BasicArrowButton btn = new BasicArrowButton(
                        BasicArrowButton.SOUTH, FIELD_BG, FIELD_BG, HINT, FIELD_BG);
                btn.setBorder(BorderFactory.createEmptyBorder());
                return btn;
            }

            @Override
            public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
                g.setColor(FIELD_BG);
                g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }
        });

        box.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBorder(BorderFactory.createEmptyBorder(4, 7, 4, 7));
                setBackground(index >= 0 && isSelected ? TITLE_BAR : FIELD_BG);
                setForeground(Color.WHITE);
                return this;
            }
        });
    }

    private static JLabel createErrorLabel() {
        JLabel label = new JLabel(" ");
        label.setFont(new Font("Segoe UI", Font.BOLD, 11));
        label.setForeground(ERROR);
        return label;
    }

    // ---------------------------------------------------------------- adatok

    private void fillFields(Deadline d) {
        titleField.setText(d.getTitle());
        descriptionField.setText(d.getDescription());
        dateField.setText(d.getDate().format(DeadlineUpdater.DATE_FORMAT));
        priorityBox.setSelectedItem(d.getPriority());
        updateExpiredState();
    }

    private void updateExpiredState() {
        boolean expired = DeadlineUpdater.deadline_is_expired(deadlineId);
        expiredInfo.setText(expired ? "⚠ Ez a határidő lejárt, a dátuma nem módosítható." : " ");
        dateField.setEditable(!expired);
        dateField.setForeground(expired ? HINT : Color.WHITE);
        dateField.setToolTipText(expired ? "Lejárt határidő dátuma nem módosítható" : null);
        expiredButton.setVisible(expired);
    }

    private void clearErrors() {
        for (JTextField f : new JTextField[]{titleField, descriptionField, dateField}) {
            f.setBorder(FIELD_BORDER);
        }
        priorityBox.setBorder(COMBO_BORDER);
        for (JLabel l : new JLabel[]{titleError, descriptionError, dateError, priorityError, generalError}) {
            l.setText(" ");
        }
        generalError.setToolTipText(null);
    }

    private void showErrors(Map<String, String> errors) {
        if (errors.containsKey(DeadlineUpdater.F_TITLE)) {
            titleError.setText(errors.get(DeadlineUpdater.F_TITLE));
            titleField.setBorder(FIELD_ERROR_BORDER);
        }
        if (errors.containsKey(DeadlineUpdater.F_DESCRIPTION)) {
            descriptionError.setText(errors.get(DeadlineUpdater.F_DESCRIPTION));
            descriptionField.setBorder(FIELD_ERROR_BORDER);
        }
        if (errors.containsKey(DeadlineUpdater.F_DATE)) {
            dateError.setText(errors.get(DeadlineUpdater.F_DATE));
            dateField.setBorder(FIELD_ERROR_BORDER);
        }
        if (errors.containsKey(DeadlineUpdater.F_PRIORITY)) {
            priorityError.setText(errors.get(DeadlineUpdater.F_PRIORITY));
            priorityBox.setBorder(COMBO_ERROR_BORDER);
        }
        if (errors.containsKey(DeadlineUpdater.F_DB)) {
            generalError.setText(errors.get(DeadlineUpdater.F_DB));
            generalError.setToolTipText(errors.get("db_detail"));
        }
        for (String key : errors.keySet()) {
            switch (key) {
                case DeadlineUpdater.F_TITLE -> titleField.requestFocusInWindow();
                case DeadlineUpdater.F_DESCRIPTION -> descriptionField.requestFocusInWindow();
                case DeadlineUpdater.F_DATE -> dateField.requestFocusInWindow();
                case DeadlineUpdater.F_PRIORITY -> priorityBox.requestFocusInWindow();
                default -> { continue; }
            }
            break;
        }
    }

    // ---------------------------------------------------------------- események

    private void onSave() {
        clearErrors();

        LocalDate done = current.getDone();

        Map<String, String> errors = DeadlineUpdater.deadline_edit(
                deadlineId, titleField.getText(), descriptionField.getText(), dateField.getText(),
                (String) priorityBox.getSelectedItem(), done);

        if (errors.isEmpty()) {
            saved = true;
            dispose();
        } else {
            showErrors(errors);
        }
    }

    private void onHandleExpired() {
        DeadlineUpdater.ExpiredResult result = DeadlineUpdater.deadline_handle_expired(deadlineId);
        switch (result) {
            case RESCHEDULED -> {
                saved = true;
                current = DeadlineUpdater.deadline_get(deadlineId);
                clearErrors();
                fillFields(current);
                JOptionPane.showMessageDialog(this,
                        "Az ismétlődő határidő a következő alkalomra lépett:\n"
                                + current.getDate().format(DeadlineUpdater.DATE_FORMAT),
                        "Lejárt határidő", JOptionPane.INFORMATION_MESSAGE);
            }
            case STILL_EXPIRED -> JOptionPane.showMessageDialog(this,
                    "Ez a határidő nem ismétlődő, ezért lejártként marad.\n",
                    "Lejárt határidő", JOptionPane.INFORMATION_MESSAGE);
            case NOT_EXPIRED -> JOptionPane.showMessageDialog(this,
                    "A határidő még nem járt le.", "Lejárt határidő", JOptionPane.INFORMATION_MESSAGE);
            case NOT_FOUND -> JOptionPane.showMessageDialog(this,
                    "A határidő nem található.", "Hiba", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ---------------------------------------------------------------- segéd osztály

    private static class FlatButton extends JButton {
        private final Color background;

        FlatButton(String text, Color background) {
            super(text);
            this.background = background;
            setFont(new Font("Segoe UI", Font.BOLD, 14));
            setPreferredSize(new Dimension(92, 34));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(background);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            g2.setColor(Color.WHITE);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(getText())) / 2;
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(getText(), x, y);
            g2.dispose();
        }
    }
}