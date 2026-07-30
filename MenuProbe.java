import java.sql.*;

public class MenuProbe {
  public static void main(String[] args) throws Exception {
    String url = "jdbc:postgresql://localhost:5433/maharecruitment";
    String user = "postgres";
    String pass = "postgres";
    try (Connection con = DriverManager.getConnection(url, user, pass)) {
      String[] queries = {
        "select menu_id, menu_name_english, menu_name_marathi, url, is_active, is_sub_menu from mst_menu where upper(url) = upper('/hr/employees') or upper(menu_name_english) like upper('%Onboarded%') order by menu_id",
        "select mr.menu_id, mr.id as role_id, r.name as role_name, m.menu_name_english, m.url from menu_role mr join roles r on r.id = mr.id join mst_menu m on m.menu_id = mr.menu_id where upper(m.url) = upper('/hr/employees') order by mr.id",
        "select m.menu_id, m.menu_name_english, m.url from mst_menu m join menu_role mr on mr.menu_id = m.menu_id join roles r on r.id = mr.id where upper(r.name)=upper('ROLE_HR') order by m.menu_id",
        "select m.menu_id, m.menu_name_english, m.url from mst_menu m join menu_role mr on mr.menu_id = m.menu_id join roles r on r.id = mr.id where upper(r.name)=upper('HR') order by m.menu_id"
      };
      for (String sql : queries) {
        System.out.println("SQL> " + sql);
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
          ResultSetMetaData md = rs.getMetaData();
          int cc = md.getColumnCount();
          int row = 0;
          while (rs.next()) {
            row++;
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i <= cc; i++) {
              if (i > 1) sb.append(" | ");
              sb.append(md.getColumnLabel(i)).append('=').append(rs.getString(i));
            }
            System.out.println(sb);
          }
          if (row == 0) {
            System.out.println("<no rows>");
          }
        }
        System.out.println();
      }
    }
  }
}
