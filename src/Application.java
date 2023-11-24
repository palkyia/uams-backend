
import java.io.File;
import java.util.UUID;

/**
 * To keep info on a User's application to a specific ScholarshipForm.
 */
public class Application {

    private String username;
    private UUID scholarshipID;
    private String[] responses; // user responses to ScholarshipForm's fields
    private File uploadedFile; // path to file inside UASAMS server
    private boolean hasNotified; // whether the user has been notified of the application's status


    /*
    Constructors
    */
    public Application(String username, UUID scholarshipID, String[] responses, File uploadedFile, boolean hasNotified) {
        this.username = username;
        this.scholarshipID = scholarshipID;
        this.responses = responses;
        this.uploadedFile = uploadedFile;
        this.hasNotified = hasNotified;
    }


    /*
    Getter functions (attributes)
    */
    public String getUsername() {
        return username;
    }

    public UUID getScholarshipID() {
        return scholarshipID;
    }

    public String[] getResponses() {
        return responses;
    }

    public File getUploadedFile() {
        return uploadedFile;
    }

    public String getUploadedFilePath() {
        return uploadedFile.getPath();
    }

    public boolean getHasNotified() {
        return hasNotified;
    }


}
