import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
public class ExpenseGUI {
    static String url="jdbc:mysql://localhost:3306/expense_db";
    static String username="root";
    static String password="Vinay#371708";

    public static void main(String[] args) {
        JFrame frame=new JFrame("Expense Analyzer");
        frame.setSize(400,400);
        frame.setLayout(new GridLayout(6,2));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Labels and fields
        JLabel dateLabel=new JLabel("Date (YYYY-MM-DD)");
        JTextField dateField=new JTextField();

        JLabel categoryLabel=new JLabel("Category");
        JTextField categoryField=new JTextField();

        JLabel amountLabel=new JLabel("Amount");
        JTextField amountField=new JTextField();

        JLabel descLabel=new JLabel("Description");
        JTextField descField=new JTextField();

        JButton addButton=new JButton("Add Expense");
        JButton viewButton=new JButton("View Expenses");

        JTextArea outputArea=new JTextArea();
        JScrollPane scroll=new JScrollPane(outputArea);

        addButton.addActionListener(e->{
            try{
                Connection con=DriverManager.getConnection(url,username,password);
                String query="INSERT INTO expenses(expense_date,category,amount,description) VALUES (?,?,?,?)";

                PreparedStatement ps=con.prepareStatement(query);

                ps.setDate(1,Date.valueOf(dateField.getText()));
                ps.setString(2,categoryField.getText());
                ps.setDouble(3,Double.parseDouble(amountField.getText()));
                ps.setString(4,descField.getText());

                ps.executeUpdate();
                outputArea.setText("Expenses added successfully!");
                con.close();
            }catch(Exception ex){
                outputArea.setText("Error: "+ex.getMessage());
            }
        });
        //View Button Logic
        viewButton.addActionListener(e->{
            try{
                Connection con=DriverManager.getConnection(url,username,password);

                Statement st=con.createStatement();
                ResultSet rs=st.executeQuery("SELECT * FROM expenses");

                StringBuilder data=new StringBuilder();

                while(rs.next()){
                    data.append(
                            rs.getInt("id")+" | "+
                                    rs.getString("expense_date")+" | "+
                                    rs.getString("category")+" | "+
                                    rs.getDouble("amount")+" | "+
                                    rs.getString("description")+"\n"
                    );
                }
                outputArea.setText(data.toString());
                con.close();
            }catch(Exception ex){
                outputArea.setText("Error: "+ex.getMessage());
            }
        });
        //Add Components
        frame.add(dateLabel); frame.add(dateField);
        frame.add(categoryLabel); frame.add(categoryField);
        frame.add(amountLabel); frame.add(amountField);
        frame.add(descLabel); frame.add(descField);
        frame.add(addButton); frame.add(viewButton);
        frame.add(scroll);

        frame.setVisible(true);
    }
}
