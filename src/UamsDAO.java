import org.postgresql.ds.PGSimpleDataSource;

import java.sql.Array;
import java.sql.Connection;
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


    /*
    Constructors
     */
    public UamsDAO() {
        dataSource = new PGSimpleDataSource();
        dataSource.setURL(DB_URL);
        dataSource.setUser(DB_USER);
        dataSource.setPassword(DB_PASSWORD);
        loginSessionManager = new LoginSessionManager();
    }


    /*
    Login Functions
     */
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
            User user = new User(resultSet.getString("username"), resultSet.getString("password"), User.ROLE.valueOf(resultSet.getString("user_role")), (String[]) resultSet.getArray("security_answers").getArray(), resultSet.getBoolean("enabled"));
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


    /*
    User-related Functions
     */
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
            connection.createStatement().execute("INSERT INTO users VALUES ('%s', '%s', '%s', %s, %s)".formatted(newUser.getUsername(), newUser.getPassword(), newUser.getRole().toString(), newUser.getSecurityAnswersAsString(), newUser.isEnabled()));
            return true;
        } catch (SQLException e) {
            System.out.println("There was a problem with the database.");
            printDBError(e);
            return false;
        }
    }


    /*
    Application-related Functions
     */
    public boolean saveApplication(UUID userSession, Application application) {

        String loggedUsername = loginSessionManager.getUser(userSession).getUsername();
        String applicantUsername = application.getUsername();

        // Protect by authenticating application ownership by user
        if (!loggedUsername.equals(applicantUsername)) {
            System.out.println("User is not owner of Application.");
            return false;
        }
        try (Connection connection = dataSource.getConnection()) {

            // Check if application already exists
            ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM applications WHERE student_username = '" + application.getUsername() + "' AND scholarship_id = '" + application.getScholarshipID().toString() + "'");

            if (resultSet.next()) {
                // overwrite existing with new
                connection.createStatement().execute("UPDATE applications SET custom_responses = %s, uploaded_file_path = '%s' WHERE student_username = '%s' AND scholarship_id = '%s'".formatted(arrayToSQLString(application.getResponses()), application.getUploadedFilePath(), application.getUsername(), application.getScholarshipID().toString()));
            } else {
                // no existing, so insert new
                connection.createStatement().execute("INSERT INTO applications (scholarship_id, student_username, custom_responses, uploaded_file_path) VALUES ('%s', '%s', %s, '%s')".formatted(application.getScholarshipID().toString(), application.getUsername(), arrayToSQLString(application.getResponses()), application.getUploadedFilePath()));
            }
            return true;
        } catch (SQLException e) {
            System.out.println("There was a problem with the database.");
            printDBError(e);
            return false;
        }

    }

    public Application getApplication(UUID userSession, String username, UUID scholarshipID) {

        // Protect routes using roles
        if ((loginSessionManager.getUser(userSession).getRole() != User.ROLE.ADMIN)
                || !loginSessionManager.getUser(userSession).getUsername().equals(username)) {
            System.out.println("User is not allowed to access this application.");
            return null;
        }
        try (Connection connection = dataSource.getConnection()) {
            // retrieve application
            ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM applications WHERE student_username = '" + username + "' AND scholarship_id = '" + scholarshipID.toString() + "'");
            if (!resultSet.next()) {
                System.out.println("Application not found.");
                return null;
            }
            // found, so return it
            return new Application(username, scholarshipID, (String[]) (resultSet.getArray("custom_responses")).getArray(), resultSet.getString("uploaded_file_path"));
        } catch (SQLException e) {
            System.out.println("There was a problem with the database.");
            printDBError(e);
            return null;
        }
    }


    /*
    Utility Functions
     */
    private static void printDBError(SQLException e) {
        System.err.println("\tMessage:   " + e.getMessage());
        System.err.println("\tSQLState:  " + e.getSQLState());
        System.err.println("\tErrorCode: " + e.getErrorCode());
    }

    private static String arrayToSQLString(String[] array) {
        if (array == null || array.length == 0) {
            return "ARRAY[]";
        }

        StringBuilder sb = new StringBuilder("ARRAY[");
        for (int i = 0; i < array.length; i++) {
            sb.append("'").append(array[i]).append("'");
            if (i < array.length - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");

        return sb.toString();
    }

}
