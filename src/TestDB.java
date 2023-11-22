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
        uamsDAO.updateUserInfo(sessionID, new User("jacob", "lol", User.ROLE.STUDENT, new String[]{"Phan", "lol", "lol"}), "updateJacob", "newpassword", new String[]{"1", "2", "3"}, User.ROLE.STUDENT);
    }

}
