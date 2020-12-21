import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;

public class Schedule {
    private ArrayList<TimeSlot> timeSlots;
    private Dentist dentist; // Dental office
    private final int SLOT_DURATION = 30; // Length of appointment time slots
    private LocalDate selectedDate;

    /**
     * Needs to know the dental office, opening hours, possibly which dentist it belongs to as well as the date.
     * @param dentist dental office
     * @param selectedDate date for the schedule
     */
    public Schedule(Dentist dentist, LocalDate selectedDate) {
        timeSlots = new ArrayList<>();
        setDentist(dentist);
        setSelectedDate(selectedDate);
        generateTimeSlots();
    }

    public Dentist getDentist() {
        return dentist;
    }

    public void setDentist(Dentist dentist) {
        this.dentist = dentist;
    }

    public LocalDate getSelectedDate() {
        return selectedDate;
    }

    public void setSelectedDate(LocalDate selectedDate) {
        this.selectedDate = selectedDate;
    }

    public ArrayList<TimeSlot> getTimeSlots() {
        return timeSlots;
    }

    /**
     * Creates time slots and fills the schedule with them based on the opening hours and day of the week.
     */
    private void generateTimeSlots() {
        HashMap<String, String> weekOpeningHours = dentist.getOpeningHours();

        // Get the day of the week from the selected date
        DayOfWeek selectedDayOfWeek = selectedDate.getDayOfWeek();
        String weekDay = selectedDayOfWeek.name();

        // Splits opening hour string so we get the start and end time
        ArrayList<LocalTime> startClosingHours = splitOpeningHours(weekOpeningHours.get(weekDay.toLowerCase()));

        // Where to start the loop, also keeps track of the start of the next time slot
        LocalTime slotStartTime = startClosingHours.get(0);
        LocalTime closingHour = startClosingHours.get(1);

        // Creates time slots until slotStartTime is the same as closingHour
        while (slotStartTime != closingHour) {
            LocalTime endTime = slotStartTime.plusMinutes(SLOT_DURATION);
            timeSlots.add(new TimeSlot(slotStartTime, endTime, true));
            slotStartTime = endTime;
        }
    }

    /**
     * Splits the opening hours string into two separate LocalTimes.
     * @param openingHoursString opening hours as a string
     * @return Opening hour and closing hour as an ArrayList
     */
    private ArrayList<LocalTime> splitOpeningHours(String openingHoursString) {
        String[] openingHours = openingHoursString.split("-");

        LocalTime startHour = LocalTime.parse(openingHours[0], DateTimeFormatter.ofPattern("H:mm"));
        LocalTime closingHour = LocalTime.parse(openingHours[1], DateTimeFormatter.ofPattern("H:mm"));

        ArrayList<LocalTime> openingHoursList = new ArrayList<>();
        openingHoursList.add(startHour);
        openingHoursList.add(closingHour);

        return openingHoursList;
    }
}
