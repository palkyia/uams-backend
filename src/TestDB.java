import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.io.File;
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
        System.out.println("Testing UamsDAO...");


        System.out.println("\n\nTesting update user info...");
        testUpdateUserInfo(uamsDAO);
        System.out.println("\n\nTesting save application...");
        testSaveApplication(uamsDAO);
        System.out.println("\n\nTesting get application...");
        testGetApplication(uamsDAO);
        System.out.println("\n\nTesting save student...");
        testSaveStudent(uamsDAO);
        System.out.println("\n\nTesting update student...");
        testUpdateStudent(uamsDAO);
        System.out.println("\n\nTesting get student info...");
        testGetStudentInfo(uamsDAO);
        System.out.println("\n\nTesting get user info...");
        testGetUserInfo(uamsDAO);
        System.out.println("\n\nTesting create scholarship...");
        testCreateScholarship(uamsDAO);
        System.out.println("\n\nTesting retrieve scholarship...");
        testRetrieveScholarship(uamsDAO);
        System.out.println("\n\nTesting update scholarship...");
        testUpdateScholarship(uamsDAO);
        System.out.println("\n\nTesting remove scholarships by year...");
        testRemoveScholarshipsByYear(uamsDAO);

    }

    public static void testUpdateUserInfo(UamsDAO uamsDAO) {
        UUID sessionID = uamsDAO.loginWithSecurityAnswer("jphan07", "lol", "What is your mother's maiden name?", "Phan");
        uamsDAO.createUser(sessionID, new User("IT_account", "IT_password", User.ROLE.IT, new String[]{"IT", "IT", "IT"}, "it@arizona.edu", true));

        // IT people log in
        UUID sessionIDIT = uamsDAO.loginWithSecurityAnswer("IT_account", "IT_password", "What is your mother's maiden name?", "IT");
        // IT people modifies the user info
        uamsDAO.modifyUser(sessionIDIT, new User("jacob", "lol", User.ROLE.STUDENT, new String[]{"Phan", "lol", "lol"}), "jacob", "newpassword", new String[]{"1a", "2a", "3a"}, User.ROLE.PROVIDER);
    }

    public static void testCreateScholarship(UamsDAO uamsDAO) {
        UUID sessionID = uamsDAO.loginWithSecurityAnswer("jphan07", "lol", "What is your mother's maiden name?", "Phan");
        String desktopPath = System.getProperty("user.home") + "\\Desktop";
        File testfile = new File(desktopPath, "testfile.txt");
        Calendar calendar = Calendar.getInstance();
        calendar.set(2022, Calendar.JANUARY, 1);
        Date date = new java.sql.Date(calendar.getTimeInMillis());
        Scholarship newscholarship = new Scholarship(UUID.randomUUID(), "Trash Scholarship", "This is outdated", new String[]{"custom"},
                date, false, true, true, true, true, false, false,
                true, true, testfile, 10000);
        uamsDAO.CreateScholarship(sessionID, newscholarship);

    }

    public static void testRetrieveScholarship(UamsDAO uamsDAO) {
        UUID sessionID = uamsDAO.loginWithSecurityAnswer("jphan07", "lol", "What is your mother's maiden name?", "Phan");
        Scholarship result = uamsDAO.RetrieveScholarshipByID(sessionID, UUID.fromString("8c752c0a-d3f8-445e-9c9b-95fc2b9cead3"));
        System.out.println(result);
        ArrayList<Scholarship> results = new ArrayList<>();
        results = uamsDAO.RetrieveScholarshipsByName(sessionID, "Trash");
        System.out.println(results);
        ArrayList<String> myKeywords = new ArrayList<>();
        myKeywords.add("Test");
        myKeywords.add("is");
        results = uamsDAO.RetrieveScholarshipsByKeywords(sessionID, myKeywords);
        System.out.println(results);

    }

    public static void testUpdateScholarship(UamsDAO uamsDAO) {
        UUID sessionID = uamsDAO.loginWithSecurityAnswer("jphan07", "lol", "What is your mother's maiden name?", "Phan");
        uamsDAO.UpdateScholarshipForm(sessionID, UUID.fromString("8c752c0a-d3f8-445e-9c9b-95fc2b9cead3"), "description", "haha");
        uamsDAO.UpdateScholarshipForm(sessionID, UUID.fromString("8c752c0a-d3f8-445e-9c9b-95fc2b9cead3"), "is_required_email", "false");
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
        Application application = new Application("jphan07", UUID.fromString("118c6269-5dbf-4efd-9e28-c178dd887319"), responses, testfile, false);

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


    public static void testSaveStudent(UamsDAO uamsDAO) {
        UUID sessionID = uamsDAO.loginWithSecurityAnswer("jphan07", "lol", "What is your mother's maiden name?", "Phan");

        if (sessionID == null) {
            System.out.println("Failed to login");
        }
        //public boolean saveStudentInfo(UUID userSession, Student student)


        Student student = new Student("new_student", "test_netID"
                , "test_firstName", "test_lastName", "test_ethnicity",
                "test_gender", "test_major", "test_Year",
                4.0, true);


        boolean isStored = uamsDAO.saveStudentInfo(sessionID, student);
        if (!isStored) {
            System.out.println("Failed to store Student information");
        }

    }

    public static void testUpdateStudent(UamsDAO uamsDAO) {
        UUID sessionID = uamsDAO.loginWithSecurityAnswer("jphan07", "lol", "What is your mother's maiden name?", "Phan");

        //updateStudentInfo(UUID userSession, Student existingStudent, String newUsername,
        //      String newNetID, String newFirstName, String newLastName,
        //    String newEthnicity, String newGender, String newMajor,
        //  String newSchoolYear, double newGpa, boolean newCitizenshipStatus)

        Student student = new Student("new_student", "test_netID"
                , "test_firstName", "test_lastName", "test_ethnicity",
                "test_gender", "test_major", "test_Year",
                4.0, true);

        String newUsername = "new_student";
        String newNetID = "UPDATED_NETID";
        String newFirstName = "updated_firstName";
        String newLastName = "new_lastName";
        String newEthnicity = "new_ethnicity";
        String newGender = "new_gender";
        String newMajor = "new_major";
        String newSchoolYear = "new_schoolYear";
        double newGpa = 3.5;
        boolean newCitizenshipStatus = true;

        uamsDAO.updateStudentInfo(sessionID, student, newUsername, newNetID, newFirstName, newLastName,
                newEthnicity, newGender, newMajor, newSchoolYear, newGpa, newCitizenshipStatus);

    }

    public static void testGetStudentInfo(UamsDAO uamsDAO) {
        UUID sessionID = uamsDAO.loginWithSecurityAnswer("jphan07", "lol", "What is your mother's maiden name?", "Phan");
        Student student_got = uamsDAO.getStudentInfo(sessionID, "new_student");
        if (student_got == null) {
            System.out.println("Failed to get student information");
        }

        //Print student information
        System.out.println("Username: " + student_got.getUsername());
        System.out.println("NetID: " + student_got.getNetID());
        System.out.println("First Name: " + student_got.getFirstName());
        System.out.println("Last Name: " + student_got.getLastName());
        System.out.println("Ethnicity: " + student_got.getEthnicity());
        System.out.println("Gender: " + student_got.getGender());
        System.out.println("Major: " + student_got.getMajor());
        System.out.println("School Year: " + student_got.getSchoolYear());
        System.out.println("GPA: " + student_got.getGpa());
        System.out.println("Citizenship Status: " + student_got.getCitizenshipStatus());

    }

    public static void testGetUserInfo(UamsDAO uamsDAO) {
        UUID sessionID = uamsDAO.loginWithSecurityAnswer("jphan07", "lol", "What is your mother's maiden name?", "Phan");
        User user = uamsDAO.getUserInfo(sessionID, "IT_account");
        if (user == null) {
            System.out.println("Failed to get user information");
        }


        System.out.println("Username: " + user.getUsername());
        System.out.println("Password: " + user.getPassword());
        System.out.println("User role: " + user.getRole());
        System.out.println("Security answers: " + user.getSecurityAnswersAsString());
        System.out.println("Is enabled: " + user.isEnabled());
        System.out.println("Email: " + user.getEmail());
    }

    public static void testRemoveScholarshipsByYear(UamsDAO uamsDAO) {
        UUID sessionID = uamsDAO.loginWithSecurityAnswer("jphan07", "lol", "What is your mother's maiden name?", "Phan");
        uamsDAO.RemoveScholarshipsYearly(sessionID, 2022);
    }

}
