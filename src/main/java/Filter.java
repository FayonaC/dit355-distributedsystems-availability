import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * Filter class is filtering a received booking using the pipe-and-filter style.
 * It also filters out available time slots.
 */
public class Filter implements Supplier<ReceivedBooking>  {

    private ArrayList<Booking> receivedBookingRegistry;
    private ArrayList<Dentist> receivedDentistRegistry;
    private LocalDate receivedSelectedDate;
    private ReceivedBooking receivedBooking;

    /**
     * Throwing random exceptions.
     * @return a ReceivedBooking that can be either successful or rejected.
     */
    @Override
    public ReceivedBooking get() {
        /*if (ThreadLocalRandom.current().nextDouble() > 0.5) {
            throw new RuntimeException("PARTIAL FAILURE");
        }*/

        if (receivedBooking != null) {
            checkAvailability(receivedBooking, receivedDentistRegistry, receivedBookingRegistry);
        } else {
            System.out.println("Waiting for a booking request...");
        }

        return receivedBooking;
    }

    // If booking(i).dentistID is the same as requestBooking.dentistID, add to ArrayList of bookings from the same dentist
    public ArrayList<Booking> getDentistBookings(ReceivedBooking requestBooking, ArrayList<Booking> bookings) {
        ArrayList<Booking> requestedDentistConfirmedBookings = new ArrayList<>();

        for (int i = 0; i < bookings.size(); i++) {
            // if the requested dentist office has the request timeslot available, make the booking
            if (requestBooking.getDentistid() == bookings.get(i).getDentistid()) {
                Booking newBooking = bookings.get(i);
                requestedDentistConfirmedBookings.add(newBooking);
            }
        }
        return requestedDentistConfirmedBookings;
    }

    /**
     * This method takes in a booking request and the requestedDentistConfirmedBookings from the checkDentistBooking method
     * and it checks if there are any date and time matches
     * @param requestBooking
     * @param requestedDentistConfirmedBookings
     * @return
     */
    public boolean checkForMatchingDate(ReceivedBooking requestBooking, ArrayList<Booking> requestedDentistConfirmedBookings) {
        boolean check = false;

        for (int i = 0; i < requestedDentistConfirmedBookings.size(); i++) {
            if (requestedDentistConfirmedBookings.get(i).getTime().equals(requestBooking.getTime())) {
                check = true;
            }
        }
        return check;
    }

    /**
     * This method counts the number of appointments that have already been made with the requested dentist at the requested time
     * Used in checkAppointmentSlots
     * @param requestedDentistConfirmedBookings
     * @param requestBooking
     * @param dentistRegistry
     */
    public void countExistingAppointments(ArrayList<Booking> requestedDentistConfirmedBookings, ReceivedBooking requestBooking,
                                          ArrayList<Dentist> dentistRegistry) {
        int count = 0;
        long numberOfWorkingDentists = checkDentistNumber(dentistRegistry, requestBooking);

        for (int i = 0; i < requestedDentistConfirmedBookings.size(); i++) {

            if (requestedDentistConfirmedBookings.get(i).getTime().equals(requestBooking.getTime())) {
                count = count + 1;
            }
        }

        if (count < numberOfWorkingDentists) {
            setSuccessfulBooking(requestBooking);
        } else {
            setRejectedBooking(requestBooking);
        }
    }

    /**
     * This method is used in countExistingAppointments to find the number of dentists working at the requested location
     * @param dentistRegistry
     * @param requestBooking
     * @return
     */
    public long checkDentistNumber(ArrayList<Dentist> dentistRegistry, ReceivedBooking requestBooking) {
        long numberOfWorkingDentists = 0;
        for (int i = 0; i < dentistRegistry.size(); i++) {
            if (dentistRegistry.get(i).getId() == requestBooking.getDentistid()) {
                numberOfWorkingDentists = dentistRegistry.get(i).getDentistNumber();
            }
        }
        return numberOfWorkingDentists;
    }
    // If any bookings have the same date&time, boolean above is true, send array to XXXX method to check how many
    // If none, boolean becomes false, booking possible! Send request to Booking component

    /**
     * This method takes in a boolean from the checkForMatchingDate method, if it is true then it means there is already
     * at least one booking on that date, so it counts how many appointments there are and compares it to the number of
     * dentists working at that location
     * If it is false, a booking is created as there are no appointments on the requested date and time
     * @param checkedDate
     * @param requestedDentistConfirmedBookings
     * @param requestBooking
     * @param dentistRegistry
     */
    public void checkAppointmentSlots(boolean checkedDate, ArrayList<Booking> requestedDentistConfirmedBookings,
                                      ReceivedBooking requestBooking, ArrayList<Dentist> dentistRegistry) {
        if (checkedDate == true) {
            countExistingAppointments(requestedDentistConfirmedBookings, requestBooking, dentistRegistry);

        } else if (checkedDate == false) {
            setSuccessfulBooking(requestBooking);
        }
    }

    /**
     * This is the main method that checks if the requested booking can be made
     * @param requestBooking
     * @param dentistRegistry
     * @param bookingRegistry
     */
    public void checkAvailability(ReceivedBooking requestBooking, ArrayList<Dentist> dentistRegistry,
                                  ArrayList<Booking> bookingRegistry) {

        // Stores new filtered array of bookings for a particular dentist
        ArrayList<Booking> requestedDentistConfirmedBookings = getDentistBookings(requestBooking, bookingRegistry);

        // Stores a boolean that is returned by checkForMatchingDate (if there are existing appts on requested date)
        boolean checkedDate = checkForMatchingDate(requestBooking, requestedDentistConfirmedBookings);

        // Now calls method to either accept appointment if none on date&time, or check how many and compare to # of
        // dentists at location
        checkAppointmentSlots(checkedDate, requestedDentistConfirmedBookings, requestBooking, dentistRegistry);
    }

    public void makeDentistArray(MqttMessage message) throws Exception {
        JSONParser jsonParser = new JSONParser();
        Object jsonObject = jsonParser.parse(message.toString());
        JSONObject dentistObj = (JSONObject) jsonObject;
        JSONArray dentistsJSON = (JSONArray) dentistObj.get("dentists");

        ArrayList<Dentist> dentistsRegistry = new ArrayList<>();

        for (Object dentist : dentistsJSON) {
            JSONObject dObj = (JSONObject) dentist;
            try {

                long id = (Long) dObj.get("id");
                String dentistName = (String) dObj.get("name");
                String owner = (String) dObj.get("owner");
                long dentistNumber = (Long) dObj.get("dentists");
                String address = (String) dObj.get("address");
                String city = (String) dObj.get("city");

                JSONObject coordinateObj = (JSONObject) dObj.get("coordinate");
                JSONObject openinghoursObj = (JSONObject) dObj.get("openinghours");

                double latitude = (Double) coordinateObj.get("latitude");
                double longitude = (Double) coordinateObj.get("longitude");
                String monday = (String) openinghoursObj.get("monday");
                String tuesday = (String) openinghoursObj.get("tuesday");
                String wednesday = (String) openinghoursObj.get("wednesday");
                String thursday = (String) openinghoursObj.get("thursday");
                String friday = (String) openinghoursObj.get("friday");

                // Adding dentist objects created using the fields from the parsed JSON to arraylist
                Dentist newDentist = new Dentist(id, dentistName, owner, dentistNumber, address, city,
                        latitude, longitude, monday, tuesday, wednesday, thursday,
                        friday);

                dentistsRegistry.add(newDentist);
            } catch (IllegalArgumentException e) {
                System.err.println("Error when creating new Dentist: " + e.getMessage());
                System.err.println(dentistObj);
            } catch (ClassCastException e) {
                System.err.println("Error when creating new Dentist: " + e.getMessage());
                System.err.println(dentistObj);
            }
        }
        this.receivedDentistRegistry = dentistsRegistry;
    }

    public void makeBookingArray(MqttMessage message) throws Exception {
        JSONParser jsonParser = new JSONParser();
        Object jsonObject = jsonParser.parse(message.toString());
        JSONObject bookingObj = (JSONObject) jsonObject;
        JSONArray bookingsJSON = (JSONArray) bookingObj.get("bookings");

        ArrayList<Booking> bookingsRegistry = new ArrayList<>();

        for (Object booking : bookingsJSON) {
            JSONObject bObj = (JSONObject) booking;
            try {
                long userid = (Long) bObj.get("userid");
                long requestid = (Long) bObj.get("requestid");
                long dentistid = (Long) bObj.get("dentistid");
                long issuance = (Long) bObj.get("issuance");
                String time = (String) bObj.get("time");

                // Creating a booking object using the fields from the parsed JSON
                Booking newBooking = new Booking(userid, requestid, dentistid, issuance, time);

                bookingsRegistry.add(newBooking);
            } catch (IllegalArgumentException e) {
                System.err.println("Error when adding new Booking: " + e.getMessage());
            } catch (ClassCastException e) {
                System.err.println("Error when adding new Booking: " + e.getMessage());
            }
        }
        this.receivedBookingRegistry = bookingsRegistry;
    }

    public void makeReceivedBooking(Object message) throws ParseException, ClassCastException {
        JSONParser jsonParser = new JSONParser();
        ReceivedBooking newBooking = new ReceivedBooking();
        Object jsonObject = jsonParser.parse(message.toString());
        JSONObject parser = (JSONObject) jsonObject;

        try {
            long userid = (Long) parser.get("userid");
            long requestid = (Long) parser.get("requestid");
            long dentistid = (Long) parser.get("dentistid");
            long issuance = (Long) parser.get("issuance");
            String time = (String) parser.get("time");

        	// Creating a booking object using the fields from the parsed JSON
        	newBooking = new ReceivedBooking(userid, requestid, dentistid, issuance, time);
        } catch (IllegalArgumentException e){
        	System.err.println("Booking will be rejected: " + e.getMessage());
            newBooking = new ReceivedBooking((Long) parser.get("userid"), (Long) parser.get("requestid"), "none");
        }
        this.receivedBooking = newBooking;
    }

    private void setSuccessfulBooking(ReceivedBooking requestBooking) {
        ReceivedBooking acceptedBooking = new ReceivedBooking(requestBooking.getUserid(), requestBooking.getRequestid(), requestBooking.getDentistid(), requestBooking.getIssuance(), requestBooking.getTime());
        this.receivedBooking = acceptedBooking;
        System.out.println("ACCEPTED");

    }

    private void setRejectedBooking(ReceivedBooking requestBooking) {
        ReceivedBooking rejectedBooking = new ReceivedBooking(requestBooking.getUserid(), requestBooking.getRequestid(), "none");
        this.receivedBooking = rejectedBooking;
        System.out.println("REJECTED");
    }

    /**
     * Sets the selected date from an incoming message.
     * @param message incoming MqttMessage containing a selected date
     * @throws ParseException thrown when date cannot be parsed
     */
    public void setSelectedDate (MqttMessage message) throws ParseException {
        JSONParser jsonParser = new JSONParser();
        Object jsonObject = jsonParser.parse(message.toString());
        JSONObject parser = (JSONObject) jsonObject;

        String stringDate = (String) parser.get("date");
        LocalDate selectedDate = LocalDate.parse(stringDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        this.receivedSelectedDate = selectedDate;
    }

    /**
     * Creates schedules for each dental office and sets the availability based on booking registry.
     * @return the available slots which will be published ready to be used by the frontend
     */
    public ArrayList<Schedule> getAvailability() {
        ArrayList<Schedule> schedules = new ArrayList<>();

        for (Object dentist : receivedDentistRegistry) {
            Schedule schedule = new Schedule((Dentist) dentist, receivedSelectedDate);
            schedule.setUnavailableTimeSlots(receivedBookingRegistry);
            schedules.add(schedule);
        }

        return schedules;
    }
}
