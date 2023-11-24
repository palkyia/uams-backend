import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

public class TestDB {
    public static void main(String[] args) {
        UamsDAO uamsDAO = new UamsDAO();
        test(uamsDAO);

    }

    public static void testLogin(UamsDAO uamsDAO) {
        UUID sessionID = uamsDAO.loginWithSecurityAnswer("jphan07", "lol", "What is your mother's maiden name?", "Phan");
        uamsDAO.createUser(sessionID, new User("jacob", "lol", User.ROLE.STUDENT, new String[]{"Phan", "lol", "lol"}, "jphan07@arizona.edu", true));
    }

    public static void test(UamsDAO uamsDAO) {


    }

    public static void testAutomatedDeadlineNotification(UamsDAO uamsDAO) {
        UUID sessionID = uamsDAO.loginWithSecurityAnswer("jphan07", "lol", "What is your mother's maiden name?", "Phan");
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        Date date = new java.sql.Date(calendar.getTimeInMillis());
        // TODO: use update/create scholarship methods to set time

        uamsDAO.checkAndNotifyDeadlines();
    }

}
