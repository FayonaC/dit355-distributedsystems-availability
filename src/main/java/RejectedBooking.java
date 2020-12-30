public class RejectedBooking {
    private long userid;
    private long requestid;
    private String time;

    public long getRequestid() {
        return requestid;
    }

    public void setRequestid(long requestid) {
        this.requestid = requestid;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public long getUserid() {
        return userid;
    }

    public void setUserid(long userid) {
        this.userid = userid;
    }

    public RejectedBooking(long userid, long requestid, String time) {
        this.userid = userid;
        this.requestid = requestid;
        this.time = time;
    }

    public String toString() {
        return "\n{\n" +
                "\"userid\": " + userid +
                ",\n\"requestid\": " + requestid +
                ",\n\"time\": \"" + time + "\"" +
                "\n}\n";
    }
}
