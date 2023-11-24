import org.postgresql.ds.PGSimpleDataSource;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;

public class UamsDAO {

    private static final String DB_URL = "jdbc:postgresql://potent-markhor-10752.6wr.cockroachlabs.cloud:26257/defaultdb?sslmode=require";
    private static final String DB_USER = "jacob";
    private static final String DB_PASSWORD = "w0IvQemMCK9jmWXA8efx-Q";

    private static final String[] securityQuestions = {
            "What is your mother's maiden name?",
            "What is the name of your first pet?",
            "What is the name of the high school you graduated from?",
    };
    private PGSimpleDataSource dataSource;
    private LoginSessionManager loginSessionManager;

    public UamsDAO() {
        dataSource = new PGSimpleDataSource();
        dataSource.setURL(DB_URL);
        dataSource.setUser(DB_USER);
        dataSource.setPassword(DB_PASSWORD);
        loginSessionManager = new LoginSessionManager();
    }

    public String getSecurityQuestion(String username) {
        Random random = new Random();
        return securityQuestions[random.nextInt(0, securityQuestions.length)];
    }

    public UUID loginWithSecurityAnswer(String username, String password, String securityQuestion, String securityAnswer) {
        if (Arrays.stream(securityQuestions).noneMatch(securityQuestion::equals)) {
            System.out.println("Invalid security question.");
            return null;
        }
        try (Connection connection = dataSource.getConnection()) {
            // Execute your SQL query by calling the executeQuery() method on the connection object with your SQL query string.
            // Make sure all strings are properly escaped with single quotes ' '
            ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM users WHERE username = '" + username + "' AND password = '" + password + "'");

            // Result sets contain one row of the table at a time. You can use the next() method to move to the next row in the result set.
            if (!resultSet.next()) {
                System.out.println("Invalid username or password.");
                return null;
            }
            // You can access the columns of the current row by calling the getXXX() method on the result set object, where XXX is the type of the column and the parameter is the name of the column.
            User user = new User(resultSet.getString("username"), resultSet.getString("password"), User.ROLE.valueOf(resultSet.getString("user_role")), (String[]) resultSet.getArray("security_answers").getArray(), resultSet.getString("email"), resultSet.getBoolean("enabled"));
            if (!user.isEnabled()) {
                System.out.println("User is not enabled.");
                return null;
            }

            if (!user.getSecurityAnswers()[Arrays.asList(securityQuestions).indexOf(securityQuestion)].equals(securityAnswer)) {
                System.out.println("Invalid security answer.");
                return null;
            }

            return loginSessionManager.createSession(user);

        } catch (SQLException e) {
            System.out.println("There was a problem with the database.");
            printDBError(e);
        }
        return null;
    }

    public boolean createUser(UUID userSession, User newUser) {
        // Protect routes using roles
        if (loginSessionManager.getUser(userSession).getRole() != User.ROLE.ADMIN) {
            System.out.println("User is not an admin.");
            return false;
        }
        try (Connection connection = dataSource.getConnection()) {
            // Check if user already exists
            ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM users WHERE username = '" + newUser.getUsername() + "'");
            if (resultSet.next()) {
                System.out.println("User already exists.");
                return false;
            }
            connection.createStatement().execute("INSERT INTO users VALUES ('%s', '%s', '%s', %s, %s, %s)".formatted(newUser.getUsername(), newUser.getPassword(), newUser.getRole().toString(), newUser.getSecurityAnswersAsString(), newUser.isEnabled(), newUser.getEmail()));
            return true;
        } catch (SQLException e) {
            System.out.println("There was a problem with the database.");
            printDBError(e);
            return false;
        }
    }

    //Student Information related functions


    // function to store student information, student has to be existing user
    public boolean saveStudentInfo(UUID userSession, Student student) {
        // check role
        if (loginSessionManager.getUser(userSession).getRole() != User.ROLE.ADMIN &&
                loginSessionManager.getUser(userSession).getRole() != User.ROLE.IT &&
                loginSessionManager.getUser(userSession).getRole() != User.ROLE.AUTH_STAFF) {
            System.out.println("No Authority to store student information.");
            return false;
        }

        try (Connection connection = dataSource.getConnection()) {
            // Check if student already exists
            ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM student_info WHERE user_id = '" + student.getUsername() + "'");

            if (resultSet.next()) {
                System.out.println("Student already exists.");
                return false;
            } else {
                connection.createStatement().execute(("INSERT INTO student_info VALUES ('%s', '%s', '%s', '%s', '%s', " +
                        "'%s', '%s', '%s', '%s' , '%s')").formatted(student.getUsername(),
                        student.getNetID(), student.getFirstName(), student.getLastName(), student.getEthnicity(), student.getGender(),
                        student.getMajor(), student.getSchoolYear(), student.getGpa(), student.getCitizenshipStatus()));
            }
            return true;
        } catch (SQLException e) {
            System.out.println("There was a problem with the database.");
            printDBError(e);
            return false;
        }
    }
    // update existing student information

    public void updateStudentInfo(UUID userSession, Student existingStudent, String newUsername,
                                  String newNetID, String newFirstName, String newLastName,
                                  String newEthnicity, String newGender, String newMajor,
                                  String newSchoolYear, double newGpa, boolean newCitizenshipStatus) {

        if (loginSessionManager.getUser(userSession) == null) {
            System.out.println("User is not logged in.");
        }

        // check role
        if (loginSessionManager.getUser(userSession).getRole() != User.ROLE.ADMIN &&
                loginSessionManager.getUser(userSession).getRole() != User.ROLE.IT &&
                loginSessionManager.getUser(userSession).getRole() != User.ROLE.AUTH_STAFF) {
            System.out.println("No authority to update student information.");
        }

        try (Connection connection = dataSource.getConnection()) {
            // Check if student already exists
            ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM student_info WHERE user_id = '" + existingStudent.getUsername() + "'");

            if (!resultSet.next()) {
                System.out.println("Student does not exist.");
            }
            String queryUpdateStudentInfo = "UPDATE student_info SET user_id = ?, net_id = ?, first_name = ?, last_name = ?, ethnicity = ?, gender = ?, major = ?, year = ?, gpa = ?, citizenship = ? WHERE user_id = ?";
            try (PreparedStatement updatedStatement = connection.prepareStatement(queryUpdateStudentInfo)) {
                updatedStatement.setString(1, newUsername != null ? newUsername : existingStudent.getUsername());
                updatedStatement.setString(2, newNetID != null ? newNetID : existingStudent.getNetID());
                updatedStatement.setString(3, newFirstName != null ? newFirstName : existingStudent.getFirstName());
                updatedStatement.setString(4, newLastName != null ? newLastName : existingStudent.getLastName());
                updatedStatement.setString(5, newEthnicity != null ? newEthnicity : existingStudent.getEthnicity());
                updatedStatement.setString(6, newGender != null ? newGender : existingStudent.getGender());
                updatedStatement.setString(7, newMajor != null ? newMajor : existingStudent.getMajor());
                updatedStatement.setString(8, newSchoolYear != null ? newSchoolYear : existingStudent.getSchoolYear());
                updatedStatement.setDouble(9, newGpa);
                updatedStatement.setBoolean(10, newCitizenshipStatus);
                updatedStatement.setString(11, existingStudent.getUsername());

                int rowsUpdated = updatedStatement.executeUpdate();
                if (rowsUpdated > 0) {
                    System.out.println("Student was updated successfully!");
                } else {
                    System.out.println("Failed to Update Student Information.");
                }
            }
        } catch (SQLException e) {
            System.out.println("There was a problem with the database.");
            printDBError(e);
        }
    }

    public Student getStudentInfo(UUID userSession, String username) {

        // Protect routes using roles
        if ((loginSessionManager.getUser(userSession).getRole() != User.ROLE.ADMIN) &&
                (loginSessionManager.getUser(userSession).getRole() != User.ROLE.IT) &&
                (loginSessionManager.getUser(userSession).getRole() != User.ROLE.AUTH_STAFF)) {
            System.out.println("User is not allowed to access this Students information.");
            return null;
        }

        try (Connection connection = dataSource.getConnection()) {
            ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM student_info WHERE user_id = '" + username + "'");
            if (!resultSet.next()) {
                System.out.println("Student does not exist.");
                return null;
            }
            // found the student so return it
            return new Student(username, resultSet.getString("net_id"), resultSet.getString("first_name"),
                    resultSet.getString("last_name"), resultSet.getString("ethnicity"), resultSet.getString("gender"),
                    resultSet.getString("major"), resultSet.getString("year"), resultSet.getDouble("gpa"), resultSet.getBoolean("citizenship"));
        } catch (SQLException e) {
            System.out.println("There was a problem with the database.");
            printDBError(e);
            return null;
        }
    }

    private static void printDBError(SQLException e) {
        System.err.println("\tMessage:   " + e.getMessage());
        System.err.println("\tSQLState:  " + e.getSQLState());
        System.err.println("\tErrorCode: " + e.getErrorCode());
    }
}
