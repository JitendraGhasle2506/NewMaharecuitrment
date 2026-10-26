import java.lang.reflect.Proxy;
import java.sql.DriverManager;
import org.flywaydb.core.api.migration.Context;
import db.postmigration.V157__employee_pip_reason;

class CheckPipReasonMigration {
    public static void main(String[] args) throws Exception {
        try (var connection=DriverManager.getConnection("jdbc:h2:mem:pip_reason;MODE=PostgreSQL", "sa", "")) {
            try(var sql=connection.createStatement()) {
                sql.execute("create table employee_pip_plan (id bigint primary key, objectives varchar(2000))");
                sql.execute("insert into employee_pip_plan values (1, 'Original objectives')");
            }
            Context context=(Context)Proxy.newProxyInstance(Context.class.getClassLoader(),new Class<?>[]{Context.class},
                    (proxy,method,parameters)->method.getName().equals("getConnection")?connection:null);
            var migration=new V157__employee_pip_reason();
            migration.migrate(context);
            try(var sql=connection.createStatement();var row=sql.executeQuery("select objectives, reason from employee_pip_plan where id=1")) {
                if(!row.next() || !"Original objectives".equals(row.getString(1)) || row.getString(2)!=null)
                    throw new AssertionError("Existing PIP history was changed");
            }
            try(var sql=connection.createStatement()) { sql.execute("update employee_pip_plan set reason='Missed deadlines' where id=1"); }
            migration.migrate(context);
            try(var sql=connection.createStatement();var row=sql.executeQuery("select reason from employee_pip_plan where id=1")) {
                if(!row.next() || !"Missed deadlines".equals(row.getString(1)))throw new AssertionError("Repeated migration changed the saved reason");
            }
            System.out.println("PASS: V157 preserves existing plans, adds nullable reason, and safely handles an existing column.");
        }
    }
}