import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;

public class Availability { // Change class name

    // This method takes the Dentist Registry (from Dentist component) and Selected Date (from frontend)
    // It attempts to change the selected date to a day of the week, then find the opening hours on the selected day
    // for every dental clinic
    // Rename method or split for/switch logic into another method as it seems to be doing more now
    public String getDayOfTheWeek(ArrayList<Dentist> dentistReg, LocalDate selectedDate) {

        DayOfWeek selectedDayOfWeek = selectedDate.getDayOfWeek();
        String day = selectedDayOfWeek.name();
        String openingHours = "";

        // This for loop/switch case is not working correctly and needs to be worked on
        // ATM: It does not store each dental clinics operation hours separately, it overwrites the openingHours
        // attribute each time it loops to a new clinic
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
