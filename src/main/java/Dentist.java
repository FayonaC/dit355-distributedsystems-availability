public class Dentist {

    private long id;
    private long dentistNumber;


    public Dentist(long id, long dentistNumber) {
        this.id = id;
        this.dentistNumber = dentistNumber;

    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getDentistNumber() {
        return dentistNumber;
    }

    public void setDentistNumber(long dentistNumber) {
        this.dentistNumber = dentistNumber;
    }
}
