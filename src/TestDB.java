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
        testSaveGetApplication(uamsDAO);

    }

    public static void testLogin(UamsDAO uamsDAO) {
        UUID sessionID = uamsDAO.loginWithSecurityAnswer("jphan07", "lol", "What is your mother's maiden name?", "Phan");
        uamsDAO.createUser(sessionID, new User("jacob", "lol", User.ROLE.STUDENT, new String[]{"Phan", "lol", "lol"}, true));
    }

    /**
     * Tester for both UamsDAO
     * saveApplication() and
     * getApplication() functions
     *
     * @param uamsDAO
     */
    public static void testSaveGetApplication(UamsDAO uamsDAO) {
        // login to existing user
        UUID sessionID = uamsDAO.loginWithSecurityAnswer("jphan07", "lol", "What is your mother's maiden name?", "Phan");
        if (sessionID == null) {
            System.out.println("User not in db.");
            return;
        }

        // get an existing scholarship_form's uuid
        UUID scholarshipID = UUID.fromString("118c6269-5dbf-4efd-9e28-c178dd887319"); //TODO: Unsafe. Replace w/ a uamsDAO.getScholarship().getScholarshipID() function

        // create a test application
        String[] responses = {"response1", "lol2", "something3", "1a", "2b", "3c", "lmao", "4d", "5e", "6f"};
        Application application = new Application("jphan07", scholarshipID, responses, "[legit server filepath]");

        // attempt saving
        boolean isSaved = uamsDAO.saveApplication(sessionID, application);
        if (!isSaved) {
            System.out.println("Failed to save application.");
            return;
        }

        // attempt getting
        Application application_got = uamsDAO.getApplication(sessionID, "jphan07", scholarshipID);
        if (application_got == null) {
            System.out.println("Failed to get application.");
            return;
        }


        //print application_got
        System.out.println("ScholarshipID: %s".formatted(application_got.getScholarshipID()));
        System.out.println("StudentUsername: %s".formatted(application_got.getUsername()));
        System.out.println("Responses: %s".formatted(String.join(", ", application_got.getResponses())));
        System.out.println("PathToFileUpload: %s".formatted(application_got.getUploadedFilePath()));

    }

}
