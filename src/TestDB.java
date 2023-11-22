import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

import org.postgresql.ds.PGSimpleDataSource;

public class TestDB {
    public static void main(String[] args) {
        UamsDAO uamsDAO = new UamsDAO();
        testLogin(uamsDAO);
        testUpdateUserInfo(uamsDAO);
    }

    public static void testLogin(UamsDAO uamsDAO) {
        UUID sessionID = uamsDAO.loginWithSecurityAnswer("jphan07", "lol", "What is your mother's maiden name?", "Phan");
        uamsDAO.createUser(sessionID, new User("jacob", "lol", User.ROLE.STUDENT, new String[]{"Phan", "lol", "lol"}, "jphan07@arizona.edu", true));
    }

    public static void testUpdateUserInfo(UamsDAO uamsDAO) {
        UUID sessionID = uamsDAO.loginWithSecurityAnswer("jphan07", "lol", "What is your mother's maiden name?", "Phan");
        uamsDAO.createUser(sessionID, new User("IT_account", "IT_password", User.ROLE.IT, new String[]{"IT", "IT", "IT"}, "it@arizona.edu", true));

        // IT people log in
        UUID sessionIDIT = uamsDAO.loginWithSecurityAnswer("IT_account", "IT_password", "What is your mother's maiden name?", "IT");
        // IT people modifies the user info
        uamsDAO.modifyUser(sessionIDIT, new User("jacob", "lol", User.ROLE.STUDENT, new String[]{"Phan", "lol", "lol"}), "jacob", "newpassword", new String[]{"1a", "2a", "3a"}, User.ROLE.PROVIDER);
    }

}
