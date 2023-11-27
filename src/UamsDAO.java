import org.postgresql.ds.PGSimpleDataSource;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class UamsDAO {

    private static final String DB_URL = "jdbc:postgresql://potent-markhor-10752.6wr.cockroachlabs.cloud:26257/defaultdb?sslmode=require";
    private static final String DB_USER = "jacob";
    private static final String DB_PASSWORD = "w0IvQemMCK9jmWXA8efx-Q";
    private static final String fromEmail = "team12sfwe301@gmail.com";
    private static final String EMAIL_PASS = "hwou gioz mcnm wios";

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
            connection.createStatement().execute("INSERT INTO users VALUES ('%s', '%s', '%s', %s, %s, %s)".formatted(newUser.getUsername(), newUser.getPassword(), newUser.getRole().toString(), newUser.getSecurityAnswersAsString(), newUser.isEnabled(), newUser.getEmail()));
            return true;
        } catch (SQLException e) {
            System.out.println("There was a problem with the database.");
            printDBError(e);
            return false;
        }
    }


    public boolean CreateScholarship(UUID userSession, Scholarship newScholarship) {
        if ((loginSessionManager.getUser(userSession).getRole() != User.ROLE.ADMIN) && (loginSessionManager.getUser(userSession).getRole() != User.ROLE.PROVIDER)) {
            System.out.println("User is not an admin or a provider.");
            return false;
        }
        try (Connection connection = dataSource.getConnection()) {
            // Check if scholarship already exists
            ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM scholarship_forms WHERE id = '" + newScholarship.getId().toString() + "'");
            if (resultSet.next()) {
                System.out.println("Scholarship already exists.");
                return false;
            }
            String Custom = "ARRAY[";
            for (int i = 0; i < newScholarship.getCusttomInputFields().length; i++) {
                Custom += "'" + newScholarship.getCusttomInputFields()[i] + "'";
                if (i < newScholarship.getCusttomInputFields().length - 1) {
                    Custom += ", ";
                }
            }
            Custom += "]";
            // insert new scholarship from newScholarship fields
            connection.createStatement().execute("INSERT INTO scholarship_forms VALUES ('%s', '%s', %s, '%s', '%s', '%b', '%b', '%b', '%b', '%b', '%b', '%b', '%b', '%b', '%s')".formatted
                    (newScholarship.getId().toString(), newScholarship.getName(), Custom, newScholarship.getDeadline().toString(), newScholarship.getDescription(), newScholarship.isRequiredEmail(), newScholarship.isRequirednetID(), newScholarship.isRequiredGPA(), newScholarship.isRequiredMajor(), newScholarship.isRequiredYear(), newScholarship.isRequiredGender(), newScholarship.isRequiredEthnicity(), newScholarship.isRequiredCitizenship(), newScholarship.isRequiredName(), newScholarship.getUploadedFile().getPath()));
            return true;
        } catch (SQLException e) {
            System.out.println("There was a problem with the database.");
            printDBError(e);
            return false;
        }
    }


    public ArrayList<Scholarship> RetrieveScholarshipByID(UUID userSession, UUID id) {
        //Query the database Scholarship_forms looking for a scholarship that matches the given name and deadline

        if ((loginSessionManager.getUser(userSession).getRole() != User.ROLE.ADMIN) && (loginSessionManager.getUser(userSession).getRole() != User.ROLE.STUDENT) && (loginSessionManager.getUser(userSession).getRole() != User.ROLE.PROVIDER)) {
            System.out.println("User is not an admin, student or a provider.");
            return null;
        }
        try (Connection connection = dataSource.getConnection()) {
            ArrayList<Scholarship> scholarships = new ArrayList<>();

            // Check if user already exists
            ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM scholarship_forms WHERE id = '" + id + "'");
            if (resultSet.next()) {
                scholarships.add(new Scholarship(UUID.fromString(resultSet.getString("id")), resultSet.getString("name"), resultSet.getString("description"), (String[]) resultSet.getArray("custom_input_fields").getArray(), resultSet.getDate("deadline"), resultSet.getBoolean("is_required_email"), resultSet.getBoolean("is_required_netid"), resultSet.getBoolean("is_required_gpa"), resultSet.getBoolean("is_required_major"), resultSet.getBoolean("is_required_year"), resultSet.getBoolean("is_required_gender"), resultSet.getBoolean("is_required_ethnicity"), resultSet.getBoolean("is_required_citizenship"), resultSet.getBoolean("is_required_name"), new File(resultSet.getString("uploaded_file_path"))));
            } else {
                return null;
            }
            return scholarships;
        } catch (SQLException e) {
            System.out.println("There was a problem with the database.");
            printDBError(e);
            return null;
        }
    }

    public ArrayList<Scholarship> RetrieveScholarshipsByName(UUID userSession, String name) {
        try (Connection connection = dataSource.getConnection()) {
            ArrayList<Scholarship> scholarships = new ArrayList<>();
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM scholarship_forms WHERE name ILIKE ?");
            stmt.setString(1, "%" + name + "%");
            ResultSet resultSet = stmt.executeQuery();
            while (resultSet.next()) {
                // add scholarship to list
                scholarships.add(new Scholarship(UUID.fromString(resultSet.getString("id")), resultSet.getString("name"), resultSet.getString("description"), (String[]) resultSet.getArray("custom_input_fields").getArray(), resultSet.getDate("deadline"), resultSet.getBoolean("is_required_email"), resultSet.getBoolean("is_required_netid"), resultSet.getBoolean("is_required_gpa"), resultSet.getBoolean("is_required_major"), resultSet.getBoolean("is_required_year"), resultSet.getBoolean("is_required_gender"), resultSet.getBoolean("is_required_ethnicity"), resultSet.getBoolean("is_required_citizenship"), resultSet.getBoolean("is_required_name"), new File(resultSet.getString("uploaded_file_path"))));
            }
            return scholarships;
        } catch (SQLException e) {
            System.out.println("There was a problem with the database.");
            printDBError(e);
            return null;
        }

    }

    public ArrayList<Scholarship> RetrieveScholarshipsByKeywords(UUID userSession, ArrayList<String> keywords) {
        try (Connection connection = dataSource.getConnection()) {
            ArrayList<Scholarship> scholarships = new ArrayList<>();
            StringBuilder query = new StringBuilder("SELECT * FROM scholarship_forms WHERE description ");
            for (int i = 0; i < keywords.size(); i++) {
                if (i == 0) {
                    query.append("ILIKE ?");
                } else {
                    query.append("OR description ILIKE ?");
                }
            }
            PreparedStatement stmt = connection.prepareStatement(query.toString());
            for (int i = 0; i < keywords.size(); i++) {
                stmt.setString(i + 1, "%" + keywords.get(i) + "%");
            }
            ResultSet resultSet = stmt.executeQuery();
            while (resultSet.next()) {
                scholarships.add(new Scholarship(UUID.fromString(resultSet.getString("id")), resultSet.getString("name"), resultSet.getString("description"), (String[]) resultSet.getArray("custom_input_fields").getArray(), resultSet.getDate("deadline"), resultSet.getBoolean("is_required_email"), resultSet.getBoolean("is_required_netid"), resultSet.getBoolean("is_required_gpa"), resultSet.getBoolean("is_required_major"), resultSet.getBoolean("is_required_year"), resultSet.getBoolean("is_required_gender"), resultSet.getBoolean("is_required_ethnicity"), resultSet.getBoolean("is_required_citizenship"), resultSet.getBoolean("is_required_name"), new File(resultSet.getString("uploaded_file_path"))));
            }
            return scholarships;
        } catch (SQLException e) {
            System.out.println("There was a problem with the database.");
            printDBError(e);
            return null;
        }

    }

    public void UpdateScholarshipForm(UUID userSession, UUID id, String updateWhat, String updateTo) {
        if ((loginSessionManager.getUser(userSession).getRole() != User.ROLE.ADMIN) && (loginSessionManager.getUser(userSession).getRole() != User.ROLE.PROVIDER)) {
            System.out.println("User is not an admin or a provider.");
        }
        try (Connection connection = dataSource.getConnection()) {
            String updateQ = "UPDATE scholarship_forms SET " + updateWhat + " = ? WHERE id = ?";
            PreparedStatement prepare = connection.prepareStatement(updateQ);
            prepare.setString(2, id.toString());
            // Check if scholarship exists
            ArrayList<Scholarship> oldScholarship = RetrieveScholarshipByID(userSession, id);
            if (oldScholarship == null) {
                System.out.println("scholarship does not exist.");
            }
            if (updateTo.equals("true") || updateTo.equals("false")) {
                boolean value = Boolean.parseBoolean(updateTo);
                prepare.setBoolean(1, value);
            } else {
                prepare.setString(1, updateTo);
            }
            prepare.executeUpdate();
        } catch (SQLException e) {
            System.out.println("There was a problem with the database.");
            printDBError(e);
        }
    }

    public void RemoveScholarshipsYearly(UUID userSession, int year) {
        if (loginSessionManager.getUser(userSession).getRole() != User.ROLE.ADMIN) {
            System.out.println("User is not an admin.");
        }
        try (Connection connection = dataSource.getConnection()) {
            // Check if scholarship already exists

            ResultSet resultSet = connection.createStatement().executeQuery("DELETE FROM scholarship_forms where EXTRACT(YEAR FROM deadline) =  '" + year + "'");

        } catch (SQLException e) {
            System.out.println("There was a problem with the database.");
            printDBError(e);

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


    public User getUserInfo(UUID userSession, String username) {
        if ((loginSessionManager.getUser(userSession).getRole() != User.ROLE.ADMIN) &&
                (loginSessionManager.getUser(userSession).getRole() != User.ROLE.IT) &&
                (loginSessionManager.getUser(userSession).getRole() != User.ROLE.AUTH_STAFF)) {
            System.out.println("User is not allowed to access this user information.");
            return null;
        }

        try (Connection connection = dataSource.getConnection()) {
            ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM users WHERE username = '" + username + "'");
            if (!resultSet.next()) {
                System.out.println("User does not exist.");
                return null;
            }
            return new User(resultSet.getString("username"), resultSet.getString("password"), User.ROLE.valueOf(resultSet.getString("user_role")), (String[]) resultSet.getArray("security_answers").getArray(), resultSet.getString("email"), resultSet.getBoolean("enabled"));
        } catch (SQLException e) {
            System.out.println("There was a problem with the database.");
            printDBError(e);
            return null;
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
                String uploadServerLocation = uploadFileToServer(application.getUploadedFile());
                connection.createStatement().execute("UPDATE applications SET custom_responses = %s, uploaded_file_path = '%s' WHERE student_username = '%s' AND scholarship_id = '%s'".formatted(arrayToSQLString(application.getResponses()), uploadServerLocation, application.getUsername(), application.getScholarshipID().toString()));
            } else {
                // no existing, so insert new
                String uploadServerLocation = uploadFileToServer(application.getUploadedFile());
                connection.createStatement().execute("INSERT INTO applications (scholarship_id, student_username, custom_responses, uploaded_file_path) VALUES ('%s', '%s', %s, '%s')".formatted(application.getScholarshipID().toString(), application.getUsername(), arrayToSQLString(application.getResponses()), uploadServerLocation));
                notifyReviewersOfApplication(application.getScholarshipID().toString());
                notifyProvidersOfApplication(application.getScholarshipID().toString());
            }
            checkAndNotifyDeadlines();
            return true;
        } catch (SQLException e) {
            System.out.println("There was a problem with the database.");
            printDBError(e);
            return false;
        }

    }

    public Application getApplication(UUID userSession, String username, UUID scholarshipID) {
        checkAndNotifyDeadlines();
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
            File appilcation_file = new File(resultSet.getString("uploaded_file_path"));
            return new Application(username, scholarshipID, (String[]) (resultSet.getArray("custom_responses")).getArray(), appilcation_file, resultSet.getBoolean("has_notified"));
        } catch (SQLException e) {
            System.out.println("There was a problem with the database.");
            printDBError(e);
            return null;
        }
    }

    /**
     * Saves a file into the UASAMS server
     *
     * @param inputFile
     * @return String (location file was saved in server), else null (error saving)
     */
    public static String uploadFileToServer(File inputFile) {
        String uploadFolderName = "file_uploads";
        String serverDirectory = System.getProperty("user.dir");
        File uploadsFolder = new File(serverDirectory, uploadFolderName);

        // Create the subfolder if it does not exist
        if (!uploadsFolder.exists()) {
            boolean folderCreated = uploadsFolder.mkdir();
            if (!folderCreated) {
                System.out.println("Failed to create folder for uploads.");
                return null;
            }
        }

        // Create a Path for the destination file in 'file_uploads' folder
        Path destinationPath = uploadsFolder.toPath().resolve(inputFile.getName());

        try {
            // copy file to server
            Files.copy(inputFile.toPath(), destinationPath, StandardCopyOption.REPLACE_EXISTING);
            return destinationPath.toString();
        } catch (IOException e) {
            System.out.println("Failed to copy the file.");
            return null;
        }
    }


    // Authority: IT, ADMIN, AUTH_STAFF
    public boolean modifyUser(UUID userSession, User existingUser, String newUsername, String newPassword, String[] newSecurityAnswers, User.ROLE newUserRole) {
        // check valid user session
        if (loginSessionManager.getUser(userSession) == null) {
            System.out.println("Invalid user session.");
            return false;
        }

        // check user role
        if (loginSessionManager.getUser(userSession).getRole() != User.ROLE.ADMIN && loginSessionManager.getUser(userSession).getRole() != User.ROLE.IT && loginSessionManager.getUser(userSession).getRole() != User.ROLE.AUTH_STAFF) {
            System.out.println("No authority to modify the user.");
            return false;
        }

        try (Connection connection = dataSource.getConnection()) {

            ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM users WHERE username = '" + existingUser.getUsername() + "'");
            if (!resultSet.next()) {
                System.out.println("User does not exist.");
                return false;
            }

            ResultSet resultSetStudentInfo = connection.createStatement().executeQuery("SELECT * FROM student_info WHERE user_id = '" + existingUser.getUsername() + "'");
            if (!resultSetStudentInfo.next()) {
                System.out.println("User does not exist (student_info).");
                return false;
            }

            String updateQueryStudentInfo = "UPDATE student_info SET user_id = ? WHERE user_id = ?";
            try (PreparedStatement updateStatementStudentInfo = connection.prepareStatement(updateQueryStudentInfo)) {
                updateStatementStudentInfo.setString(1, newUsername);
                updateStatementStudentInfo.setString(2, existingUser.getUsername());

                int rowsAffectedStudentInfo = updateStatementStudentInfo.executeUpdate();
                if (rowsAffectedStudentInfo > 0) {
                    System.out.println("User information updated successfully (student_info).");
                } else {
                    System.out.println("Failed to update user information (student_info).");
                }
            }

            String updateQuery = "UPDATE users SET username = ?, password = ?, user_role = ?, security_answers = ? WHERE username = ?";
            try (PreparedStatement updateStatement = connection.prepareStatement(updateQuery)) {
                updateStatement.setString(1, newUsername != null ? newUsername : existingUser.getUsername());
                updateStatement.setString(2, newPassword != null ? newPassword : existingUser.getPassword());
                updateStatement.setString(3, newUserRole.toString() != null ? newUserRole.toString() : String.valueOf(existingUser.getRole()));
                updateStatement.setArray(4, newSecurityAnswers != null ? connection.createArrayOf("text", newSecurityAnswers) : connection.createArrayOf("text", existingUser.getSecurityAnswers()));
                updateStatement.setString(5, existingUser.getUsername());

                int rowsAffected = updateStatement.executeUpdate();
                if (rowsAffected > 0) {
                    System.out.println("User information updated successfully (users).");
                    return true;
                } else {
                    System.out.println("Failed to update user information (users).");
                    return false;
                }
            }

        } catch (SQLException e) {
            System.out.println("There was a problem with the database (modifyUser).");
            printDBError(e);
            return false;
        }
    }


    public boolean checkAndNotifyDeadlines() {
        try (Connection connection = dataSource.getConnection()) {
            HashSet<String> scholarshipIDs = new HashSet<>();
            PreparedStatement studentsStmt = connection.prepareStatement(" SELECT email, name, deadline, username, id, has_notified FROM scholarship_forms JOIN applications ON scholarship_forms.id = applications.scholarship_id JOIN users ON student_username = username WHERE deadline <= NOW() + INTERVAL '1 day' AND deadline >= NOW()");
            ResultSet resultSet = studentsStmt.executeQuery();
            while (resultSet.next()) {
                if (resultSet.getBoolean("has_notified")) {
                    continue;
                }
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
            PreparedStatement updateNotifiedStmt = connection.prepareStatement("UPDATE applications SET has_notified = true WHERE scholarship_id = ?");
            for (String scholarshipID : scholarshipIDs) {
                updateNotifiedStmt.setString(1, scholarshipID);
                updateNotifiedStmt.executeUpdate();
            }
            return true;
        } catch (SQLException e) {
            System.out.println("There was a problem with the database.");
            printDBError(e);
            return false;
        }
    }

    public boolean notifyProvidersOfApplication(String scholarshipId) {
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
