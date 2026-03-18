import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Scanner;

public class DeleteExpense {
    public static void main(String[] args) {
        String url="jdbc:mysql://localhost:3306/expense_db";
        String username="root";
        String password="Vinay#371708";

        Scanner sc=new Scanner(System.in);
        System.out.print("Enter Expense ID to delete: ");
        int id=sc.nextInt();

        try{
            Connection con=DriverManager.getConnection(url,username,password);
            String query="DELETE FROM expenses WHERE id=?";
            PreparedStatement ps=con.prepareStatement(query);
            ps.setInt(1,id);
            int rows=ps.executeUpdate();
            if(rows>0){
                System.out.println("Expense deleted successfully");
            }else{
                System.out.println("Expense ID not found");
            }
            con.close();
        }catch(Exception e){
            e.printStackTrace();
        }
        sc.close();
    }
}
