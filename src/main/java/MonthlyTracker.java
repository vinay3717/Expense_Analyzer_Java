import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.sql.*;
import java.util.*;
import java.util.List;

public class MonthlyTracker extends JFrame {

    // ── DB Config (shared) ─────────────────────────────────────
    static final String DB_URL  = "jdbc:mysql://localhost:3306/expense_db";
    static final String DB_USER = "root";
    static final String DB_PASS = "Vinay#371708";

    // ── Palette (same as ExpenseGUI) ───────────────────────────
    static final Color BG_DARK    = new Color(15, 17, 26);
    static final Color BG_CARD    = new Color(24, 27, 42);
    static final Color BG_INPUT   = new Color(32, 36, 54);
    static final Color ACCENT     = new Color(99, 179, 237);
    static final Color SUCCESS    = new Color(72, 199, 142);
    static final Color DANGER     = new Color(252, 92, 101);
    static final Color WARN       = new Color(255, 204, 77);
    static final Color TEXT_MAIN  = new Color(230, 235, 255);
    static final Color TEXT_DIM   = new Color(120, 130, 165);
    static final Color BORDER_COL = new Color(45, 50, 75);

    static final Color[] CAT_COLORS = {
            new Color(99,  179, 237), new Color(154, 117, 234),
            new Color(72,  199, 142), new Color(252, 141,  98),
            new Color(252,  92, 101), new Color(255, 204,  77),
            new Color(102, 217, 232), new Color(255, 138, 128),
    };

    static final String[] MONTH_NAMES = {
            "January","February","March","April","May","June",
            "July","August","September","October","November","December"
    };

    // ── Fonts ──────────────────────────────────────────────────
    static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD,  22);
    static final Font FONT_LABEL = new Font("Segoe UI", Font.BOLD,  12);
    static final Font FONT_BODY  = new Font("Segoe UI", Font.PLAIN, 13);
    static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    static final Font FONT_MONO  = new Font("Consolas",  Font.PLAIN, 12);
    static final Font FONT_BIG   = new Font("Segoe UI", Font.BOLD,  20);

    // ── State ──────────────────────────────────────────────────
    private JComboBox<String> monthCombo;
    private JComboBox<Integer> yearCombo;
    private JLabel statusLabel;

    // Summary card labels
    private JLabel totalAmtLabel, topCatLabel, topCatAmtLabel,
            txnCountLabel, avgLabel, highestLabel;

    // Table
    private JTable table;
    private DefaultTableModel tableModel;

    // Charts
    private BarChartPanel  barChart;
    private PieChartPanel  pieChart;

    // ── Constructor ────────────────────────────────────────────
    public MonthlyTracker() {
        setTitle("Monthly Expense Tracker");
        setSize(1200, 760);
        setMinimumSize(new Dimension(1000, 640));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout(0, 0));

        add(buildHeader(),    BorderLayout.NORTH);
        add(buildContent(),   BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        // Load current month on open
        loadData();
    }

    // ── Header ─────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(20, 22, 38), getWidth(), 0, new Color(30, 24, 52));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(BORDER_COL);
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
            }
        };
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(16, 28, 14, 28));

        // Left: icon + title
        JLabel icon  = new JLabel("📅");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 26));
        JLabel title = new JLabel("  Monthly Tracker");
        title.setFont(FONT_TITLE);
        title.setForeground(TEXT_MAIN);
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);
        left.add(icon);
        left.add(title);

        // Right: month + year pickers + Load button
        Calendar cal = Calendar.getInstance();
        String[] months = MONTH_NAMES;
        monthCombo = styledCombo(months);
        monthCombo.setSelectedIndex(cal.get(Calendar.MONTH));
        monthCombo.setMaximumSize(new Dimension(130, 36));
        monthCombo.setPreferredSize(new Dimension(130, 36));

        Integer[] years = buildYearRange();
        yearCombo = styledCombo(years);
        yearCombo.setSelectedItem(cal.get(Calendar.YEAR));
        yearCombo.setMaximumSize(new Dimension(90, 36));
        yearCombo.setPreferredSize(new Dimension(90, 36));

        JButton loadBtn = buildButton("  Load  ", ACCENT);
        loadBtn.setPreferredSize(new Dimension(90, 36));
        loadBtn.addActionListener(e -> loadData());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        JLabel forLabel = new JLabel("Showing:");
        forLabel.setFont(FONT_LABEL);
        forLabel.setForeground(TEXT_DIM);
        right.add(forLabel);
        right.add(monthCombo);
        right.add(yearCombo);
        right.add(loadBtn);

        header.add(left,  BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private Integer[] buildYearRange() {
        int current = Calendar.getInstance().get(Calendar.YEAR);
        Integer[] years = new Integer[10];
        for (int i = 0; i < 10; i++) years[i] = current - 5 + i;
        return years;
    }

    // ── Main Content ───────────────────────────────────────────
    private JPanel buildContent() {
        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setBackground(BG_DARK);
        content.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        content.add(buildSummaryRow(),  BorderLayout.NORTH);
        content.add(buildMiddleRow(),   BorderLayout.CENTER);
        return content;
    }

    // ── Summary Cards Row ──────────────────────────────────────
    private JPanel buildSummaryRow() {
        JPanel row = new JPanel(new GridLayout(1, 5, 12, 0));
        row.setBackground(BG_DARK);
        row.setPreferredSize(new Dimension(0, 100));

        totalAmtLabel   = new JLabel("₹0.00");
        topCatLabel     = new JLabel("—");
        topCatAmtLabel  = new JLabel("₹0.00");
        txnCountLabel   = new JLabel("0");
        avgLabel        = new JLabel("₹0.00");

        row.add(summaryCard("💸  Total Spent",       totalAmtLabel,  ACCENT));
        row.add(summaryCard("🏆  Top Category",       topCatLabel,    new Color(154, 117, 234)));
        row.add(summaryCard("📦  Top Cat. Amount",    topCatAmtLabel, WARN));
        row.add(summaryCard("🔢  Transactions",       txnCountLabel,  SUCCESS));
        row.add(summaryCard("📊  Daily Average",      avgLabel,       new Color(252, 141, 98)));
        return row;
    }

    private JPanel summaryCard(String title, JLabel valueLabel, Color accentCol) {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // left accent bar
                g2.setColor(accentCol);
                g2.fillRoundRect(0, 0, 4, getHeight(), 4, 4);
            }
        };
        card.setBackground(BG_CARD);
        card.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COL, 1, true),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(FONT_SMALL);
        titleLbl.setForeground(TEXT_DIM);

        valueLabel.setFont(FONT_BIG);
        valueLabel.setForeground(accentCol);

        card.add(titleLbl,   BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    // ── Middle Row: Charts left, Table right ───────────────────
    private JSplitPane buildMiddleRow() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildChartsPanel(), buildTablePanel());
        split.setDividerLocation(480);
        split.setDividerSize(4);
        split.setBorder(null);
        split.setBackground(BG_DARK);
        split.setContinuousLayout(true);
        return split;
    }

    private JPanel buildChartsPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 0, 12));
        panel.setBackground(BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));

        // Bar chart card
        JPanel barCard = wrapCard(createCardInner("Monthly Spending — Bar Chart"));
        barChart = new BarChartPanel();
        barCard.add(barChart, BorderLayout.CENTER);

        // Pie chart card
        JPanel pieCard = wrapCard(createCardInner("Category Breakdown"));
        pieChart = new PieChartPanel();
        pieCard.add(pieChart, BorderLayout.CENTER);

        panel.add(barCard);
        panel.add(pieCard);
        return panel;
    }

    private JPanel buildTablePanel() {
        String[] cols = {"ID", "Date", "Category", "Amount (₹)", "Description"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(tableModel);
        styleTable();

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(BG_CARD);
        scroll.setBackground(BG_CARD);

        JPanel header = createCardInner("Transactions This Month");
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(BG_CARD);
        card.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COL, 1, true),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));
        JPanel headerWrap = new JPanel(new BorderLayout());
        headerWrap.setBackground(BG_CARD);
        headerWrap.setBorder(BorderFactory.createEmptyBorder(14, 16, 8, 16));
        headerWrap.add(header, BorderLayout.CENTER);

        card.add(headerWrap, BorderLayout.NORTH);
        card.add(scroll,     BorderLayout.CENTER);
        return card;
    }

    // ── Status Bar ─────────────────────────────────────────────
    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 6));
        bar.setBackground(new Color(12, 14, 22));
        bar.setBorder(new MatteBorder(1, 0, 0, 0, BORDER_COL));
        statusLabel = new JLabel("Ready");
        statusLabel.setFont(FONT_SMALL);
        statusLabel.setForeground(TEXT_DIM);
        bar.add(statusLabel);
        return bar;
    }

    // ── Load Data ──────────────────────────────────────────────
    private void loadData() {
        int month = monthCombo.getSelectedIndex() + 1; // 1-based
        int year  = (Integer) yearCombo.getSelectedItem();

        tableModel.setRowCount(0);
        Map<String, Double> catTotals = new LinkedHashMap<>();
        double total = 0;
        int txnCount = 0;

        String sql = "SELECT * FROM expenses " +
                "WHERE MONTH(expense_date) = ? AND YEAR(expense_date) = ? " +
                "ORDER BY expense_date ASC";

        try (Connection con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, month);
            ps.setInt(2, year);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                double amt = rs.getDouble("amount");
                String cat = rs.getString("category");
                total += amt;
                txnCount++;
                catTotals.merge(cat, amt, Double::sum);
                tableModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("expense_date"),
                        cat,
                        String.format("%.2f", amt),
                        rs.getString("description")
                });
            }

            // Days in selected month for avg
            Calendar cal = Calendar.getInstance();
            cal.set(year, month - 1, 1);
            int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

            // Top category
            String topCat    = "—";
            double topAmt    = 0;
            for (Map.Entry<String, Double> e : catTotals.entrySet()) {
                if (e.getValue() > topAmt) { topAmt = e.getValue(); topCat = e.getKey(); }
            }

            // Update summary cards
            totalAmtLabel .setText(String.format("₹%.2f", total));
            topCatLabel   .setText(topCat);
            topCatAmtLabel.setText(String.format("₹%.2f", topAmt));
            txnCountLabel .setText(String.valueOf(txnCount));
            avgLabel      .setText(String.format("₹%.2f", total / daysInMonth));

            // Update charts
            barChart.setData(catTotals, MONTH_NAMES[month - 1] + " " + year);
            pieChart.setData(catTotals);

            statusLabel.setText("📊  " + txnCount + " transaction(s) | " +
                    MONTH_NAMES[month - 1] + " " + year + " | Total: ₹" + String.format("%.2f", total));

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "DB error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Table Styling ──────────────────────────────────────────
    private void styleTable() {
        table.setBackground(BG_CARD);
        table.setForeground(TEXT_MAIN);
        table.setFont(FONT_MONO);
        table.setRowHeight(34);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setShowGrid(false);
        table.setSelectionBackground(new Color(99, 179, 237, 50));
        table.setSelectionForeground(TEXT_MAIN);
        table.setFocusable(false);

        JTableHeader header = table.getTableHeader();
        header.setBackground(new Color(30, 34, 55));
        header.setForeground(TEXT_DIM);
        header.setFont(new Font("Segoe UI", Font.BOLD, 11));
        header.setBorder(new MatteBorder(0, 0, 1, 0, BORDER_COL));
        header.setReorderingAllowed(false);

        int[] widths = {50, 110, 110, 100, 0};
        for (int i = 0; i < widths.length; i++)
            if (widths[i] > 0) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                setBackground(sel ? new Color(99, 179, 237, 60) : (row % 2 == 0 ? BG_CARD : new Color(28, 32, 50)));
                setForeground(col == 3 ? ACCENT : TEXT_MAIN);
                setFont(col == 3 ? new Font("Segoe UI", Font.BOLD, 12) : FONT_MONO);
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                setOpaque(true);
                return this;
            }
        });
    }

    // ── Helpers ────────────────────────────────────────────────
    private JPanel createCardInner(String title) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(BG_CARD);
        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbl.setForeground(TEXT_DIM);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(lbl);
        return card;
    }

    private JPanel wrapCard(JPanel inner) {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(BG_CARD);
        wrap.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COL, 1, true),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)
        ));
        wrap.add(inner, BorderLayout.NORTH);
        return wrap;
    }

    private <T> JComboBox<T> styledCombo(T[] items) {
        JComboBox<T> combo = new JComboBox<>(items);
        combo.setBackground(BG_INPUT);
        combo.setForeground(new Color(24, 8, 128));
        combo.setFont(FONT_BODY);
        combo.setBorder(new LineBorder(BORDER_COL, 1, true));
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object val, int idx, boolean sel, boolean focus) {
                super.getListCellRendererComponent(list, val, idx, sel, focus);
                list.setBackground(BG_INPUT);
                list.setForeground(TEXT_MAIN);
                setBackground(sel ? ACCENT : BG_INPUT);
                setForeground(sel ? Color.WHITE : TEXT_MAIN);
                setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
                setOpaque(true);
                return this;
            }
        });
        combo.setForeground(new Color(24, 8, 128));
        return combo;
    }

    private JButton buildButton(String text, Color bg) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed() ? bg.darker() : getModel().isRollover() ? bg.brighter() : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ── Bar Chart ──────────────────────────────────────────────
    static class BarChartPanel extends JPanel {
        private Map<String, Double> data = new LinkedHashMap<>();
        private String subtitle = "";

        BarChartPanel() { setBackground(BG_CARD); setOpaque(true); }

        void setData(Map<String, Double> d, String sub) {
            this.data = d; this.subtitle = sub; repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (data.isEmpty()) {
                g2.setColor(TEXT_DIM); g2.setFont(FONT_SMALL);
                String msg = "No data for " + subtitle;
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, getHeight() / 2);
                return;
            }

            int pad = 40, bottom = 30, top = 10;
            int chartW = getWidth()  - pad * 2;
            int chartH = getHeight() - bottom - top;

            double maxVal = data.values().stream().mapToDouble(Double::doubleValue).max().orElse(1);
            List<String> keys = new ArrayList<>(data.keySet());
            int n = keys.size();
            int barW = Math.max(20, (chartW - (n + 1) * 8) / n);
            int gap  = (chartW - n * barW) / (n + 1);

            // Y-axis gridlines
            g2.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10, new float[]{4}, 0));
            g2.setColor(BORDER_COL);
            for (int i = 1; i <= 4; i++) {
                int y = top + (int)(chartH * (1 - i / 4.0));
                g2.drawLine(pad, y, pad + chartW, y);
                g2.setColor(TEXT_DIM);
                g2.setFont(FONT_SMALL);
                g2.drawString(String.format("₹%.0f", maxVal * i / 4), 2, y + 4);
                g2.setColor(BORDER_COL);
            }
            g2.setStroke(new BasicStroke(1));

            // Bars
            for (int i = 0; i < n; i++) {
                String key = keys.get(i);
                double val = data.get(key);
                int barH = (int)(chartH * val / maxVal);
                int x    = pad + gap + i * (barW + gap);
                int y    = top + chartH - barH;

                Color col = CAT_COLORS[i % CAT_COLORS.length];
                // Gradient bar
                GradientPaint gp = new GradientPaint(x, y, col.brighter(), x, y + barH, col.darker());
                g2.setPaint(gp);
                g2.fillRoundRect(x, y, barW, barH, 6, 6);

                // Amount on top
                g2.setColor(TEXT_MAIN);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                String amt = String.format("₹%.0f", val);
                FontMetrics fm = g2.getFontMetrics();
                int tx = x + (barW - fm.stringWidth(amt)) / 2;
                if (y - 4 > top) g2.drawString(amt, tx, y - 4);

                // Category label below
                g2.setColor(TEXT_DIM);
                g2.setFont(FONT_SMALL);
                FontMetrics fm2 = g2.getFontMetrics();
                String shortKey = key.length() > 6 ? key.substring(0, 6) + "." : key;
                int lx = x + (barW - fm2.stringWidth(shortKey)) / 2;
                g2.drawString(shortKey, lx, top + chartH + 18);
            }

            // Baseline
            g2.setColor(BORDER_COL);
            g2.setStroke(new BasicStroke(1));
            g2.drawLine(pad, top + chartH, pad + chartW, top + chartH);
        }
    }

    // ── Pie Chart (same as ExpenseGUI) ─────────────────────────
    static class PieChartPanel extends JPanel {
        private Map<String, Double> data = new LinkedHashMap<>();

        PieChartPanel() { setBackground(BG_CARD); setOpaque(true); }

        void setData(Map<String, Double> d) { this.data = d; repaint(); }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (data.isEmpty()) {
                g2.setColor(TEXT_DIM); g2.setFont(FONT_SMALL);
                String msg = "No data yet";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, getHeight() / 2);
                return;
            }

            double total = data.values().stream().mapToDouble(Double::doubleValue).sum();
            int size  = Math.min(getWidth() - 20, getHeight() - 70);
            int x     = (getWidth() - size) / 2;
            int y     = 8;
            double start = -90;
            int idx = 0;

            List<String> keys = new ArrayList<>(data.keySet());
            for (String key : keys) {
                double arc = data.get(key) / total * 360;
                Color col  = CAT_COLORS[idx % CAT_COLORS.length];
                g2.setColor(col);
                g2.fill(new Arc2D.Double(x, y, size, size, start, arc, Arc2D.PIE));
                g2.setColor(BG_CARD);
                g2.setStroke(new BasicStroke(2));
                g2.draw(new Arc2D.Double(x, y, size, size, start, arc, Arc2D.PIE));
                start += arc;
                idx++;
            }

            int legendY = y + size + 12;
            int lx = 6; idx = 0;
            for (String key : keys) {
                if (lx + 140 > getWidth()) { lx = 6; legendY += 18; }
                g2.setColor(CAT_COLORS[idx % CAT_COLORS.length]);
                g2.fillRoundRect(lx, legendY, 10, 10, 3, 3);
                g2.setColor(TEXT_DIM);
                g2.setFont(FONT_SMALL);
                g2.drawString(String.format("%s %.0f%%", key, data.get(key) / total * 100), lx + 14, legendY + 9);
                lx += 130;
                idx++;
            }
        }
    }
}