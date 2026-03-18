import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class ExpenseSummary {

    public static void main(String[] args) {

        String url = "jdbc:mysql://localhost:3306/expense_db";
        String username = "root";
        String password = "Vinay#371708";

        try {

            Connection con = DriverManager.getConnection(url, username, password);

            // TOTAL EXPENSE
            Statement st1 = con.createStatement();
            ResultSet rs1 = st1.executeQuery("SELECT SUM(amount) AS total FROM expenses");

            if(rs1.next()) {
                System.out.println("Total Expense: Rs " + rs1.getDouble("total"));
            }

            System.out.println("\nCategory-wise Expense:");

            // CATEGORY-WISE
            Statement st2 = con.createStatement();
            ResultSet rs2 = st2.executeQuery(
                    "SELECT category, SUM(amount) AS total FROM expenses GROUP BY category"
            );

            while(rs2.next()) {
                System.out.println(rs2.getString("category") + " : Rs " + rs2.getDouble("total"));
            }

            con.close();

        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}