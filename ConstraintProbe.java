import java.sql.*;
public class ConstraintProbe {
  public static void main(String[] args) throws Exception {
    String url = "jdbc:postgresql://localhost:5433/maharecruitment";
    String user = "postgres";
    String pass = "postgres";
    try (Connection con = DriverManager.getConnection(url, user, pass);
         PreparedStatement ps = con.prepareStatement("select conname, pg_get_constraintdef(oid) as def from pg_constraint where conname ilike 'department_project_application%status%check' order by conname")) {
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          System.out.println(rs.getString("conname") + " => " + rs.getString("def"));
        }
      }
    }
  }
}
