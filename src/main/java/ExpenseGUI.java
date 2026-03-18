import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class ExpenseGUI {

    static String url = "jdbc:mysql://localhost:3306/expense_db";
    static String username = "root";
    static String password = "Vinay#371708";

    public static void main(String[] args) {

        JFrame frame = new JFrame("Expense Analyzer");
        frame.setSize(700, 500);
        frame.setLayout(new GridLayout(6, 2));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Labels & Fields
        JLabel dateLabel = new JLabel("Date (YYYY-MM-DD)");
        JTextField dateField = new JTextField();

        JLabel categoryLabel = new JLabel("Category");
        JTextField categoryField = new JTextField();

        JLabel amountLabel = new JLabel("Amount");
        JTextField amountField = new JTextField();

        JLabel descLabel = new JLabel("Description");
        JTextField descField = new JTextField();

        String[] columnNames = {"ID", "Date", "Category", "Amount", "Description"};

        JTable table = new JTable();
        JScrollPane scrollPane = new JScrollPane(table);

        JButton addButton = new JButton("Add Expense");
        JButton viewButton = new JButton("View Expenses");

        // ADD BUTTON
        addButton.addActionListener(e -> {
            try {
                Connection con = DriverManager.getConnection(url, username, password);

                String query = "INSERT INTO expenses(expense_date, category, amount, description) VALUES (?,?,?,?)";

                PreparedStatement ps = con.prepareStatement(query);

                ps.setDate(1, Date.valueOf(dateField.getText()));
                ps.setString(2, categoryField.getText());
                ps.setDouble(3, Double.parseDouble(amountField.getText()));
                ps.setString(4, descField.getText());

                ps.executeUpdate();

                JOptionPane.showMessageDialog(frame, "Expense Added Successfully");

                con.close();

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Error: " + ex.getMessage());
            }
        });

        // VIEW BUTTON (FIXED VERSION)
        viewButton.addActionListener(e -> {
            try {
                Connection con = DriverManager.getConnection(url, username, password);

                Statement st = con.createStatement();
                ResultSet rs = st.executeQuery("SELECT * FROM expenses");

                java.util.List<String[]> list = new java.util.ArrayList<>();

                while (rs.next()) {
                    list.add(new String[]{
                            String.valueOf(rs.getInt("id")),
                            rs.getString("expense_date"),
                            rs.getString("category"),
                            String.valueOf(rs.getDouble("amount")),
                            rs.getString("description")
                    });
                }

                String[][] data = list.toArray(new String[0][0]);

                table.setModel(new javax.swing.table.DefaultTableModel(data, columnNames));

                con.close();

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Error: " + ex.getMessage());
            }
        });

        // Add Components
        frame.add(dateLabel); frame.add(dateField);
        frame.add(categoryLabel); frame.add(categoryField);
        frame.add(amountLabel); frame.add(amountField);
        frame.add(descLabel); frame.add(descField);
        frame.add(addButton); frame.add(viewButton);
        frame.add(scrollPane);

        frame.setVisible(true);
    }
}