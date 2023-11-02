import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.postgresql.ds.PGSimpleDataSource;

public class TestDB {
    public static void main(String[] args) {


        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setUrl("jdbc:postgresql://potent-markhor-10752.6wr.cockroachlabs.cloud:26257/defaultdb?sslmode=verify-full");
        ds.setUser("jacob");
        ds.setPassword("<ENTER-SQL-USER-PASSWORD>");

    }
}
