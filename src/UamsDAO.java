import org.postgresql.ds.PGSimpleDataSource;

import java.sql.*;
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
            // Check if scholarship exists
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

    public boolean CreateScholarship(UUID userSession, String name, String deadline, String description, String[] customIF, boolean Email, boolean NetID, boolean Name,
                                     boolean Ethnicity, boolean Gender, boolean SchoolYear, boolean GPA, boolean Major, boolean Citizenship) {

        if (loginSessionManager.getUser(userSession).getRole() != User.ROLE.ADMIN) {
            System.out.println("User is not an admin.");
            return false;
        }
        try (Connection connection = dataSource.getConnection()) {
            // Check if scholarship already exists
            ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM scholarship_forms WHERE name = '" + name + "'");
            if (resultSet.next()) {
                System.out.println("Scholarship already exists.");
                return false;
            }
            String Custom = "ARRAY[";
            for (int i = 0; i < customIF.length; i++) {
                Custom += "'" + customIF[i] + "'";
                if (i < customIF.length - 1) {
                    Custom += ", ";
                }
            }
            Custom += "]";
            connection.createStatement().execute("INSERT INTO scholarship_forms VALUES ('%s', '%s', %s, '%s', '%s', '%b', '%b', '%b', '%b', '%b', '%b', '%b', '%b', '%b')".formatted
                    (UUID.randomUUID(), name, Custom, deadline, description, Email, NetID, GPA, Major, SchoolYear, Gender, Citizenship, Name, Ethnicity));
            return true;
        } catch (SQLException e) {
            System.out.println("There was a problem with the database.");
            printDBError(e);
            return false;
        }
    }

    public ResultSet RetrieveScholarshipForm(UUID userSession, String gname, String gdeadline) {
        //Query the database Scholarship_forms looking for a scholarship that matches the given name and deadline

        if ((loginSessionManager.getUser(userSession).getRole() != User.ROLE.ADMIN) && (loginSessionManager.getUser(userSession).getRole() != User.ROLE.STUDENT)) {
            System.out.println("User is not an admin or a student.");
            return null;
        }
        try (Connection connection = dataSource.getConnection()) {
            // Check if user already exists
            ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM scholarship_forms WHERE name = '" + gname + "'");
            while (resultSet.next()) {
                String ScholarshipName = resultSet.getString("name");
                String ScholarshipDeadline = resultSet.getString("deadline");
                if (ScholarshipName.equals(gname) && ScholarshipDeadline.equals(gdeadline)) {
                    //System.out.println(resultSet.getString("name"));
                    return resultSet;
                }
            }
        } catch (SQLException e) {
            System.out.println("There was a problem with the database.");
            printDBError(e);
            return null;
        }
        return null;
    }

    public void UpdateScholarshipForm(UUID userSession, String name, String deadline, String updateWhat, String updateTo) {
        if (loginSessionManager.getUser(userSession).getRole() != User.ROLE.ADMIN) {
            System.out.println("User is not an admin.");
        }
        try (Connection connection = dataSource.getConnection()) {
            String updateQ = "UPDATE scholarship_forms SET " + updateWhat + " = ? WHERE name = ?";
            PreparedStatement prepare = connection.prepareStatement(updateQ);
            // Check if scholarship exists
            ResultSet resultSet = RetrieveScholarshipForm(userSession, name, deadline);
            if (resultSet.next()) {
                System.out.println("scholarship does not exist.");
            }
            if(updateTo.equals("true") || updateTo.equals("false")){
                boolean value = Boolean.parseBoolean(updateTo);
                prepare.setBoolean(1, value);
                prepare.setString(2, name);
            }
            else{
                prepare.setString(1, updateTo);
                prepare.setString(2, name);
            }
            prepare.executeUpdate();
        } catch (SQLException e) {
            System.out.println("There was a problem with the database.");
            printDBError(e);
        }
    }

    public void RemoveScholarshipsYearly(UUID userSession, String year){
        if (loginSessionManager.getUser(userSession).getRole() != User.ROLE.ADMIN) {
            System.out.println("User is not an admin.");
        }
        try (Connection connection = dataSource.getConnection()) {
            // Check if scholarship already exists
            ResultSet resultSet = connection.createStatement().executeQuery("DELETE FROM scholarship_forms WHERE YEAR(deadline) = '" + year + "'");
            if (resultSet.next()) {
                System.out.println("Scholarships do not exist.");
            }
        }
        catch (SQLException e) {
            System.out.println("There was a problem with the database.");
            printDBError(e);

        }
    }

    private static void printDBError(SQLException e) {
        System.err.println("\tMessage:   " + e.getMessage());
        System.err.println("\tSQLState:  " + e.getSQLState());
        System.err.println("\tErrorCode: " + e.getErrorCode());
    }

}
