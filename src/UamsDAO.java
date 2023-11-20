import org.postgresql.ds.PGSimpleDataSource;

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


    public boolean updateUserInfo(UUID userSession, User existingUser, String newUsername, String newPassword, String[] newSecurityAnswers, User.ROLE newUserRole) {
        // check valid user session
        if (loginSessionManager.getUser(userSession) == null) {
            System.out.println("Invalid user session");
            return false;
        }

        try (Connection connection = dataSource.getConnection()) {

            ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM users WHERE username = '" + existingUser.getUsername() + "'");
            if (!resultSet.next()) {
                System.out.println("User does not exist.");
                return false;
            }

            String updateQuery = "UPDATE users SET username = ?, password = ?, user_role = ?, security_answers = ? WHERE username = ?";
            try (PreparedStatement updateStatement = connection.prepareStatement(updateQuery)) {
                if (newUsername != null) {
                    updateStatement.setString(1, newUsername);
                }
                if (newPassword != null) {
                    updateStatement.setString(2, newPassword);
                }
                if (newUserRole != null) {
                    updateStatement.setString(3, newUserRole.toString());
                }
                if (newSecurityAnswers != null) {
                    updateStatement.setArray(4, connection.createArrayOf("text", newSecurityAnswers));
                }

                updateStatement.setString(5, existingUser.getUsername());

                int rowsAffected = updateStatement.executeUpdate();

                if (rowsAffected > 0) {
                    System.out.println("User information updated successfully.");
                    return true;
                } else {
                    System.out.println("Failed to update user information.");
                    return false;
                }
            }

        } catch (SQLException e) {
            System.out.println("There was a problem with the database (updateUserInfo).");
            printDBError(e);
            return false;
        }
    }

    private static void printDBError(SQLException e) {
        System.err.println("\tMessage:   " + e.getMessage());
        System.err.println("\tSQLState:  " + e.getSQLState());
        System.err.println("\tErrorCode: " + e.getErrorCode());
    }

}
