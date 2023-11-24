public class User {
    public static enum ROLE {
        ADMIN, STUDENT, REVIEWER, PROVIDER, IT, FUND_STEWARD, AUTH_STAFF
    }

    private String username;
    private String password;
    private ROLE role;
    private String[] securityAnswers;
    private String email;
    private boolean isEnabled;

    public User(String username, String password, ROLE role, String[] securityAnswers, String email, boolean isEnabled) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.securityAnswers = securityAnswers;
        this.email = email;
        this.isEnabled = isEnabled;
    }

    public User(String username, String password, ROLE role, String[] securityAnswers) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.securityAnswers = securityAnswers;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public ROLE getRole() {
        return role;
    }

    public String[] getSecurityAnswers() {
        return securityAnswers;
    }

    public String getSecurityAnswersAsString() {
        String securityAnswersAsString = "ARRAY[";
        for (int i = 0; i < securityAnswers.length; i++) {
            securityAnswersAsString += "'" + securityAnswers[i] + "'";
            if (i < securityAnswers.length - 1) {
                securityAnswersAsString += ", ";
            }
        }
        securityAnswersAsString += "]";
        return securityAnswersAsString;
    }

    public String getEmail() {
        return email;
    }

    public boolean isEnabled() {
        return isEnabled;
    }


}
