import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

import org.postgresql.ds.PGSimpleDataSource;

public class TestDB {
    public static void main(String[] args) {
        UamsDAO uamsDAO = new UamsDAO();
        testLogin(uamsDAO);
        testSaveStudent(uamsDAO);
        testUpdateStudent(uamsDAO);
        testGetStudentInfo(uamsDAO);

    }

    public static void testLogin(UamsDAO uamsDAO) {
        UUID sessionID = uamsDAO.loginWithSecurityAnswer("jphan07", "lol", "What is your mother's maiden name?", "Phan");
        uamsDAO.createUser(sessionID, new User("jacob", "lol", User.ROLE.STUDENT, new String[]{"Phan", "lol", "lol"}, "jphan07@arizona.edu", true));
    }

    public static void testSaveStudent(UamsDAO uamsDAO) {
        UUID sessionID = uamsDAO.loginWithSecurityAnswer("jphan07", "lol", "What is your mother's maiden name?", "Phan");

        if (sessionID == null) {
            System.out.println("Failed to login");
        }
        //public boolean saveStudentInfo(UUID userSession, Student student)

        Student student = new Student("IT_account", "test_netID"
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

        Student student = new Student("IT_account", "new_netID"
                , "new_firstName", "new_lastName", "new_ethnicity",
                "new_gender", "new_major", "new_Year",
                3.5, true);
        String newUsername = "IT_account";
        String newNetID = "new_netID";
        String newFirstName = "new_firstName";
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
        Student student_got = uamsDAO.getStudentInfo(sessionID, "IT_account");
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
}

