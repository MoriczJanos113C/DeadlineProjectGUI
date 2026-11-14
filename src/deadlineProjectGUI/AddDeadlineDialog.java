package deadlineProjectGUI;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;


public class AddDeadlineDialog extends JDialog {

    public static final String DATE_PATTERN_TEXT = "ÉÉÉÉ/HH/NN - ÓÓ:PP";
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("uuuu/MM/dd - HH:mm").withResolverStyle(ResolverStyle.STRICT);

    private static final String REQUIRED_MESSAGE = "Kérjük, töltse ki a kötelező mezőt.";
    private static final String PRIORITY_HINT = "Válasszon egyet";

    private static final Color WINDOW_BG = new Color(0xB0B0B0);
    private static final Color TITLE_BAR = new Color(0x3E5063);
    private static final Color DOT = new Color(0x7B8D9E);
    private static final Color FIELD_BG = new Color(0x2B3A4A);
    private static final Color TEXT_DARK = new Color(0x2B3A4A);
    private static final Color HINT = new Color(0x8FA0B0);
    private static final Color ERROR = new Color(0xC8102E);
    private static final Color BLUE = new Color(0x4A7AAB);
    private static final Color GRAY = new Color(0x7B8D9E);

    private static final Border FIELD_BORDER = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(FIELD_BG, 2), BorderFactory.createEmptyBorder(5, 7, 5, 7));
    private static final Border FIELD_ERROR_BORDER = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ERROR, 2), BorderFactory.createEmptyBorder(5, 7, 5, 7));
    private static final Border COMBO_BORDER = BorderFactory.createLineBorder(FIELD_BG, 2);
    private static final Border COMBO_ERROR_BORDER = BorderFactory.createLineBorder(ERROR, 2);

    private final JTextField titleField = new HintTextField("Adja meg a feladat nevét", 28);
    private final JTextField descriptionField = new HintTextField("Adjon meg egy rövid leírást", 28);
    private final JTextField dateField = new HintTextField(DATE_PATTERN_TEXT, 18);
    private final JComboBox<String> priorityBox = new JComboBox<>(new String[]{"Low", "Normal", "High"});

    private final JLabel titleError = createErrorLabel();
    private final JLabel descriptionError = createErrorLabel();
    private final JLabel dateError = createErrorLabel();
    private final JLabel priorityError = createErrorLabel();
    private final JLabel generalError = createErrorLabel();

    private boolean saved = false;
    private boolean priorityChosen = false;

    public AddDeadlineDialog(Frame owner) {
        super(owner, "Új határidő felvétele", true);
        buildUi();
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
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
        stylePriorityBox();

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
        JPanel bar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(TITLE_BAR);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(DOT);
                for (int i = 0; i < 3; i++) {
                    g2.fillOval(12 + i * 16, (getHeight() - 9) / 2, 9, 9);
                }
                g2.dispose();
            }
        };
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
        form.setBorder(BorderFactory.createEmptyBorder(16, 22, 4, 22));

        addRow(form, 0, "Feladat neve", titleField, titleError, true);
        addRow(form, 1, "Leírás", descriptionField, descriptionError, true);
        addRow(form, 2, "Határidő dátuma", dateField, dateError, false);
        addRow(form, 3, "Prioritás", priorityBox, priorityError, false);
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
        cancel.addActionListener(e -> dispose());
        save.addActionListener(e -> onSave());
        getRootPane().setDefaultButton(save);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
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

    private void stylePriorityBox() {
        priorityBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        priorityBox.setBackground(FIELD_BG);
        priorityBox.setForeground(HINT);
        priorityBox.setBorder(COMBO_BORDER);
        priorityBox.setPreferredSize(new Dimension(185, 34));
        priorityBox.setSelectedIndex(-1);

        priorityBox.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                BasicArrowButton btn = new BasicArrowButton(
                        BasicArrowButton.SOUTH,
                        FIELD_BG,
                        FIELD_BG,
                        HINT,
                        FIELD_BG
                );
                btn.setBorder(BorderFactory.createEmptyBorder());
                return btn;
            }

            @Override
            public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
                g.setColor(FIELD_BG);
                g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }

            @Override
            @SuppressWarnings("unchecked")
            public void paintCurrentValue(Graphics g, Rectangle bounds, boolean hasFocus) {
                ListCellRenderer<Object> renderer = comboBox.getRenderer();
                Component c = renderer.getListCellRendererComponent(
                        listBox, comboBox.getSelectedItem(), -1, false, false);
                c.setFont(comboBox.getFont());
                c.setBackground(FIELD_BG);
                c.setForeground(priorityChosen ? Color.WHITE : HINT);
                currentValuePane.paintComponent(g, c, comboBox, bounds.x, bounds.y, bounds.width, bounds.height, c instanceof JPanel);
            }
        });


        priorityBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBorder(BorderFactory.createEmptyBorder(4, 7, 4, 7));

                if (index == -1 && (!priorityChosen || value == null)) {
                    setText(PRIORITY_HINT);
                    setBackground(FIELD_BG);
                    setForeground(HINT);
                } else {
                    setBackground(isSelected ? TITLE_BAR : FIELD_BG);
                    setForeground(Color.WHITE);
                }
                return this;
            }
        });

        priorityBox.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                priorityBox.setBackground(FIELD_BG);
                priorityBox.repaint();
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                priorityBox.setBackground(FIELD_BG);
                priorityBox.repaint();
            }
        });


        priorityBox.addActionListener(e -> {
            if (priorityBox.getSelectedIndex() != -1) {
                priorityChosen = true;
                priorityBox.setForeground(Color.WHITE);
                priorityBox.setBorder(COMBO_BORDER);
                priorityError.setText(" ");
                priorityBox.repaint();
            }
        });
    }


    private static JLabel createErrorLabel() {
        JLabel label = new JLabel(" ");
        label.setFont(new Font("Segoe UI", Font.BOLD, 11));
        label.setForeground(ERROR);
        return label;
    }

    // ---------------------------------------------------------------- hibák

    private void setFieldError(JTextField field, JLabel label, String message) {
        label.setText(message);
        field.setBorder(FIELD_ERROR_BORDER);
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

    // ---------------------------------------------------------------- mentés

    private void onSave() {
        clearErrors();

        String title = titleField.getText().trim();
        String description = descriptionField.getText().trim();
        String dateText = dateField.getText().trim();
        String priority = priorityChosen ? (String) priorityBox.getSelectedItem() : null;

        JComponent firstInvalid = null;

        if (title.isEmpty()) {
            setFieldError(titleField, titleError, REQUIRED_MESSAGE);
            firstInvalid = titleField;
        } else if (title.length() > 100) {
            setFieldError(titleField, titleError, "Legfeljebb 100 karakter lehet.");
            firstInvalid = titleField;
        }

        if (description.isEmpty()) {
            setFieldError(descriptionField, descriptionError, REQUIRED_MESSAGE);
            if (firstInvalid == null) firstInvalid = descriptionField;
        } else if (description.length() > 255) {
            setFieldError(descriptionField, descriptionError, "Legfeljebb 255 karakter lehet.");
            if (firstInvalid == null) firstInvalid = descriptionField;
        }

        LocalDateTime date = null;
        if (dateText.isEmpty()) {
            setFieldError(dateField, dateError, REQUIRED_MESSAGE);
            if (firstInvalid == null) firstInvalid = dateField;
        } else {
            try {
                date = LocalDateTime.parse(dateText, DATE_FORMAT);
                if (date.isBefore(LocalDateTime.now())) {
                    setFieldError(dateField, dateError, "Múltbeli időpont nem adható meg.");
                    if (firstInvalid == null) firstInvalid = dateField;
                }
            } catch (DateTimeParseException ex) {
                setFieldError(dateField, dateError, "Hibás formátum vagy dátum. Példa: 2026/11/20 - 14:30");
                if (firstInvalid == null) firstInvalid = dateField;
            }
        }

        if (priority == null) {
            priorityBox.setBorder(COMBO_ERROR_BORDER);
            priorityError.setText("Kérjük, válasszon egyet.");
            if (firstInvalid == null) firstInvalid = priorityBox;
        }

        if (firstInvalid != null) {
            firstInvalid.requestFocusInWindow();
            return;
        }

        try {
            Deadline.deadline_add(0, title, description, date, priority, null);
            saved = true;
            dispose();
        } catch (SQLException ex) {
            generalError.setText("Nem sikerült menteni az adatbázisba.");
            generalError.setToolTipText(ex.getMessage());
        }
    }

    // ---------------------------------------------------------------- segéd osztályok
    private static class HintTextField extends JTextField {
        private final String hint;

        HintTextField(String hint, int columns) {
            super(columns);
            this.hint = hint;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(HINT);
                g2.setFont(getFont());
                Insets in = getInsets();
                FontMetrics fm = g2.getFontMetrics();
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(hint, in.left, y);
                g2.dispose();
            }
        }
    }

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