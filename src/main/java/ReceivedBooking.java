import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ReceivedBooking {
    private long userid;
    private long requestid;
    private long dentistid;
    private long issuance;
    private String time;

    public ReceivedBooking(long userid, long requestid, long dentistid, long issuance, String time) {
        setUserid(userid);
        setRequestid(requestid);
        setDentistid(dentistid);
        setIssuance(issuance);
        setTime(time);
    }

    public ReceivedBooking(long userid, long requestid, String time) {
    	setUserid(userid);
        setRequestid(requestid);
        setTime(time);
    }
    
    public long getRequestid() {
        return requestid;
    }

    public void setRequestid(long requestid) {
    	String requestValidation = String.valueOf(requestid); // Converts the long requestid to a String to be used for validation
        if (requestValidation.matches("[0-9]{1,5}")) { // This allows there to be up to 99999 requests
            this.requestid = requestid;
        } else {
            throw new IllegalArgumentException("Request id has to be between one and five digits long");
        }
    }

    public long getDentistid() {
        return dentistid;
    }

    public void setDentistid(long dentistid) {
    	String dentistValidation = String.valueOf(dentistid); // Converts the long dentistid to a String to be used for validation
        if (dentistValidation.matches("[0-9]{1,4}")) {
            this.dentistid = dentistid;
        } else {
            throw new IllegalArgumentException("Dentist id has to be between one and four digits long");
        }
    }

    public long getIssuance() {
        return issuance;
    }

    public void setIssuance(long issuance) {
    	String issuanceValidation = String.valueOf(issuance); // Converts the long issuance to a String to be used for validation, must add L after the issuance when testing for it to work
        if (issuanceValidation.matches("[0-9]{13}")) {
            this.issuance = issuance;
        } else {
            throw new IllegalArgumentException("Issuance has to be thirteen digits long");
        }
    }

    public String getTime() {
        return time;
    }

    public boolean timeIsValid(String time) {
        try {
            LocalDateTime.parse(time, DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm"));
        } catch (DateTimeParseException e) {
            return false;
        }
        return true;
    }

    public void setTime(String time) {
        if (timeIsValid(time)) {
            this.time = time;
        } else {
            throw new IllegalArgumentException("Time has to be in the format YYYY-MM-DD 00:00 (16 characters long including spaces, dashes, and colons): " + time);
        }
    }

    public long getUserid() {
        return userid;
    }

    public void setUserid(long userid) {
    	String userValidation = String.valueOf(userid); // Converts the long userid to a String to be used for validation
        if (userValidation.matches("[0-9]{1,6}")) {
            this.userid = userid;
        } else {
            throw new IllegalArgumentException("User id has to  be between one and five digits long");
        }
    }

    public String toString() {
        return "\n{\n" +
                "\"userid\": " + userid +
                ",\n\"requestid\": " + requestid +
                ",\n\"dentistid\": " + dentistid +
                ",\n\"issuance\": " + issuance +
                ",\n\"time\": \"" + time + "\"" +
                "\n}\n";
    }
}
