import org.postgresql.ds.PGSimpleDataSource;


import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.sql.*;
import java.util.*;
import java.util.Date;

public class UamsDAO {

    private static final String DB_URL = "jdbc:postgresql://potent-markhor-10752.6wr.cockroachlabs.cloud:26257/defaultdb?sslmode=require";
    private static final String DB_USER = "jacob";
    private static final String DB_PASSWORD = "w0IvQemMCK9jmWXA8efx-Q";
    private final String fromEmail = "team12sfwe301@gmail.com";
    private final String EMAIL_PASS = "hwou gioz mcnm wios";

    private static final String[] securityQuestions = {
            "What is your mother's maiden name?",
            "What is the name of your first pet?",
            "What is the name of the high school you graduated from?",
    };
    private PGSimpleDataSource dataSource;
    private LoginSessionManager loginSessionManager;
    private Session emailSession;

    public UamsDAO() {
        dataSource = new PGSimpleDataSource();
        dataSource.setURL(DB_URL);
        dataSource.setUser(DB_USER);
        dataSource.setPassword(DB_PASSWORD);
        loginSessionManager = new LoginSessionManager();

        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com"); //SMTP Host
        props.put("mail.smtp.port", "587"); //TLS Port
        props.put("mail.smtp.auth", "true"); //enable authentication
        props.put("mail.smtp.starttls.enable", "true"); //enable STARTTLS

        //create Authenticator object to pass in Session.getInstance argument
        Authenticator auth = new Authenticator() {
            //override the getPasswordAuthentication method
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(fromEmail, EMAIL_PASS);
            }
        };
        emailSession = Session.getInstance(props, auth);

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
            connection.createStatement().execute("INSERT INTO users VALUES ('%s', '%s', '%s', %s, %s)".formatted(newUser.getUsername(), newUser.getPassword(), newUser.getRole().toString(), newUser.getSecurityAnswersAsString(), newUser.isEnabled()));
            return true;
        } catch (SQLException e) {
            System.out.println("There was a problem with the database.");
            printDBError(e);
            return false;
        }
    }

    private static void printDBError(SQLException e) {
        System.err.println("\tMessage:   " + e.getMessage());
        System.err.println("\tSQLState:  " + e.getSQLState());
        System.err.println("\tErrorCode: " + e.getErrorCode());
    }

    public boolean checkAndNotifyDeadlines() {
        try (Connection connection = dataSource.getConnection()) {
            HashSet<String> scholarshipIDs = new HashSet<>();
            PreparedStatement studentsStmt = connection.prepareStatement(" SELECT email, name, deadline, username, id FROM scholarship_forms JOIN applications ON scholarship_forms.id = applications.scholarship_id JOIN users ON student_username = username WHERE deadline <= NOW() + INTERVAL '1 day' AND deadline >= NOW()");
            ResultSet resultSet = studentsStmt.executeQuery();
            while (resultSet.next()) {
                String email = resultSet.getString("email");
                String scholarshipName = resultSet.getString("name");
                Date deadline = resultSet.getDate("deadline");
                String username = resultSet.getString("username");
                String subject = "Scholarship Deadline Reminder";
                String body = "Hello %s,\n\nThis is a reminder that the deadline for the scholarship you applied for: '%s' is due on %s.\n\nSincerely,\nUArizona Scholarship Application Management System".formatted(username, scholarshipName, deadline);
                sendEmail(email, subject, body);
                scholarshipIDs.add(resultSet.getString("id"));
            }
            PreparedStatement reviewersStmt = connection.prepareStatement("SELECT username, name, email, deadline from reviewers JOIN scholarship_forms ON reviewers.assigned_scholarship = scholarship_forms.id JOIN public.users u on u.username = reviewers.reviewer_username WHERE scholarship_forms.id = ?");
            for (String scholarshipID : scholarshipIDs) {
                reviewersStmt.setString(1, scholarshipID);
                resultSet = reviewersStmt.executeQuery();
                while (resultSet.next()) {
                    String email = resultSet.getString("email");
                    String username = resultSet.getString("username");
                    String scholarshipName = resultSet.getString("name");
                    String subject = "Scholarship Review Deadline Reminder";
                    Date deadline = resultSet.getDate("deadline");
                    String body = "Hello %s,\n\nThis is a reminder that the deadline for the scholarship you are assigned to review: '%s' is due on %s.\n\nSincerely,\nUArizona Scholarship Application Management System".formatted(username, scholarshipName, deadline);
                    sendEmail(email, subject, body);
                }
            }
            return true;
        } catch (SQLException e) {
            System.out.println("There was a problem with the database.");
            printDBError(e);
            return false;
        }
    }

    public boolean notifyDonorsOfApplication(String scholarshipId) {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement donorStmt = connection.prepareStatement("SELECT email, name, username FROM scholarship_forms JOIN donors ON scholarship_forms.id = donors.donor_scholarship JOIN public.users u on donors.donor_username = u.username WHERE scholarship_forms.id = ?");
            donorStmt.setString(1, scholarshipId);
            ResultSet resultSet = donorStmt.executeQuery();
            while (resultSet.next()) {
                String email = resultSet.getString("email");
                String username = resultSet.getString("username");
                String name = resultSet.getString("name");
                String subject = "Scholarship Application Submitted";
                String body = "Hello %s,\n\nThis is a notification that a student has submitted an application for your scholarship: '%s'.\n\nSincerely,\nUArizona Scholarship Application Management System".formatted(username, name);
                sendEmail(email, subject, body);
            }
            return true;
        } catch (SQLException e) {
            System.out.println("There was a problem with the database.");
            printDBError(e);
            return false;
        }
    }

    public boolean notifyReviewersOfApplication(String scholarshipId) {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement reviewerStmt = connection.prepareStatement("SELECT email, name, username FROM scholarship_forms JOIN reviewers ON scholarship_forms.id = reviewers.assigned_scholarship JOIN public.users u on reviewers.reviewer_username = u.username WHERE scholarship_forms.id = ?");
            reviewerStmt.setString(1, scholarshipId);
            ResultSet resultSet = reviewerStmt.executeQuery();
            while (resultSet.next()) {
                String email = resultSet.getString("email");
                String username = resultSet.getString("username");
                String name = resultSet.getString("name");
                String subject = "Scholarship Application Submitted";
                String body = "Hello %s,\n\nThis is a notification that a student has submitted an application for a scholarship you are assigned to review: '%s'.\n\nSincerely,\nUArizona Scholarship Application Management System".formatted(username, name);
                sendEmail(email, subject, body);
            }
            return true;
        } catch (SQLException e) {
            System.out.println("There was a problem with the database.");
            printDBError(e);
            return false;
        }
    }

    private void sendEmail(String toEmail, String subject, String body) {
        try {
            MimeMessage msg = new MimeMessage(emailSession);
            //set message headers
            msg.addHeader("Content-type", "text/HTML; charset=UTF-8");
            msg.addHeader("format", "flowed");
            msg.addHeader("Content-Transfer-Encoding", "8bit");

            msg.setFrom(new InternetAddress("team12sfwe301@gmail.com", "UArizona Scholarship Application Management System"));

            msg.setReplyTo(InternetAddress.parse("team12sfwe301@gmail.com", false));

            msg.setSubject(subject, "UTF-8");

            msg.setText(body, "UTF-8");

            msg.setSentDate(new Date());

            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail, false));
            Transport.send(msg);

            System.out.printf("Email sent successfully to %s.%n", toEmail);
        } catch (Exception e) {
            System.out.println("There was a problem sending the email.");
            e.printStackTrace();
        }
    }

}
