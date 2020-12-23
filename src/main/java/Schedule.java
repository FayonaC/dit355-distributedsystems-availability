import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import static java.time.temporal.ChronoUnit.HOURS;

public class Schedule {
    private final int SLOT_DURATION = 30; // Length of appointment time slots

    private ArrayList<TimeSlot> timeSlots;
    private Dentist dentist; // Dental office
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
     * Taking all the timeslots and checking if they are available so that we end up with only available slots.
     * @return availableSlots
     */
    private ArrayList<TimeSlot> getAvailableTimeSlots() {
        ArrayList<TimeSlot> availableSlots = new ArrayList<>();

        for (TimeSlot timeSlot : timeSlots) {
            if (timeSlot.isAvailable()) {
                availableSlots.add(timeSlot);
            }
        }
        return availableSlots;
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

        setBreaks();
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

    /**
     * Sets lunch break and fika break as unavailable in the schedule.
     * Lunch is the hour in the middle of the day and break is always the first 30 minutes of the day.
     *
     * The lunch and break is dynamic which means that if a dental office changes its opening hours, the lunch and
     * fika break will move. This will probably lead to bugs since the break slots might be unavailable due to
     * bookings made before the change in opening hours. For the sake of simplicity and purposes of this project, we've
     * decided not to address this issue.
     *
     * TODO: Change how breaks are set, possibly in Booking so the breaks are saved.
     */
    private void setBreaks() {
        LocalTime start = timeSlots.get(0).startTime;
        LocalTime end = timeSlots.get(timeSlots.size()-1).endTime;

        // Difference in hours between start and end
        long hoursOpen = start.until(end, HOURS);
        long lunchHour = hoursOpen / 2;
        LocalTime lunchStartTime = start.plusHours(lunchHour);

        // Set the hour in the middle of the opening hours as lunch break
        for (int i = 0; i < timeSlots.size()-1; i++) {
            if (timeSlots.get(i).startTime == lunchStartTime) {
                timeSlots.get(i).setAvailable(false);
                timeSlots.get(i+1).setAvailable(false);
                break;
            }
        }

        // Fika break is always first slot of the day.
        timeSlots.get(0).setAvailable(false);
    }

    /**
     * Sets booked time slots to unavailable, i.e. changing available to false.
     * @param bookings unavailable time slots
     */
    public void setUnavailableTimeSlots(ArrayList<Booking> bookings) {
        for (Booking booking : bookings) {
            // Find bookings for the dental office
            if (booking.getDentistid() == dentist.getId()) {
                LocalDateTime dateTime = LocalDateTime.parse(booking.getTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm"));
                // Match booking date with schedule date
                if (selectedDate.equals(dateTime.toLocalDate())) {
                    // Loop through available slots for that date & match time slots
                    for (TimeSlot slot : timeSlots) {
                        if (slot.startTime.equals(dateTime.toLocalTime())) {
                            slot.setAvailable(false);
                        }
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        return "{ \"dentist\": " + dentist.getId() +
                ", \"date\": \"" + selectedDate +
                "\", \"timeSlots\": " + getAvailableTimeSlots().toString() +
        "}\n";
    }
}
