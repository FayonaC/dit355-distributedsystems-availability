import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;

public class Availability {


    public String getDayOfTheWeek(ArrayList<Dentist> dentistReg, LocalDate selectedDate) {

        DayOfWeek selectedDayOfWeek = selectedDate.getDayOfWeek();
        String day = selectedDayOfWeek.name();
        String openingHours = "";

        for (Dentist dentist : dentistReg) {

            switch (day.toLowerCase()) {
                case "monday":
                    openingHours = dentist.getMonday();
                    break;

                case "tuesday":
                    openingHours = dentist.getTuesday();
                    break;

                case "wednesday":
                    openingHours = dentist.getWednesday();
                    break;

                case "thursday":
                    openingHours = dentist.getThursday();
                    break;

                case "friday":
                    openingHours = dentist.getFriday();
                    break;

                default:
                    System.out.println("Error");
                    break;
            }
        }
        return openingHours;
    }
}
