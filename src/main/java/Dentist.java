public class Dentist {

    private long id;
    private String dentistName;
    private String owner;
    private long dentistNumber;
    private String address;
    private String city;
    private double latitude;
    private double longitude;
    private String monday;
    private String tuesday;
    private String wednesday;
    private String thursday;
    private String friday;

    public Dentist(long id, String dentistName, String owner, long dentistNumber, String address, String city,
                   double latitude, double longitude, String monday, String tuesday, String wednesday, String thursday,
                   String friday) {
        this.id = id;
        this.dentistName = dentistName;
        this.owner = owner;
        this.dentistNumber = dentistNumber;
        this.address = address;
        this.city = city;
        this.latitude = latitude;
        this.longitude = longitude;
        this.monday = monday;
        this.tuesday = tuesday;
        this.wednesday = wednesday;
        this.thursday = thursday;
        this.friday = friday;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDentistName() {
        return dentistName;
    }

    public void setDentistName(String dentistName) {
        this.dentistName = dentistName;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public long getDentistNumber() {
        return dentistNumber;
    }

    public void setDentistNumber(long dentistNumber) {
        this.dentistNumber = dentistNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getMonday() {
        return monday;
    }

    public void setMonday(String monday) {
        this.monday = monday;
    }

    public String getTuesday() {
        return tuesday;
    }

    public void setTuesday(String tuesday) {
        this.tuesday = tuesday;
    }

    public String getWednesday() {
        return wednesday;
    }

    public void setWednesday(String wednesday) {
        this.wednesday = wednesday;
    }

    public String getThursday() {
        return thursday;
    }

    public void setThursday(String thursday) {
        this.thursday = thursday;
    }

    public String getFriday() {
        return friday;
    }

    public void setFriday(String friday) {
        this.friday = friday;
    }

    /*add toString*/
    @Override
    public String toString() {
        return "\n{\n" +
                "\"id\": \"" + id + "\"" +
                ",\n\"name\": \"" + dentistName + "\"" +
                ",\n\"owner\": \"" + owner + "\"" +
                ",\n\"dentists\": " + dentistNumber +
                ",\n\"address\": \"" + address + "\"" +
                ",\n\"city\": \"" + city + "\"" +
                ",\n\"coordinate\": {" +
                "\n\"latitude\": " + latitude +
                ",\n\"longitude\": " + longitude +
                "\n},\n\"openinghours\": {" +
                "\n\"monday\": \"" + monday + "\"" +
                ",\n\"tuesday\": \"" + tuesday + "\"" +
                ",\n\"wednesday\": \"" + wednesday + "\"" +
                ",\n\"thursday\": \"" + thursday + "\"" +
                ",\n\"friday\": \"" + friday + "\"" +
                "\n}\n}";
    }
}
