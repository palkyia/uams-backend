
import java.util.UUID;

//student class for retrieving student information
public class Student {
    private String username;

    private String netID;

    private String firstName;

    private String lastName;
    private String ethnicity;
    private String gender;

    private String major;
    private String schoolYear;
    private double gpa;
    private boolean citizenshipStatus;


    //private boolean isEnabled;

    //constructors


    public Student(String username, String netID, String firstName, String lastName,
                   String ethnicity, String gender, String major,String schoolYear, double gpa, boolean citizenshipStatus){
        this.username = username;
        this.netID = netID;
        this.firstName = firstName;
        this.lastName = lastName;
        this.ethnicity = ethnicity;
        this.gender = gender;
        this.schoolYear = schoolYear;
        this.gpa = gpa;
        this.major = major;
        this.citizenshipStatus = citizenshipStatus;
    }

    //getters
    public String getUsername(){return username;}

    public String getNetID(){return netID;}

    public String getFirstName(){return firstName;}

    public String getLastName(){return lastName;}

    public String getEthnicity(){return ethnicity;}

    public String getGender(){return gender;}
    public String getMajor() {return major;}
    public String getSchoolYear() {return schoolYear;}

    public double getGpa() {return gpa;}

    public boolean getCitizenshipStatus() {return citizenshipStatus;}


}
