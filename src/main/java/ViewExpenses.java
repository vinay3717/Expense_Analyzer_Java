import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
public class ViewExpenses {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/expense_db";
        String username = "root";
        String password = "Vinay#371708";

        try {
            Connection con = DriverManager.getConnection(url, username, password);
            String query = "SELECT * FROM expenses";
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(query);
            System.out.println("ID | Date | Category | Amount | Description");
            System.out.println("-------------------------------------------------");

            while (rs.next()) {
                int id = rs.getInt("id");
                String date = rs.getString("expense_date");
                String category = rs.getString("category");
                double amount = rs.getDouble("amount");
                String description = rs.getString("description");

                System.out.println(id + " | " + date + " | " + category + " | " + amount + " | " + description);
            }
            con.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
