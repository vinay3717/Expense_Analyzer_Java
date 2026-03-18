import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
public class AddExpense {
    public static void main(String[] args) {
        String url="jdbc:mysql://localhost:3306/expense_db";
        String username="root";
        String password="Vinay#371708";

        try{
            Connection con=DriverManager.getConnection(url,username,password);
            String query="INSERT INTO expenses(expense_date,category,amount,description) VALUES(?,?,?,?)";
            PreparedStatement ps=con.prepareStatement(query);

            ps.setDate(1,java.sql.Date.valueOf("2026-03-10"));
            ps.setString(2,"Food");
            ps.setDouble(3,250);
            ps.setString(4,"Lunch");

            ps.executeUpdate();

            System.out.println("Expense Added Successfully");

            con.close();
        }catch(SQLException e){
            e.printStackTrace();
        }
    }
}
