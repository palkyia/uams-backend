import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.postgresql.ds.PGSimpleDataSource;

public class TestDB {
    public static void main(String[] args) {
        UamsDAO uamsDAO = new UamsDAO();
        //testLogin(uamsDAO);
        testCreateScholarship(uamsDAO);
        //testRetrieveScholarship(uamsDAO);
        //testUpdateScholarship(uamsDAO);

    }

    public static void testLogin(UamsDAO uamsDAO) {
        UUID sessionID = uamsDAO.loginWithSecurityAnswer("jphan07", "lol", "What is your mother's maiden name?", "Phan");
        uamsDAO.createUser(sessionID, new User("jacob", "lol", User.ROLE.STUDENT, new String[]{"Phan", "lol", "lol"}, true));
    }

    public static void testCreateScholarship(UamsDAO uamsDAO){
        UUID sessionID = uamsDAO.loginWithSecurityAnswer("jphan07", "lol", "What is your mother's maiden name?", "Phan");
        uamsDAO.CreateScholarship(sessionID, "Dummy scholarship", "11/21/2023", "This is a test scholarship", new String[]{"custom, maybe file location"}, true, true, true, false, false, true, true, false, false);
    }

    public static void testRetrieveScholarship(UamsDAO uamsDAO){
        UUID sessionID = uamsDAO.loginWithSecurityAnswer("jphan07", "lol", "What is your mother's maiden name?", "Phan");
        ResultSet result = uamsDAO.RetrieveScholarshipForm(sessionID, "Jacob Scholarship", "2024-01-27");
        System.out.println(result);
    }
    public static void testUpdateScholarship(UamsDAO uamsDAO){
        UUID sessionID = uamsDAO.loginWithSecurityAnswer("jphan07", "lol", "What is your mother's maiden name?", "Phan");
        uamsDAO.UpdateScholarshipForm(sessionID, "Jacob Scholarship", "2024-01-27", "description", "haha");
        uamsDAO.UpdateScholarshipForm(sessionID, "Jacob Scholarship", "2024-01-27", "is_required_email", "false");
    }
}
