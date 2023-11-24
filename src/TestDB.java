import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.postgresql.ds.PGSimpleDataSource;

public class TestDB {
    public static void main(String[] args) {
        UamsDAO uamsDAO = new UamsDAO();
        testLogin(uamsDAO);
        testSaveApplication(uamsDAO);
        testGetApplication(uamsDAO);

    }

    public static void testLogin(UamsDAO uamsDAO) {
        UUID sessionID = uamsDAO.loginWithSecurityAnswer("jphan07", "lol", "What is your mother's maiden name?", "Phan");
        uamsDAO.createUser(sessionID, new User("jacob", "lol", User.ROLE.STUDENT, new String[]{"Phan", "lol", "lol"}, "jphan07@arizona.edu", true));
    }

    /**
     * Tester for both UamsDAO
     * saveApplication() and
     * getApplication() functions
     *
     * @param uamsDAO
     */
    public static void testSaveApplication(UamsDAO uamsDAO) {
        // login to existing user
        UUID sessionID = uamsDAO.loginWithSecurityAnswer("jphan07", "lol", "What is your mother's maiden name?", "Phan");
        if (sessionID == null) {
            System.out.println("User not in db.");
            return;
        }

        // get an existing local PC file "testfile.txt" on local desktop
        String desktopPath = System.getProperty("user.home") + "\\Desktop";
        File testfile = new File(desktopPath, "testfile.txt");
        if (!testfile.exists()) {
            System.out.println("testfile.txt does not exist on local PC");
            return;
        }

        // create a test application
        String[] responses = {"response1", "lol2", "something3", "1a", "2b", "3c", "lmao", "4d", "5e", "6f"};
        Application application = new Application("jphan07", UUID.fromString("118c6269-5dbf-4efd-9e28-c178dd887319"), responses, testfile);

        // attempt saving
        boolean isSaved = uamsDAO.saveApplication(sessionID, application);
        if (!isSaved) {
            System.out.println("Failed to save application.");
            return;
        }


    }

    public static void testGetApplication(UamsDAO uamsDAO) {
        // login to existing user
        UUID sessionID = uamsDAO.loginWithSecurityAnswer("jphan07", "lol", "What is your mother's maiden name?", "Phan");
        if (sessionID == null) {
            System.out.println("User not in db.");
            return;
        }

        // get existing application
        Application application_got = uamsDAO.getApplication(sessionID, "jphan07", UUID.fromString("118c6269-5dbf-4efd-9e28-c178dd887319"));
        if (application_got == null) {
            System.out.println("Failed to get application.");
            return;
        }

        //print application_got
        System.out.println("\nScholarshipID: %s".formatted(application_got.getScholarshipID()));
        System.out.println("StudentUsername: %s".formatted(application_got.getUsername()));
        System.out.println("Responses: %s".formatted(String.join(", ", application_got.getResponses())));
        System.out.println("PathToFileUpload: %s".formatted(application_got.getUploadedFilePath()));
    }

}
