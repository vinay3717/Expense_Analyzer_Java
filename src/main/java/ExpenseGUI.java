import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExpenseGUI extends JFrame {

    // ── DB Config ──────────────────────────────────────────────
    static final String DB_URL  = "jdbc:mysql://localhost:3306/expense_db";
    static final String DB_USER = "root";
    static final String DB_PASS = "Vinay#371708";

    // ── Palette ────────────────────────────────────────────────
    static final Color BG_DARK    = new Color(15, 17, 26);
    static final Color BG_CARD    = new Color(24, 27, 42);
    static final Color BG_INPUT   = new Color(32, 36, 54);
    static final Color ACCENT     = new Color(99, 179, 237);
    static final Color SUCCESS    = new Color(72, 199, 142);
    static final Color DANGER     = new Color(252, 92, 101);
    static final Color TEXT_MAIN  = new Color(230, 235, 255);
    static final Color TEXT_DIM   = new Color(120, 130, 165);
    static final Color BORDER_COL = new Color(45, 50, 75);

    // ── Category colours for pie chart ────────────────────────
    static final Color[] CAT_COLORS = {
            new Color(99,  179, 237),
            new Color(154, 117, 234),
            new Color(72,  199, 142),
            new Color(252, 141,  98),
            new Color(252,  92, 101),
            new Color(255, 204,  77),
            new Color(102, 217, 232),
            new Color(255, 138, 128),
    };

    // ── Fonts ──────────────────────────────────────────────────
    static final Font FONT_TITLE  = new Font("Segoe UI", Font.BOLD,  22);
    static final Font FONT_LABEL  = new Font("Segoe UI", Font.BOLD,  12);
    static final Font FONT_BODY   = new Font("Segoe UI", Font.PLAIN, 13);
    static final Font FONT_SMALL  = new Font("Segoe UI", Font.PLAIN, 11);
    static final Font FONT_MONO   = new Font("Consolas",  Font.PLAIN, 12);

    // ── Components ─────────────────────────────────────────────
    private JSpinner dateSpinner;
    private JTextField amountField, descField;
    private JComboBox<String> categoryCombo, filterCombo;
    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel totalLabel;
    private PieChartPanel pieChart;
    private JLabel statusLabel;

    // ── Entry Point ────────────────────────────────────────────
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) {}
            new ExpenseGUI().setVisible(true);
        });
    }

    public ExpenseGUI() {
        setTitle("Expense Analyzer");
        setSize(1100, 720);
        setMinimumSize(new Dimension(900, 600));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout(0, 0));

        add(buildHeader(),      BorderLayout.NORTH);
        add(buildCenter(),      BorderLayout.CENTER);
        add(buildStatusBar(),   BorderLayout.SOUTH);

        refreshTable("All");
        setVisible(true);
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
                g2.drawLine(0, getHeight()-1, getWidth(), getHeight()-1);
            }
        };
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(18, 28, 16, 28));

        JLabel icon  = new JLabel("💰");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));

        JLabel title = new JLabel("  Expense Analyzer");
        title.setFont(FONT_TITLE);
        title.setForeground(TEXT_MAIN);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);
        left.add(icon);
        left.add(title);

        totalLabel = new JLabel("Total: ₹0.00");
        totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        totalLabel.setForeground(ACCENT);

        JButton monthlyBtn = buildButton("📅  Monthly Tracker", new Color(154, 117, 234));
        monthlyBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        monthlyBtn.addActionListener(e -> new MonthlyTracker().setVisible(true));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        right.setOpaque(false);
        right.add(monthlyBtn);
        right.add(totalLabel);

        header.add(left,  BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    // ── Center Split ───────────────────────────────────────────
    private JSplitPane buildCenter() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildLeftPanel(), buildRightPanel());
        split.setDividerLocation(340);
        split.setDividerSize(4);
        split.setBorder(null);
        split.setBackground(BG_DARK);
        split.setContinuousLayout(true);
        return split;
    }

    // ── Left Panel: Form + Chart ───────────────────────────────
    private JPanel buildLeftPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 8));

        panel.add(buildFormCard(), BorderLayout.NORTH);
        panel.add(buildChartCard(), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildFormCard() {
        JPanel card = createCard("Add Expense");

        String[] categories = {"Food", "Transport", "Shopping", "Entertainment", "Health", "Education", "Utilities", "Other"};
        categoryCombo = styledCombo(categories);

        // Date Spinner
        dateSpinner = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd");
        dateSpinner.setEditor(dateEditor);
        dateSpinner.setValue(new java.util.Date());
        dateSpinner.setBackground(BG_INPUT);
        dateSpinner.setForeground(TEXT_MAIN);
        dateSpinner.setFont(FONT_BODY);
        dateSpinner.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COL, 1, true),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)
        ));
        dateSpinner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        // Style the inner editor text field
        dateEditor.getTextField().setBackground(BG_INPUT);
        dateEditor.getTextField().setForeground(TEXT_MAIN);
        dateEditor.getTextField().setCaretColor(ACCENT);
        dateEditor.getTextField().setFont(FONT_BODY);
        dateEditor.getTextField().setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        dateEditor.getTextField().setHorizontalAlignment(JTextField.LEFT);

        amountField = styledField("0.00");
        descField   = styledField("Optional note...");

        addFormRow(card, "📅  Date",       dateSpinner);
        addFormRow(card, "🏷  Category",   categoryCombo);
        addFormRow(card, "₹  Amount",      amountField);
        addFormRow(card, "📝  Description", descField);

        JButton addBtn = buildButton("+ Add Expense", SUCCESS);
        addBtn.addActionListener(e -> addExpense());
        card.add(Box.createVerticalStrut(10));
        card.add(addBtn);

        return wrapCard(card);
    }

    private JPanel buildChartCard() {
        JPanel card = createCard("Spending by Category");
        pieChart = new PieChartPanel();
        pieChart.setPreferredSize(new Dimension(280, 200));
        card.add(pieChart);
        return wrapCard(card);
    }

    // ── Right Panel: Filter + Table ────────────────────────────
    private JPanel buildRightPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 8, 16, 16));

        panel.add(buildToolbar(),      BorderLayout.NORTH);
        panel.add(buildTableCard(),    BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout(10, 0));
        bar.setBackground(BG_DARK);

        JLabel filterLabel = new JLabel("Filter by Category:");
        filterLabel.setFont(FONT_LABEL);
        filterLabel.setForeground(TEXT_DIM);

        String[] filterOpts = {"All", "Food", "Transport", "Shopping", "Entertainment", "Health", "Education", "Utilities", "Other"};
        filterCombo = styledCombo(filterOpts);
        filterCombo.addActionListener(e -> refreshTable((String) filterCombo.getSelectedItem()));

        JButton delBtn = buildButton("🗑 Delete Selected", DANGER);
        delBtn.addActionListener(e -> deleteSelected());

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setBackground(BG_DARK);
        left.add(filterLabel);
        left.add(filterCombo);

        bar.add(left,   BorderLayout.WEST);
        bar.add(delBtn, BorderLayout.EAST);
        return bar;
    }

    private JPanel buildTableCard() {
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

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(BG_CARD);
        card.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COL, 1, true),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

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

        // Column widths
        int[] widths = {50, 110, 110, 100, 0};
        for (int i = 0; i < widths.length; i++) {
            if (widths[i] > 0)
                table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        // Alternating row renderer
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

    // ── DB Operations ──────────────────────────────────────────
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    private void addExpense() {
        try {
            java.util.Date picked = (java.util.Date) dateSpinner.getValue();
            java.sql.Date sqlDate = new java.sql.Date(picked.getTime());
            double amount = Double.parseDouble(amountField.getText());

            try (Connection con = getConnection()) {
                PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO expenses(expense_date,category,amount,description) VALUES(?,?,?,?)");
                ps.setDate(1, sqlDate);
                ps.setString(2, (String) categoryCombo.getSelectedItem());
                ps.setDouble(3, amount);
                ps.setString(4, descField.getText());
                ps.executeUpdate();
            }

            amountField.setText("");
            descField.setText("");
            setStatus("✅  Expense added successfully.");
            refreshTable((String) filterCombo.getSelectedItem());

        } catch (NumberFormatException ex) {
            showError("Amount must be a valid number.");
        } catch (SQLException ex) {
            showError("DB error: " + ex.getMessage());
        }
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) { showError("Select a row to delete."); return; }

        int id = Integer.parseInt(tableModel.getValueAt(row, 0).toString());
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete expense ID " + id + "?", "Confirm Delete",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection con = getConnection()) {
            PreparedStatement ps = con.prepareStatement("DELETE FROM expenses WHERE id=?");
            ps.setInt(1, id);
            ps.executeUpdate();
            setStatus("🗑  Expense #" + id + " deleted.");
            refreshTable((String) filterCombo.getSelectedItem());
        } catch (SQLException ex) {
            showError("DB error: " + ex.getMessage());
        }
    }

    private void refreshTable(String category) {
        tableModel.setRowCount(0);
        Map<String, Double> catTotals = new LinkedHashMap<>();
        double total = 0;

        String sql = "All".equals(category)
                ? "SELECT * FROM expenses ORDER BY expense_date DESC"
                : "SELECT * FROM expenses WHERE category=? ORDER BY expense_date DESC";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            if (!"All".equals(category)) ps.setString(1, category);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                double amt = rs.getDouble("amount");
                String cat = rs.getString("category");
                total += amt;
                catTotals.merge(cat, amt, Double::sum);
                tableModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("expense_date"),
                        cat,
                        String.format("%.2f", amt),
                        rs.getString("description")
                });
            }

            totalLabel.setText(String.format("Total: ₹%.2f", total));
            pieChart.setData(catTotals);
            setStatus("📊  " + tableModel.getRowCount() + " record(s) loaded.");

        } catch (SQLException ex) {
            showError("DB error: " + ex.getMessage());
        }
    }

    // ── Helpers ────────────────────────────────────────────────
    private void setStatus(String msg) { statusLabel.setText(msg); }
    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private JPanel createCard(String title) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(BG_CARD);

        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbl.setForeground(TEXT_DIM);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        card.add(lbl);
        return card;
    }

    private JPanel wrapCard(JPanel inner) {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(BG_CARD);
        wrap.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COL, 1, true),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));
        wrap.add(inner, BorderLayout.CENTER);
        return wrap;
    }

    private void addFormRow(JPanel card, String label, JComponent field) {
        JLabel lbl = new JLabel(label);
        lbl.setFont(FONT_LABEL);
        lbl.setForeground(TEXT_DIM);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(lbl);
        card.add(Box.createVerticalStrut(4));
        card.add(field);
        card.add(Box.createVerticalStrut(10));
    }

    private JTextField styledField(String placeholder) {
        JTextField field = new JTextField() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && !isFocusOwner()) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setColor(TEXT_DIM);
                    g2.setFont(getFont().deriveFont(Font.ITALIC));
                    g2.drawString(placeholder, 10, getHeight() / 2 + 5);
                }
            }
        };
        field.setBackground(BG_INPUT);
        field.setForeground(TEXT_MAIN);
        field.setCaretColor(ACCENT);
        field.setFont(FONT_BODY);
        field.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COL, 1, true),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        field.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { field.setBorder(new CompoundBorder(new LineBorder(ACCENT, 1, true), BorderFactory.createEmptyBorder(8, 10, 8, 10))); }
            public void focusLost(FocusEvent e)   { field.setBorder(new CompoundBorder(new LineBorder(BORDER_COL, 1, true), BorderFactory.createEmptyBorder(8, 10, 8, 10))); }
        });
        return field;
    }

    private <T> JComboBox<T> styledCombo(T[] items) {
        JComboBox<T> combo = new JComboBox<>(items);
        combo.setBackground(BG_INPUT);
        combo.setForeground(new Color(24, 8, 128));
        combo.setFont(FONT_BODY);
        combo.setBorder(new LineBorder(BORDER_COL, 1, true));
        combo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
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
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        return btn;
    }

    // ── Pie Chart ──────────────────────────────────────────────
    static class PieChartPanel extends JPanel {
        private Map<String, Double> data = new LinkedHashMap<>();

        PieChartPanel() {
            setBackground(BG_CARD);
            setOpaque(false);
        }

        void setData(Map<String, Double> d) {
            this.data = d;
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (data.isEmpty()) {
                g2.setColor(TEXT_DIM);
                g2.setFont(FONT_SMALL);
                String msg = "No data yet";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, getHeight() / 2);
                return;
            }

            double total = data.values().stream().mapToDouble(Double::doubleValue).sum();
            int size = Math.min(getWidth(), getHeight() - 60) - 20;
            int x = (getWidth() - size) / 2;
            int y = 10;
            double start = -90;
            int idx = 0;

            List<String> keys = new ArrayList<>(data.keySet());
            for (String key : keys) {
                double pct = data.get(key) / total;
                double arc  = pct * 360;
                Color col = CAT_COLORS[idx % CAT_COLORS.length];

                g2.setColor(col);
                g2.fill(new Arc2D.Double(x, y, size, size, start, arc, Arc2D.PIE));
                g2.setColor(BG_CARD);
                g2.setStroke(new BasicStroke(2));
                g2.draw(new Arc2D.Double(x, y, size, size, start, arc, Arc2D.PIE));

                start += arc;
                idx++;
            }

            // Legend
            int legendY = y + size + 14;
            int lx = 6;
            idx = 0;
            for (String key : keys) {
                if (lx + 140 > getWidth()) { lx = 6; legendY += 18; }
                Color col = CAT_COLORS[idx % CAT_COLORS.length];
                g2.setColor(col);
                g2.fillRoundRect(lx, legendY, 10, 10, 3, 3);
                g2.setColor(TEXT_DIM);
                g2.setFont(FONT_SMALL);
                double pct = data.get(key) / total * 100;
                g2.drawString(String.format("%s %.0f%%", key, pct), lx + 14, legendY + 9);
                lx += 130;
                idx++;
            }
        }
    }
}