
import java.util.UUID;

/**
 * To keep info on a User's application to a specific ScholarshipForm.
 */
public class Application {

    private String username;
    private UUID scholarshipID;
    private String[] responses; // user responses to ScholarshipForm's fields
    private String uploadedFilePath; // path to file inside UASAMS server


    /*
    Constructors
    */
    public Application(String username, UUID scholarshipID, String[] responses, String uploadedFilePath) {
        this.username = username;
        this.scholarshipID = scholarshipID;
        this.responses = responses;
        this.uploadedFilePath = uploadedFilePath;
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

    public String getUploadedFilePath() {
        return uploadedFilePath;
    }


}
