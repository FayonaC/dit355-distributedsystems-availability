public class ReceivedBooking {
    public long userid;
    public long requestid;
    public long dentistid;
    public long issuance;
    public String time;

    public ReceivedBooking(long userid, long requestid, long dentistid, long issuance, String time) {
        this.userid = userid;
        this.requestid = requestid;
        this.dentistid = dentistid;
        this.issuance = issuance;
        this.time = time;
    }

    public ReceivedBooking(long userid, long requestid, String time) {
        this.userid = userid;
        this.requestid = requestid;
        this.time = time;
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
