package deadlineProjectGUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.sql.Connection;
import java.sql.PreparedStatement;


public class MainFrame extends JFrame {

    private enum View { YEAR, MONTH, DAY }

    private static final Color SIDEBAR = new Color(0x7B8D9E);
    private static final Color BACKGROUND = new Color(0xEFEFEF);
    private static final Color DARK = new Color(0x2B3A4A);
    private static final Color PLUS_BLUE = new Color(0x4A7AAB);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final Locale HU = Locale.forLanguageTag("hu");

    private View view = View.MONTH;
    private LocalDate current = LocalDate.now();

    //private int yearBlockStart = 2020;
    private static final Color SELECT_BLUE = new Color(0x89D4F5);

    private final JLabel titleLabel = new JLabel("", SwingConstants.CENTER);
    private final JPanel content = new JPanel(new BorderLayout());
    private final Map<View, JLabel> navLabels = new HashMap<>();

    public MainFrame() {
        super("Határidő alkalmazás");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 650);
        setLocationRelativeTo(null);

        add(buildSidebar(), BorderLayout.WEST);
        add(buildMainArea(), BorderLayout.CENTER);
        refresh();
    }

    // ---------------------------------------------------------------- felépítés
    private JComponent buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(SIDEBAR);
        sidebar.setPreferredSize(new Dimension(170, 0));

        JPanel nav = new JPanel();
        nav.setOpaque(false);
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.setBorder(BorderFactory.createEmptyBorder(30, 0, 0, 0));
        nav.add(createNavLabel("Year", View.YEAR));
        nav.add(createNavLabel("Month", View.MONTH));
        nav.add(createNavLabel("Day", View.DAY));

        FlatButton plus = new FlatButton("+", PLUS_BLUE, Color.WHITE, new Font("Segoe UI", Font.BOLD, 22));
        plus.setPreferredSize(new Dimension(46, 46));
        plus.addActionListener(e -> DeadlineActions.showAddDialog(this, this::refresh));
        JPanel plusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        plusPanel.setOpaque(false);
        plusPanel.setBorder(BorderFactory.createEmptyBorder(0, 25, 25, 0));
        plusPanel.add(plus);

        sidebar.add(nav, BorderLayout.NORTH);
        sidebar.add(plusPanel, BorderLayout.SOUTH);
        return sidebar;
    }

    private JLabel createNavLabel(String text, View target) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        label.setPreferredSize(new Dimension(170, 40));
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                view = target;
                refresh();
            }
        });
        navLabels.put(target, label);
        return label;
    }

    private JComponent buildMainArea() {
        FlatButton prev = new FlatButton("←", DARK, Color.WHITE, new Font("Segoe UI", Font.BOLD, 18));
        FlatButton next = new FlatButton("→", DARK, Color.WHITE, new Font("Segoe UI", Font.BOLD, 18));
        prev.setPreferredSize(new Dimension(42, 42));
        next.setPreferredSize(new Dimension(42, 42));
        prev.addActionListener(e -> shift(-1));
        next.addActionListener(e -> shift(1));

        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 36));
        titleLabel.setForeground(DARK);

        JPanel header = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 12));
        header.setOpaque(false);
        header.add(prev);
        header.add(titleLabel);
        header.add(next);

        JPanel headerWrap = new JPanel(new BorderLayout());
        headerWrap.setOpaque(false);
        headerWrap.setBorder(BorderFactory.createEmptyBorder(10, 25, 0, 25));
        headerWrap.add(header, BorderLayout.CENTER);
        JSeparator line = new JSeparator();
        line.setForeground(DARK);
        headerWrap.add(line, BorderLayout.SOUTH);

        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(10, 25, 15, 25));

        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(BACKGROUND);
        main.add(headerWrap, BorderLayout.NORTH);
        main.add(content, BorderLayout.CENTER);
        //priority codex
        main.add(buildPriorityLegend(), BorderLayout.SOUTH);

        return main;
    }

    // ---------------------------------------------------------------- frissítés
    private void refresh() {
        for (Map.Entry<View, JLabel> e : navLabels.entrySet()) {
            boolean selected = e.getKey() == view;
            e.getValue().setBorder(BorderFactory.createMatteBorder(0, 4, 0, 0,
                    selected ? Color.WHITE : SIDEBAR));
            e.getValue().setFont(new Font("Segoe UI", selected ? Font.BOLD : Font.PLAIN, 16));
            e.getValue().setText("  " + e.getValue().getText().trim());
        }

        content.removeAll();
        switch (view) {
                case YEAR -> {
                    int[] range = getYearRange();
                    titleLabel.setText(range[0] + "-" + range[1]);
                    content.add(buildYearView(), BorderLayout.CENTER);
                }
            case MONTH -> {
                titleLabel.setText(current.getYear() + ". " + capitalize(monthName(current)));
                content.add(buildMonthView(), BorderLayout.CENTER);
            }
            case DAY -> {
                titleLabel.setText(capitalize(monthName(current)) + " " + current.getDayOfMonth() + ".");
                content.add(buildDayView(), BorderLayout.CENTER);
            }
        }
        content.revalidate();
        content.repaint();
    }

    private void shift(int direction) {
        switch (view) {
            //Dinamikus banner update éves nézetben
            case YEAR -> current = current.plusYears(direction);
            case MONTH -> current = current.plusMonths(direction);
            case DAY -> current = current.plusDays(direction);
        }
        refresh();
    }
    //Dinamikus banner update logika
    private int[] getYearRange() {
        int endYear = current.getYear();
        int startYear = endYear - 5; // 6 years total (2x3 grid), ending on current.getYear()
        return new int[]{startYear, endYear};
    }
    //--------------------Éves Nézet-------------------------------------
    private JComponent buildYearView() {
        int[] range = getYearRange();
        int startYear = range[0];
        int endYear = range[1];
        titleLabel.setText(startYear + "-" + endYear);

        JPanel grid = new JPanel(new GridLayout(2, 3, 24, 24));
        grid.setOpaque(false);
        grid.setBorder(BorderFactory.createEmptyBorder(55, 45, 75, 45));

        for (int year = startYear; year <= endYear; year++) {
            final int y = year;
            boolean isCurrent = (y == endYear);

            JPanel cell = new JPanel(new GridBagLayout());
            cell.setOpaque(false);
            cell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            JLabel yearLabel = new JLabel(String.valueOf(y), SwingConstants.CENTER);
            yearLabel.setFont(new Font("Segoe UI", Font.PLAIN, 32));
            yearLabel.setForeground(DARK);
            yearLabel.setOpaque(isCurrent);
            yearLabel.setBackground(new Color(0x89D4F5));
            yearLabel.setBorder(BorderFactory.createEmptyBorder(10, 22, 10, 22));

            cell.add(yearLabel);
            cell.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    current = current.withYear(y);
                    view = View.MONTH;
                    refresh();
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    if (!isCurrent) {
                        yearLabel.setOpaque(true);
                        yearLabel.setBackground(new Color(0xDCEAF5));
                        yearLabel.repaint();
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (!isCurrent) {
                        yearLabel.setOpaque(false);
                        yearLabel.repaint();
                    }
                }
            });

            grid.add(cell);
        }

        return grid;
    }


    // ---------------------------------------------------------------- hónap nézet
    private JComponent buildMonthView() {
        YearMonth ym = YearMonth.from(current);
        LocalDate first = ym.atDay(1);
        Map<LocalDate, List<Deadline>> byDay = new HashMap<>();
        for (Deadline d : load(first.atStartOfDay(), ym.plusMonths(1).atDay(1).atStartOfDay())) {
            byDay.computeIfAbsent(d.getDate().toLocalDate(), k -> new java.util.ArrayList<>()).add(d);
        }

        JPanel grid = new JPanel(new GridLayout(0, 7, 4, 4));
        grid.setOpaque(false);
        for (String name : new String[]{"H", "K", "Sze", "Cs", "P", "Szo", "V"}) {
            JLabel l = new JLabel(name, SwingConstants.CENTER);
            l.setFont(new Font("Segoe UI", Font.BOLD, 14));
            l.setForeground(DARK);
            grid.add(l);
        }
        int offset = first.getDayOfWeek().getValue() - 1;
        for (int i = 0; i < offset; i++) {
            JPanel empty = new JPanel();
            empty.setOpaque(false);
            grid.add(empty);
        }
        for (int day = 1; day <= ym.lengthOfMonth(); day++) {
            LocalDate date = ym.atDay(day);
            grid.add(createDayCell(date, byDay.getOrDefault(date, List.of())));
        }
        return grid;
    }

    private JComponent createDayCell(LocalDate date, List<Deadline> deadlines) {
        JPanel cell = new JPanel(new BorderLayout());
        cell.setBackground(Color.WHITE);
        cell.setBorder(date.equals(LocalDate.now())
                ? BorderFactory.createLineBorder(PLUS_BLUE, 2)
                : BorderFactory.createLineBorder(new Color(0xCCCCCC)));
        cell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel number = new JLabel(" " + date.getDayOfMonth());
        number.setFont(new Font("Segoe UI", Font.BOLD, 13));
        cell.add(number, BorderLayout.NORTH);

        if (!deadlines.isEmpty()) {
            StringBuilder sb = new StringBuilder("<html>");
            for (int i = 0; i < Math.min(2, deadlines.size()); i++) {
                sb.append(escape(deadlines.get(i).getTitle())).append("<br>");
            }
            if (deadlines.size() > 2) {
                sb.append("+").append(deadlines.size() - 2).append(" további");
            }
            JLabel info = new JLabel(sb.toString());
            info.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            info.setVerticalAlignment(SwingConstants.TOP);
            info.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
            cell.add(info, BorderLayout.CENTER);
        }

        cell.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                current = date;
                view = View.DAY;
                refresh();
            }
        });
        return cell;
    }

    // ---------------------------------------------------------------- napi nézet
    /*private JComponent buildDayView() {
        List<Deadline> deadlines = load(current.atStartOfDay(), current.plusDays(1).atStartOfDay());

        JPanel list = new JPanel(new GridLayout(0, 1, 0, 0));
        list.setOpaque(false);

        list.add(createRow(new JLabel(""), headerLabel("Prioritás")));

        for (Deadline d : deadlines) {
            list.add(createDeadlineRow(d));
        }

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(list, BorderLayout.NORTH);

        if (deadlines.isEmpty()) {
            JLabel empty = new JLabel("Erre a napra nincs határidő.", SwingConstants.CENTER);
            empty.setFont(new Font("Segoe UI", Font.ITALIC, 16));
            empty.setBorder(BorderFactory.createEmptyBorder(30, 0, 0, 0));
            wrapper.add(empty, BorderLayout.CENTER);
        }

        JScrollPane scroll = new JScrollPane(wrapper);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        return scroll;
    }

    private JLabel headerLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        l.setForeground(DARK);
        return l;
    }

    private JComponent createDeadlineRow(Deadline d) {
        String desc = d.getDescription() == null ? "" : escape(d.getDescription());
        String html = "<html><b>" + d.getDate().format(TIME) + "</b> - " + escape(d.getTitle())
                + "<br><i><font size=-1 color=#444444>" + desc + "</font></i></html>";
        JLabel text = new JLabel(html);
        text.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        text.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        JLabel priority = new JLabel(d.getPriority(), SwingConstants.CENTER);
        priority.setOpaque(true);
        priority.setBackground("High".equals(d.getPriority()) ? new Color(0xF2C9C9) : new Color(0xCFE3F5));
        priority.setBorder(BorderFactory.createEmptyBorder(2, 12, 2, 12));
        priority.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        return createRow(text, priority);
    }

    private JPanel createRow(JComponent c0, JComponent c1) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xB5BEC7)));
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = 0;
        gc.fill = GridBagConstraints.BOTH;
        gc.weighty = 1;

        gc.gridx = 0;
        gc.weightx = 1;
        row.add(c0, gc);
        gc.weightx = 0;
        gc.gridx = 1;
        row.add(fixedCell(c1, 120), gc);
        return row;
    }

    private JPanel fixedCell(JComponent c, int width) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(width, 52));
        p.add(c);
        return p;
    }*/
    private JComponent buildDayView() {
        List<Deadline> deadlines = load(current.atStartOfDay(), current.plusDays(1).atStartOfDay());

        JPanel list = new JPanel(new GridLayout(0, 1, 0, 0));
        list.setOpaque(false);

        list.add(createRow(new JLabel(""), headerLabel("Prioritás"), headerLabel("Kész"), new JLabel("")));

        for (Deadline d : deadlines) {
            list.add(createDeadlineRow(d));
        }

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(list, BorderLayout.NORTH);

        if (deadlines.isEmpty()) {
            JLabel empty = new JLabel("Erre a napra nincs határidő.", SwingConstants.CENTER);
            empty.setFont(new Font("Segoe UI", Font.ITALIC, 16));
            empty.setBorder(BorderFactory.createEmptyBorder(30, 0, 0, 0));
            wrapper.add(empty, BorderLayout.CENTER);
        }

        JScrollPane scroll = new JScrollPane(wrapper);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        return scroll;
    }

    private JLabel headerLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 13));
        l.setForeground(DARK);
        return l;
    }

    private JComponent createDeadlineRow(Deadline d) {
        boolean expired = !d.isDone() && d.getDate().isBefore(LocalDateTime.now());

        String desc = d.getDescription() == null ? "" : escape(d.getDescription());
        String title = "<b>" + d.getDate().format(TIME) + "</b> - " + escape(d.getTitle());
        if (expired) title = "<s>" + title + "</s>";

        JLabel text = new JLabel("<html>" + title + "<br><i><font size=-1 color=#444444>" + desc + "</font></i></html>");
        text.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        text.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        String p = d.getPriority() == null ? "" : d.getPriority();
        JLabel priority = new JLabel(p, SwingConstants.CENTER);
        priority.setOpaque(true);
        priority.setBorder(BorderFactory.createEmptyBorder(2, 12, 2, 12));
        priority.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        priority.setBackground(switch (p) {
            case "Nagy", "High" -> new Color(0xF2C9C9);
            case "Alacsony", "Low" -> new Color(0xDBEAFE);
            default -> new Color(0xCFE3F5);
        });

        JCheckBox done = new JCheckBox();
        done.setSelected(d.isDone());
        done.setOpaque(false);
        done.addActionListener(e -> DeadlineActions.setDone(this, d, done.isSelected(), this::refresh));

        FlatButton del = new FlatButton("🗑", new Color(0xB91C1C), Color.WHITE, new Font("Segoe UI Emoji", Font.PLAIN, 13));
        FlatButton edit = new FlatButton("✎", new Color(0x6D28D9), Color.WHITE, new Font("Segoe UI Emoji", Font.BOLD, 14));
        del.setPreferredSize(new Dimension(30, 30));
        del.addActionListener(e -> DeadlineActions.deleteDeadline(this, d, this::refresh));
        edit.setPreferredSize(new Dimension(30, 30));
        edit.addActionListener(e -> DeadlineEditDialog.open(this, d.getId(), this::refresh));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        actions.setOpaque(false);
        actions.add(del);
        actions.add(edit);

        JPanel row = createRow(text, priority, done, actions);
        if (expired) {
            row.setOpaque(true);
            row.setBackground(new Color(0xD3D7DC));
        }
        return row;
    }

    private JPanel createRow(JComponent c0, JComponent c1, JComponent c2, JComponent c3) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xB5BEC7)));
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = 0;
        gc.fill = GridBagConstraints.BOTH;
        gc.weighty = 1;

        gc.gridx = 0;
        gc.weightx = 1;
        row.add(c0, gc);
        gc.weightx = 0;
        gc.gridx = 1;
        row.add(fixedCell(c1, 110), gc);
        gc.gridx = 2;
        row.add(fixedCell(c2, 65), gc);
        gc.gridx = 3;
        row.add(fixedCell(c3, 95), gc);
        return row;
    }

    private JPanel fixedCell(JComponent c, int width) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(width, 52));
        p.add(c);
        return p;
    }

    // ---------------------------------------------------------------- segédek
    private List<Deadline> load(LocalDateTime from, LocalDateTime to) {
        try {
            return DeadlineLoader.loadBetween(from, to);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Nem sikerült betölteni a határidőket!\n" + ex.getMessage(),
                    "Adatbázis hiba", JOptionPane.ERROR_MESSAGE);
            return List.of();
        }
    }

    private static String monthName(LocalDate date) {
        return date.getMonth().getDisplayName(TextStyle.FULL, HU);
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static class FlatButton extends JButton {
        private final Color background;
        private final Color foreground;

        FlatButton(String text, Color background, Color foreground, Font font) {
            super(text);
            this.background = background;
            this.foreground = foreground;
            setFont(font);
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
            g2.setColor(foreground);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(getText())) / 2;
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(getText(), x, y);
            g2.dispose();
        }
    }
    //Priority codex_2
    private JComponent buildPriorityLegend() {
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 8));
        legend.setOpaque(false);
        legend.setBorder(BorderFactory.createEmptyBorder(0, 25, 8, 25));

        legend.add(legendItem("Alacsony prioritás", new Color(0xDBEAFE)));
        legend.add(legendItem("Közepes prioritás", new Color(0xCFE3F5)));
        legend.add(legendItem("Nagy prioritás", new Color(0xF2C9C9)));
        return legend;
    }

    private JComponent legendItem(String text, Color color) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        item.setOpaque(false);

        JPanel line = new JPanel();
        line.setBackground(color);
        line.setPreferredSize(new Dimension(28, 6));
        line.setBorder(BorderFactory.createLineBorder(new Color(0xB5BEC7)));

        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setForeground(DARK);

        item.add(line);
        item.add(label);
        return item;
    }

}