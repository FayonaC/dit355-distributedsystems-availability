import org.eclipse.paho.client.mqttv3.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Filter implements MqttCallback {

    private ArrayList receivedBookingRegistry;
    private ArrayList receivedDentistRegistry;
    private LocalDate receivedSelectedDate;

    private final static ExecutorService THREAD_POOL = Executors.newSingleThreadExecutor();

    private final IMqttClient middleware;
    static Filter s;

    static {
        try {
            s = new Filter("bookings-filter", "tcp://localhost:1883");
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    public Filter(String userid, String broker) throws MqttException {
        middleware = new MqttClient(broker, userid);
        middleware.connect();
        middleware.setCallback(this);
    }

    public static void main(String[] args) throws MqttException {
        s.subscribeToMessages("BookingRegistry");
        s.subscribeToMessages("BookingRequest");
        s.subscribeToMessages("Dentists");
        s.subscribeToMessages("AvailabilityRequest");
    }

    private void subscribeToMessages(String sourceTopic) {
        THREAD_POOL.submit(() -> {
            try {
                middleware.subscribe(sourceTopic, 1);
            } catch (MqttSecurityException e) {
                e.printStackTrace();
            } catch (MqttException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void connectionLost(Throwable throwable) {
        System.out.println("Connection lost!");
        long startTime = System.currentTimeMillis();
        long elapsedTime = 0;

        while (middleware.isConnected() == false && elapsedTime < 60 * 1000) {
            // reestablish lost connection
            try {
                Thread.sleep(3000);
                System.out.println("Reconnecting..");
                middleware.reconnect();
                elapsedTime = (new Date()).getTime() - startTime;

            } catch (Exception e) {

            }
        }
        if (middleware.isConnected() == false) {
            try {
                System.out.println("Tried reconnecting for 1 minute, now disconnecting..");
                middleware.unsubscribe(new String[]{"BookingRegistry", "Dentists", "BookingRequest", "AvailabilityRequest", "SelectedDate"});
                middleware.disconnect();
                middleware.close();
                System.out.println("Availability RIP :(");
                System.out.println("Please restart broker and component");

            } catch (
                    MqttException mqttException) {
                throwable.getMessage();
            }
        }

        if (middleware.isConnected() == true) {
            try {
                middleware.subscribe(new String[]{"BookingRegistry", "Dentists", "BookingRequest", "AvailabilityRequest", "SelectedDate"});
                System.out.println("Connection to broker reestablished!");
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
    }

    @Override
    public void messageArrived(String topic, MqttMessage incoming) throws Exception {
        ReceivedBooking receivedBooking = null;

        switch (topic) {
            case "BookingRequest":
                receivedBooking = makeReceivedBooking(incoming);
                break;
            case "BookingRegistry":
                receivedBookingRegistry = makeBookingArray((incoming));
                System.out.println("We have received an updated booking registry.");
                break;
            case "Dentists":
                receivedDentistRegistry = makeDentistArray(incoming);
                System.out.println("We have received an updated dentist registry.");
                break;
            case "AvailabilityRequest":
                receivedSelectedDate = getSelectedDate(incoming);
                System.out.println("We have received a selected date.");
                getAvailability();
                break;
            default:
                System.out.println("Topic not found");
        }

        if (receivedBooking != null) {
            checkAvailability(receivedBooking, receivedDentistRegistry, receivedBookingRegistry);
        } else {
            System.out.println("Waiting for a booking request...");
        }
    }

    private void dump(ReceivedBooking receivedBooking, String sinkTopic) throws MqttException {
        MqttMessage outgoing = new MqttMessage();
        outgoing.setQos(1);
        outgoing.setPayload(receivedBooking.toString().getBytes());
        middleware.publish(sinkTopic, outgoing);
    }

    // If booking(i).dentistID is the same as requestBooking.dentistID, add to ArrayList of bookings from the same dentist
    public ArrayList getDentistBookings(ReceivedBooking requestBooking, ArrayList<Booking> bookings) throws MqttException {
        ArrayList<Booking> requestedDentistConfirmedBookings = new ArrayList<Booking>();

        for (int i = 0; i < bookings.size(); i++) {
            // if the requested dentist office has the request timeslot available, make the booking
            if (requestBooking.getDentistid() == bookings.get(i).getDentistid()) {
                Booking newBooking = bookings.get(i);
                requestedDentistConfirmedBookings.add(newBooking);
            } /*else {
                // Adds booking when there is a dentist with no bookings
                ReceivedBooking AcceptedBooking = new ReceivedBooking(requestBooking.getUserid(), requestBooking.getRequestid(), requestBooking.getDentistid(), requestBooking.getIssuance(), requestBooking.getTime());
                dump(AcceptedBooking, "SuccessfulBooking");
                System.out.println("ACCEPTED");
            }*/
        }
        return requestedDentistConfirmedBookings;
    }

    // This method takes in a booking request and the requestedDentistConfirmedBookings from the checkDentistBooking method
    // and it checks if there are any date and time matches
    public boolean checkForMatchingDate(ReceivedBooking
                                                requestBooking, ArrayList<Booking> requestedDentistConfirmedBookings) {
        boolean check = false;

        for (int i = 0; i < requestedDentistConfirmedBookings.size(); i++) {
            if (requestedDentistConfirmedBookings.get(i).getTime().equals(requestBooking.getTime())) {
                check = true;
            }
        }
        return check;
    }

    // This method counts the number of appointments that have already been made with the requested dentist at the requested time
    // Used in checkAppointmentSlots
    public void countExistingAppointments(ArrayList<Booking> requestedDentistConfirmedBookings, ReceivedBooking requestBooking,
                                          ArrayList<Dentist> dentistRegistry) throws MqttException {
        int count = 0;
        long numberOfWorkingDentists = checkDentistNumber(dentistRegistry, requestBooking);

        for (int i = 0; i < requestedDentistConfirmedBookings.size(); i++) {

            if (requestedDentistConfirmedBookings.get(i).getTime().equals(requestBooking.getTime())) {
                count = count + 1;
            }

        }

        System.out.println(numberOfWorkingDentists);
        System.out.println(count);

        if (count < numberOfWorkingDentists) {
            publishSuccessfulBooking(requestBooking);
        } else {
            publishRejectedBooking(requestBooking);
        }
    }

    // This method is used in countExistingAppointments to find the number of dentists working at the requested location
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

    // This method takes in a boolean from the checkForMatchingDate method, if it is true then it means there is already
    // at least one booking on that date, so it counts how many appointments there are and compares it to the number of
    // dentists working at that location
    // If it is false, a booking is created as there are no appointments on the requested date and time
    public void checkAppointmentSlots(boolean checkedDate, ArrayList<
            Booking> requestedDentistConfirmedBookings,
                                      ReceivedBooking requestBooking, ArrayList<Dentist> dentistRegistry) throws MqttException {
        if (checkedDate == true) {
            countExistingAppointments(requestedDentistConfirmedBookings, requestBooking, dentistRegistry);

        } else if (checkedDate == false) {
            publishSuccessfulBooking(requestBooking);
        }
    }

    // This is the main method that checks if the requested booking can be made
    public void checkAvailability(ReceivedBooking requestBooking, ArrayList<Dentist> dentistRegistry,
                                  ArrayList<Booking> bookingRegistry) throws MqttException {

        // Stores new filtered array of bookings for a particular dentist
        ArrayList<Booking> requestedDentistConfirmedBookings = getDentistBookings(requestBooking, bookingRegistry);

        // Stores a boolean that is returned by checkForMatchingDate (if there are existing appts on requested date)
        boolean checkedDate = checkForMatchingDate(requestBooking, requestedDentistConfirmedBookings);

        // Now calls method to either accept appointment if none on date&time, or check how many and compare to # of
        // dentists at location
        checkAppointmentSlots(checkedDate, requestedDentistConfirmedBookings, requestBooking, dentistRegistry);
    }

    public ArrayList makeDentistArray(MqttMessage message) throws Exception {
        JSONParser jsonParser = new JSONParser();
        Object jsonObject = jsonParser.parse(message.toString());
        JSONObject dentistObj = (JSONObject) jsonObject;
        JSONArray dentistsJSON = (JSONArray) dentistObj.get("dentists");

        ArrayList<Dentist> dentistsRegistry = new ArrayList<>();

        for (Object dentist : dentistsJSON) {

            JSONObject dObj = (JSONObject) dentist;

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
        }
        return dentistsRegistry;
    }

    public ArrayList makeBookingArray(MqttMessage message) throws Exception {
        JSONParser jsonParser = new JSONParser();
        Object jsonObject = jsonParser.parse(message.toString());
        JSONObject bookingObj = (JSONObject) jsonObject;
        JSONArray bookingsJSON = (JSONArray) bookingObj.get("bookings");

        ArrayList<Booking> bookingsRegistry = new ArrayList<>();

        for (Object booking : bookingsJSON) {

            JSONObject bObj = (JSONObject) booking;

            long userid = (Long) bObj.get("userid");
            long requestid = (Long) bObj.get("requestid");
            long dentistid = (Long) bObj.get("dentistid");
            long issuance = (Long) bObj.get("issuance");
            String time = (String) bObj.get("time");

            // Creating a booking object using the fields from the parsed JSON
            Booking newBooking = new Booking(userid, requestid, dentistid, issuance, time);

            bookingsRegistry.add(newBooking);
        }
        return bookingsRegistry;
    }

    public ReceivedBooking makeReceivedBooking(MqttMessage message) throws Exception {
        JSONParser jsonParser = new JSONParser();
        Object jsonObject = jsonParser.parse(message.toString());
        JSONObject parser = (JSONObject) jsonObject;

        long userid = (Long) parser.get("userid");
        long requestid = (Long) parser.get("requestid");
        long dentistid = (Long) parser.get("dentistid");
        long issuance = (Long) parser.get("issuance");
        String time = (String) parser.get("time");

        ReceivedBooking newBooking = null;
        
        try {
        	// Creating a booking object using the fields from the parsed JSON
        	newBooking = new ReceivedBooking(userid, requestid, dentistid, issuance, time);
        } catch (IllegalArgumentException e){
        	System.err.println("Error when creating new Booking: " + e.getMessage());
            String failedResponseJSON = "\n{\n" +
            		  "\"userid\": " + userid +
                      ",\n\"requestid\": " + requestid +
                      ",\n\"time\": \"" + time + "\"" +
                      "\n}\n";

            publishMalformedBooking(failedResponseJSON);
        }
        return newBooking;
    }
    
    void publishMalformedBooking(String failedResponseJSON) throws MqttPersistenceException, MqttException {
        MqttMessage message = new MqttMessage();
        String msg = failedResponseJSON.toString();
        message.setQos(1);
        message.setPayload(msg.getBytes());
        middleware.publish("BookingResponse", message);
        System.out.println(message.toString());
    }

    public void publishSuccessfulBooking(ReceivedBooking requestBooking) throws MqttException {
        ReceivedBooking acceptedBooking = new ReceivedBooking(requestBooking.getUserid(), requestBooking.getRequestid(), requestBooking.getDentistid(), requestBooking.getIssuance(), requestBooking.getTime());
        dump(acceptedBooking, "SuccessfulBooking");
        System.out.println("ACCEPTED");
    }

    public void publishRejectedBooking(ReceivedBooking requestBooking) throws MqttException {
        ReceivedBooking rejectedBooking = new ReceivedBooking(requestBooking.getUserid(), requestBooking.getRequestid(), "none");
        dump(rejectedBooking, "BookingResponse");
        System.out.println("REJECTED");
    }


    public LocalDate getSelectedDate (MqttMessage message) throws Exception {
        JSONParser jsonParser = new JSONParser();
        Object jsonObject = jsonParser.parse(message.toString());
        JSONObject parser = (JSONObject) jsonObject;

        String stringDate = (String) parser.get("date");
        LocalDate selectedDate = LocalDate.parse(stringDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        return selectedDate;
    }

    /**
     * Gets the available slots which will be published ready to be used by the frontend
     */
    public void getAvailability() throws Exception {
        ArrayList<Schedule> schedules = new ArrayList<>();

        for (Object dentist : receivedDentistRegistry) {
            Schedule schedule = new Schedule((Dentist) dentist, receivedSelectedDate);
            schedule.setUnavailableTimeSlots(receivedBookingRegistry);
            schedules.add(schedule);
        }

        sendMessage("free-slots", "{ \"schedules\": " + schedules + "}");
    }

    /**
     * Method to publish to the MQTT broker
     *
     * @param topic
     * @param msg
     * @throws MqttException
     */
    public void sendMessage(String topic, String msg) throws MqttException {
        MqttMessage message = new MqttMessage();
        message.setPayload(msg.getBytes());
        middleware.publish(topic, message);
    }
}
