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


/**
 * Felugró ablak (modális dialógus) új határidő felvételéhez.
 * Egyedi, keret nélküli megjelenésű ablak: saját címsávval, űrlappal és gombokkal.
 * Mentéskor ellenőrzi a beírt adatokat, majd az adatbázisba menti a határidőt.
 */
public class AddDeadlineDialog extends JDialog {

    // A dátum mező placeholder szövege (a felhasználónak mutatott minta)
    public static final String DATE_PATTERN_TEXT = "ÉÉÉÉ/HH/NN - ÓÓ:PP";
    // A dátum beolvasásához használt formátum; a STRICT miatt pl. a február 30. hibás lesz
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("uuuu/MM/dd - HH:mm").withResolverStyle(ResolverStyle.STRICT);

    // Üzenetek
    private static final String REQUIRED_MESSAGE = "Kérjük, töltse ki a kötelező mezőt.";
    private static final String PRIORITY_HINT = "Válasszon egyet";

    // Színek
    private static final Color WINDOW_BG = new Color(0xB0B0B0);
    private static final Color TITLE_BAR = new Color(0x3E5063);
    private static final Color FIELD_BG = new Color(0x2B3A4A);
    private static final Color TEXT_DARK = new Color(0x2B3A4A);
    private static final Color HINT = new Color(0x8FA0B0);
    private static final Color ERROR = new Color(0xC8102E);
    private static final Color BLUE = new Color(0x4A7AAB);
    private static final Color GRAY = new Color(0x7B8D9E);

    // Keretek: normál és hibás állapot (a hibás piros keretet kap)
    private static final Border FIELD_BORDER = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(FIELD_BG, 2), BorderFactory.createEmptyBorder(5, 7, 5, 7));
    private static final Border FIELD_ERROR_BORDER = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ERROR, 2), BorderFactory.createEmptyBorder(5, 7, 5, 7));
    private static final Border COMBO_BORDER = BorderFactory.createLineBorder(FIELD_BG, 2);
    private static final Border COMBO_ERROR_BORDER = BorderFactory.createLineBorder(ERROR, 2);

    // Beviteli mezők
    private final JTextField titleField = new HintTextField("Adja meg a feladat nevét", 28);
    private final JTextField descriptionField = new HintTextField("Adjon meg egy rövid leírást", 28);
    private final JTextField dateField = new HintTextField(DATE_PATTERN_TEXT, 18);
    private final JComboBox<String> priorityBox = new JComboBox<>(new String[]{"Alacsony", "Közepes", "Nagy"});
    private final JComboBox<DeadlineUpdater.Recurrence> recurrenceBox =
            new JComboBox<>(DeadlineUpdater.Recurrence.values());

    // Hibaüzenet-címkék a mezők alatt (üres állapotban egy szóköz, hogy ne ugráljon az elrendezés)
    private final JLabel titleError = createErrorLabel();
    private final JLabel descriptionError = createErrorLabel();
    private final JLabel dateError = createErrorLabel();
    private final JLabel priorityError = createErrorLabel();
    private final JLabel generalError = createErrorLabel();

    // Sikerült-e a mentés (a hívó ebből tudja, kell-e frissítenie a listát)
    private boolean saved = false;
    // Választott-e már prioritást a felhasználó (addig a "Válasszon egyet" hint látszik)
    private boolean priorityChosen = false;

    /**
     * Konstruktor: létrehozza a modális dialógust, felépíti a felületet,
     * majd a szülőablak közepére helyezi.
     */
    public AddDeadlineDialog(Frame owner) {
        super(owner, "Új határidő felvétele", true);
        buildUi();
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    /** Visszaadja, hogy a határidő sikeresen el lett-e mentve. */
    public boolean isSaved() {
        return saved;
    }

    // ---------------------------------------------------------------- felépítés

    /**
     * Összerakja az ablak teljes felületét: címsáv (fent), űrlap (középen), gombok (lent).
     * Beállítja az ESC billentyűt is, ami bezárja az ablakot.
     */
    private void buildUi() {
        setUndecorated(true); // saját címsávot használunk, ezért nincs rendszerkeret

        styleTextField(titleField);
        styleTextField(descriptionField);
        styleTextField(dateField);
        stylePriorityBox();
        styleRecurrenceBox();

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(WINDOW_BG);
        root.setBorder(BorderFactory.createLineBorder(TITLE_BAR, 1));
        root.add(buildTitleBar(), BorderLayout.NORTH);
        root.add(buildForm(), BorderLayout.CENTER);
        root.add(buildButtons(), BorderLayout.SOUTH);
        setContentPane(root);

        // ESC lenyomására az ablak bezárul
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    /**
     * Létrehozza az ablak felső, sötétkék címsávját.
     * Mivel az ablak keret nélküli, a címsáv húzásával lehet mozgatni az ablakot.
     */
    private JComponent buildTitleBar() {
        // A sáv hátterét saját rajzolással festjük ki
        JPanel bar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(TITLE_BAR);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        bar.setPreferredSize(new Dimension(0, 28));

        // Ablakmozgatás egérrel: nyomáskor eltároljuk a kattintás helyét,
        // húzáskor az ablakot ennek megfelelően odébb tesszük
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

    /** Összeállítja az űrlapot: négy sor (név, leírás, dátum, prioritás) GridBagLayout-tal. */
    private JComponent buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(BorderFactory.createEmptyBorder(16, 22, 4, 22));

        // Az utolsó paraméter: true = a mező kitölti a teljes szélességet, false = fix méretű marad
        addRow(form, 0, "Feladat neve", titleField, titleError, true);
        addRow(form, 1, "Leírás", descriptionField, descriptionError, true);
        addRow(form, 2, "Határidő dátuma", dateField, dateError, false);
        addRow(form, 3, "Prioritás", priorityBox, priorityError, false);
        addRow(form, 4, "Ismétlődés", recurrenceBox, new JLabel(" "), false);
        return form;
    }

    /**
     * Egy űrlapsort ad hozzá: bal oldalon a felirat, jobb oldalon a beviteli mező,
     * alatta a hozzá tartozó hibaüzenet-címke.
     *
     * @param row   a sor sorszáma (0-tól)
     * @param wide  true esetén a mező kitölti a rendelkezésre álló szélességet
     */
    private void addRow(JPanel form, int row, String labelText, JComponent field, JLabel error, boolean wide) {
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = row;

        // Bal oszlop: felirat
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.BOLD, 15));
        label.setForeground(TEXT_DARK);
        gc.gridx = 0;
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.insets = new Insets(14, 0, 0, 20);
        form.add(label, gc);

        // Jobb oszlop: mező + alatta a hibaüzenet
        JPanel cell = new JPanel(new BorderLayout(0, 2));
        cell.setOpaque(false);
        if (wide) {
            cell.add(field, BorderLayout.CENTER);
        } else {
            // Keskeny mezőnél FlowLayout-ba tesszük, hogy megtartsa a saját méretét
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

    /**
     * Létrehozza az alsó sávot: balra az általános hibaüzenet (pl. adatbázis hiba),
     * jobbra a "Mégse" és "Mentés" gomb. Az Enter a Mentés gombot aktiválja.
     */
    private JComponent buildButtons() {
        FlatButton cancel = new FlatButton("Mégse", GRAY);
        FlatButton save = new FlatButton("Mentés", BLUE);
        cancel.addActionListener(e -> dispose());
        save.addActionListener(e -> onSave());
        getRootPane().setDefaultButton(save); // Enter = Mentés

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

    /** Egy szövegmező egységes kinézetre állítása: betűtípus, színek, kurzor, keret. */
    private void styleTextField(JTextField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBackground(FIELD_BG);
        field.setForeground(Color.WHITE);
        field.setCaretColor(Color.WHITE);
        field.setBorder(FIELD_BORDER);
    }

    /**
     * A prioritás legördülő lista testreszabása:
     *  - kezdetben nincs kiválasztott elem, helyette a "Válasszon egyet" hint látszik
     *  - saját nyíl gomb és saját háttér/érték festés (BasicComboBoxUI)
     *  - saját lista-renderelő a színekhez
     *  - választáskor eltűnik a hibajelzés
     */
    private void stylePriorityBox() {
        priorityBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        priorityBox.setBackground(FIELD_BG);
        priorityBox.setForeground(HINT);
        priorityBox.setBorder(COMBO_BORDER);
        priorityBox.setPreferredSize(new Dimension(185, 34));
        priorityBox.setSelectedIndex(-1); // nincs alapértelmezett kiválasztás

        priorityBox.setUI(new BasicComboBoxUI() {
            // A lenyíló nyíl gomb: lapos, a mező színével megegyező hátterű
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

            // A kiválasztott érték mögötti háttér kifestése (fókusztól függetlenül mindig ugyanaz)
            @Override
            public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
                g.setColor(FIELD_BG);
                g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }

            // A mezőben látható aktuális érték kirajzolása:
            // amíg nincs választás, halvány hint szín, utána fehér
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


        // Lista-renderelő: az index == -1 a mezőben látható érték, az index >= 0 a lenyíló lista elemei
        priorityBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBorder(BorderFactory.createEmptyBorder(4, 7, 4, 7));

                if (index == -1 && (!priorityChosen || value == null)) {
                    // Még nincs választás: hint szöveg
                    setText(PRIORITY_HINT);
                    setBackground(FIELD_BG);
                    setForeground(HINT);
                } else {
                    // Lenyíló lista elemei: a kijelölt elem világosabb hátteret kap
                    setBackground(isSelected ? TITLE_BAR : FIELD_BG);
                    setForeground(Color.WHITE);
                }
                return this;
            }
        });

        // Fókuszváltáskor is ugyanaz a háttér maradjon (ne villanjon át a rendszer színére)
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


        // Ha a felhasználó választott: jelöljük, hogy van választás, és töröljük a hibajelzést
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


    /** Létrehoz egy üres, piros, félkövér hibaüzenet-címkét (egy szóközzel, hogy foglalja a helyet). */
    private static JLabel createErrorLabel() {
        JLabel label = new JLabel(" ");
        label.setFont(new Font("Segoe UI", Font.BOLD, 11));
        label.setForeground(ERROR);
        return label;
    }

    // ---------------------------------------------------------------- hibák

    /** Egy szövegmezőhöz hibát jelenít meg: kiírja az üzenetet és pirosra színezi a keretet. */
    private void setFieldError(JTextField field, JLabel label, String message) {
        label.setText(message);
        field.setBorder(FIELD_ERROR_BORDER);
    }

    /** Minden hibajelzést töröl (keretek visszaállítása, üzenetek ürítése). Mentés előtt hívódik. */
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

    /**
     * A "Mentés" gomb logikája:
     *  1. törli a korábbi hibákat
     *  2. beolvassa és ellenőrzi a mezőket (kötelező, hossz, dátumformátum, múltbeli dátum, prioritás)
     *  3. hiba esetén az első hibás mezőre teszi a fókuszt és megáll
     *  4. hibátlan adatnál az adatbázisba menti a határidőt, majd bezárja az ablakot
     */
    private void onSave() {
        clearErrors();

        // Mezők értékének beolvasása (a szélső szóközök levágásával)
        String title = titleField.getText().trim();
        String description = descriptionField.getText().trim();
        String dateText = dateField.getText().trim();
        String priority = priorityChosen ? (String) priorityBox.getSelectedItem() : null;

        // Az első hibás mező, ide kerül majd a fókusz
        JComponent firstInvalid = null;

        // Feladat neve: kötelező, legfeljebb 100 karakter
        if (title.isEmpty()) {
            setFieldError(titleField, titleError, REQUIRED_MESSAGE);
            firstInvalid = titleField;
        } else if (title.length() > 100) {
            setFieldError(titleField, titleError, "Legfeljebb 100 karakter lehet.");
            firstInvalid = titleField;
        }

        // Leírás: kötelező, legfeljebb 255 karakter
        if (description.isEmpty()) {
            setFieldError(descriptionField, descriptionError, REQUIRED_MESSAGE);
            if (firstInvalid == null) firstInvalid = descriptionField;
        } else if (description.length() > 255) {
            setFieldError(descriptionField, descriptionError, "Legfeljebb 255 karakter lehet.");
            if (firstInvalid == null) firstInvalid = descriptionField;
        }

        // Dátum: kötelező, megfelelő formátumú, és nem lehet múltbeli
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
                // Rossz formátum vagy nem létező dátum
                setFieldError(dateField, dateError, "Hibás formátum vagy dátum. Példa: 2026/11/20 - 14:30");
                if (firstInvalid == null) firstInvalid = dateField;
            }
        }

        // Prioritás: kötelező választani
        if (priority == null) {
            priorityBox.setBorder(COMBO_ERROR_BORDER);
            priorityError.setText("Kérjük, válasszon egyet.");
            if (firstInvalid == null) firstInvalid = priorityBox;
        }

        // Ha volt hiba, az első hibás mezőre ugrunk, és nem mentünk
        if (firstInvalid != null) {
            firstInvalid.requestFocusInWindow();
            return;
        }

        // Mentés az adatbázisba
        try {
            int newId = Deadline.deadline_add(0, title, description, date, priority, null);
            DeadlineUpdater.deadline_set_recurrence(newId,
                    (DeadlineUpdater.Recurrence) recurrenceBox.getSelectedItem());
            saved = true;
            dispose();
        } catch (SQLException ex) {
            // Adatbázis hiba: rövid üzenet, a részletek az egérrel fölé állva (tooltip) láthatók
            generalError.setText("Nem sikerült menteni az adatbázisba.");
            generalError.setToolTipText(ex.getMessage());
        }
    }

    // ---------------------------------------------------------------- segéd osztályok

    private void styleRecurrenceBox() {
        recurrenceBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        recurrenceBox.setBackground(FIELD_BG);
        recurrenceBox.setForeground(Color.WHITE);
        recurrenceBox.setBorder(COMBO_BORDER);
        recurrenceBox.setPreferredSize(new Dimension(185, 34));

        recurrenceBox.setUI(new BasicComboBoxUI() {
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

        recurrenceBox.setRenderer(new DefaultListCellRenderer() {
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

    /**
     * Szövegmező, ami üres állapotban halvány "hint" szöveget (placeholder) mutat.
     * A hint csak megjelenik a mezőben, a mező tényleges tartalma (getText) üres marad.
     */
    private static class HintTextField extends JTextField {
        private final String hint;

        HintTextField(String hint, int columns) {
            super(columns);
            this.hint = hint;
        }

        // Normál kirajzolás után, ha a mező üres, ráfestjük a hint szöveget
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
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent(); // függőleges középre igazítás
                g2.drawString(hint, in.left, y);
                g2.dispose();
            }
        }
    }

    /**
     * Lapos, lekerekített sarkú gomb egyszínű háttérrel és fehér felirattal.
     * A gomb kirajzolását teljesen mi végezzük (paintComponent).
     */
    private static class FlatButton extends JButton {
        private final Color background;

        FlatButton(String text, Color background) {
            super(text);
            this.background = background;
            setFont(new Font("Segoe UI", Font.BOLD, 14));
            setPreferredSize(new Dimension(92, 34));
            // A Swing alapértelmezett gombkinézetének kikapcsolása
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); // kéz kurzor fölé érve
        }

        // Háttér (lekerekített téglalap) és középre igazított felirat kirajzolása
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