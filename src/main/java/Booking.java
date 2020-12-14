import java.math.BigInteger;

public class Booking {

    private long userid;
    private long requestid;
    private long dentistid;
    private long issuance;
    private String time;

    public Booking(long userid, long requestid, long dentistid, long issuance, String time) {
        this.userid = userid;
        this.requestid = requestid;
        this.dentistid = dentistid;
        this.issuance = issuance;
        this.time = time;
    }

    public long getUserid() {
        return userid;
    }

    public void setUserid(long userid) {
        this.userid = userid;
    }

    public long getRequestid() {
        return requestid;
    }

    public void setRequestid(long requestid) {
        this.requestid = requestid;
    }

    public long getDentistid() {
        return dentistid;
    }

    public void setDentistid(long dentistid) {
        this.dentistid = dentistid;
    }

    public long getIssuance() {
        return issuance;
    }

    public void setIssuance(long issuance) {
        this.issuance = issuance;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

}
