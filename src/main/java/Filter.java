import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Filter implements MqttCallback {

    private ArrayList receivedBookingRegistry;
    private ArrayList receivedDentistRegistry;

    private final static ExecutorService THREAD_POOL = Executors.newSingleThreadExecutor();

    private final IMqttClient middleware;

    public Filter(String userid, String broker) throws MqttException {
        middleware = new MqttClient(broker, userid);
        middleware.connect();
        middleware.setCallback(this);
    }

    public static void main(String[] args) throws MqttException {
        Filter s = new Filter("bookings-filter", "tcp://localhost:1883");
        s.subscribeToMessages("BookingRegistry");
        s.subscribeToMessages("BookingRequest");
        s.subscribeToMessages("Dentists");
        s.subscribeToMessages("AvailabilityRequest");
    }

    private void subscribeToMessages(String sourceTopic) {
        THREAD_POOL.submit(() -> {
            try {
                middleware.subscribe(sourceTopic);
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
        while (middleware.isConnected() == false) {

            // reestablish lost connection
            try {
                Thread.sleep(3000);
                System.out.println("Reconnecting..");
                middleware.reconnect();

            } catch (Exception e) {
                throwable.getMessage();
            }
        }
        System.out.println("Connection to broker reestablished!");
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
                break;
            case "Dentists":
                receivedDentistRegistry = makeDentistArray(incoming);
                break;
            default:
                System.out.println("Topic not found");
        }

        if (receivedBooking != null) {
            checkAvailability(receivedBooking, receivedDentistRegistry, receivedBookingRegistry);
        } else {
            System.out.println("Waiting for booking request...");
        }
    }

    private void dump(ReceivedBooking receivedBooking, String sinkTopic) throws MqttException {
        MqttMessage outgoing = new MqttMessage();
        outgoing.setPayload(receivedBooking.toString().getBytes());
        middleware.publish(sinkTopic, outgoing);
    }

    // If booking(i).dentistID is the same as requestBooking.dentistID, add to ArrayList of bookings from the same dentist
    public ArrayList checkDentistBookings(ReceivedBooking requestBooking, ArrayList<Booking> bookings) throws MqttException {
        ArrayList<Booking> requestedDentistConfirmedBookings = new ArrayList<Booking>();

        for (int i = 0; i < bookings.size(); i++) {
            // if the requested dentist office has the request timeslot available, make the booking
            if (requestBooking.getDentistid() == bookings.get(i).getDentistid()) {
                Booking newBooking = bookings.get(i);
                requestedDentistConfirmedBookings.add(newBooking);

            } else {
                // Adds booking when there is a dentist with no bookings
                ReceivedBooking AcceptedBooking = new ReceivedBooking(requestBooking.getUserid(), requestBooking.getRequestid(), requestBooking.getDentistid(), requestBooking.getIssuance(), requestBooking.getTime());
                dump(AcceptedBooking, "SuccessfulBooking");
                System.out.println("ACCEPTED");
            }
        }
        return requestedDentistConfirmedBookings;
    }

    // This method takes in a booking request and the requestedDentistConfirmedBookings from the checkDentistBooking method
    // and it checks if there are any date and time matches
    public boolean checkForMatchingDate(ReceivedBooking requestBooking, ArrayList<Booking> requestedDentistConfirmedBookings) {
        boolean check = false;

        for (int i = 0; i < requestedDentistConfirmedBookings.size(); i++) {
            if (requestedDentistConfirmedBookings.get(i).getTime().equals(requestBooking.getTime())) {
                check = true;
            }
        }
        return check;
    }

    // This counts the number of appointments have already been made with the requested dentist at the requested time
    // Used in checkAppointmentSlots
    public void countExistingAppointments(ArrayList<Booking> requestedDentistConfirmedBookings, ReceivedBooking requestBooking,
                                          ArrayList<Dentist> dentistRegistry) throws MqttException {
        int count = 0;
        long numberOfWorkingDentists = 0;
        for (int i = 0; i < requestedDentistConfirmedBookings.size(); i++) {

            if (requestedDentistConfirmedBookings.get(i).getTime().equals(requestBooking.getTime())) {
                count = count + 1;
            }

        }
        numberOfWorkingDentists = checkDentistNumber(dentistRegistry, requestBooking);

        if (count < numberOfWorkingDentists) {
            ReceivedBooking AcceptedBooking = new ReceivedBooking(requestBooking.getUserid(), requestBooking.getRequestid(), requestBooking.getDentistid(), requestBooking.getIssuance(), requestBooking.getTime());
            dump(AcceptedBooking, "SuccessfulBooking");
            System.out.println("ACCEPTED");
        } else {
            ReceivedBooking rejectedBooking = new ReceivedBooking(requestBooking.getUserid(), requestBooking.getRequestid(), "none");
            dump(rejectedBooking, "BookingResponse");
            System.out.println("REJECTED");
        }
    }

    // Used in countExistingAppointments to find the number of dentists working at the requested location
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
    public void checkAppointmentSlots(boolean checkedDate, ArrayList<Booking> requestedDentistConfirmedBookings,
                                      ReceivedBooking requestBooking, ArrayList<Dentist> dentistRegistry) throws MqttException {
        if (checkedDate == true) {
            countExistingAppointments(requestedDentistConfirmedBookings, requestBooking, dentistRegistry);

        } else if (checkedDate == false) {
            ReceivedBooking AcceptedBooking = new ReceivedBooking(requestBooking.getUserid(), requestBooking.getRequestid(), requestBooking.getDentistid(), requestBooking.getIssuance(), requestBooking.getTime());
            dump(AcceptedBooking, "SuccessfulBooking");
            System.out.println("ACCEPTED");
        }
    }

    // This is the main method that checks if the requested booking can be made
    public void checkAvailability(ReceivedBooking requestBooking, ArrayList<Dentist> dentistRegistry,
                                  ArrayList<Booking> bookingRegistry) throws MqttException {

        // Stores new filtered array of bookings for a particular dentist
        ArrayList<Booking> requestedDentistConfirmedBookings = checkDentistBookings(requestBooking, bookingRegistry);

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

        // Creating a booking object using the fields from the parsed JSON
        ReceivedBooking newBooking = new ReceivedBooking(userid, requestid, dentistid, issuance, time);

        return newBooking;
    }
}
