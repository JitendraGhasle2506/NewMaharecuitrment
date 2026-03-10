import java.sql.*;
public class ConstraintFix {
  public static void main(String[] args) throws Exception {
    String url = "jdbc:postgresql://localhost:5433/maharecruitment";
    String user = "postgres";
    String pass = "postgres";
    String allowed = "'DRAFT','SUBMITTED','UNDER_REVIEW','APPROVED','REJECTED','SUBMITTED_TO_HR','HR_SENT_BACK','CORRECTED_BY_DEPARTMENT','HR_APPROVED','HR_REJECTED','AUDITOR_REVIEW','AUDITOR_SENT_BACK','AUDITOR_APPROVED','COMPLETED'";
    String[] sql = new String[] {
      "ALTER TABLE department_project_application DROP CONSTRAINT IF EXISTS department_project_application_application_status_check",
      "ALTER TABLE department_project_application ADD CONSTRAINT department_project_application_application_status_check CHECK (application_status IN (" + allowed + "))",
      "ALTER TABLE department_project_application_activity DROP CONSTRAINT IF EXISTS department_project_application_activity_previous_status_check",
      "ALTER TABLE department_project_application_activity ADD CONSTRAINT department_project_application_activity_previous_status_check CHECK (previous_status IN (" + allowed + "))",
      "ALTER TABLE department_project_application_activity DROP CONSTRAINT IF EXISTS department_project_application_activity_new_status_check",
      "ALTER TABLE department_project_application_activity ADD CONSTRAINT department_project_application_activity_new_status_check CHECK (new_status IN (" + allowed + "))"
    };

    try (Connection con = DriverManager.getConnection(url, user, pass); Statement st = con.createStatement()) {
      con.setAutoCommit(false);
      for (String q : sql) {
        st.execute(q);
      }
      con.commit();
      System.out.println("Constraint update applied successfully.");

      try (ResultSet rs = st.executeQuery("select conname, pg_get_constraintdef(oid) from pg_constraint where conname ilike 'department_project_application%status%check' order by conname")) {
        while (rs.next()) {
          System.out.println(rs.getString(1) + " => " + rs.getString(2));
        }
      }
    }
  }
}
