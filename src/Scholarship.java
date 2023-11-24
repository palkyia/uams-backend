import java.io.File;
import java.util.Date;
import java.util.UUID;

public class Scholarship {
    private UUID id;
    private String name;
    private String description;
    private String[] customInputFields;
    private Date deadline;
    private boolean isRequiredEmail;
    private boolean isRequirednetID;
    private boolean isRequiredGPA;
    private boolean isRequiredMajor;
    private boolean isRequiredYear;
    private boolean isRequiredGender;
    private boolean isRequiredEthnicity;
    private boolean isRequiredCitizenship;
    private boolean isRequiredName;
    private File uploadedFile;


    // constructor
    public Scholarship(UUID id, String name, String description, String[] customInputFields, Date deadline, boolean isRequiredEmail, boolean isRequirednetID, boolean isRequiredGPA, boolean isRequiredMajor, boolean isRequiredYear, boolean isRequiredGender, boolean isRequiredEthnicity, boolean isRequiredCitizenship, boolean isRequiredName, File uploadedFile) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.customInputFields = customInputFields;
        this.deadline = deadline;
        this.isRequiredEmail = isRequiredEmail;
        this.isRequirednetID = isRequirednetID;
        this.isRequiredGPA = isRequiredGPA;
        this.isRequiredMajor = isRequiredMajor;
        this.isRequiredYear = isRequiredYear;
        this.isRequiredGender = isRequiredGender;
        this.isRequiredEthnicity = isRequiredEthnicity;
        this.isRequiredCitizenship = isRequiredCitizenship;
        this.isRequiredName = isRequiredName;
        this.uploadedFile = uploadedFile;
    }

    // getters
    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String[] getCusttomInputFields() {
        return customInputFields;
    }

    public Date getDeadline() {
        return deadline;
    }

    public boolean isRequiredEmail() {
        return isRequiredEmail;
    }

    public boolean isRequirednetID() {
        return isRequirednetID;
    }

    public boolean isRequiredGPA() {
        return isRequiredGPA;
    }

    public boolean isRequiredMajor() {
        return isRequiredMajor;
    }

    public boolean isRequiredYear() {
        return isRequiredYear;
    }

    public boolean isRequiredGender() {
        return isRequiredGender;
    }

    public boolean isRequiredEthnicity() {
        return isRequiredEthnicity;
    }

    public boolean isRequiredCitizenship() {
        return isRequiredCitizenship;
    }

    public boolean isRequiredName() {
        return isRequiredName;
    }

    public File getUploadedFile() {
        return uploadedFile;
    }
}
