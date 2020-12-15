import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Filter implements MqttCallback {

    ArrayList receivedBookingRegistry;
    ArrayList receivedDentistRegistry;

    private final static ExecutorService THREAD_POOL = Executors.newSingleThreadExecutor();

    private final IMqttClient middleware;

    public Filter(String userid, String broker) throws MqttException {
        middleware = new MqttClient(broker, userid);
        middleware.connect();
        middleware.setCallback(this);
    }

    public static void main(String[] args) throws MqttException, InterruptedException {
        Filter s = new Filter("test-filter", "tcp://localhost:1883");
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


            // reestablish connection lost? PLan b
            try {
                Thread.sleep(3000);
                System.out.println("Reconnecting..");
                middleware.reconnect();

                // middleware.setCallback(this);  // unclear what this method does (still works with or without).
            } catch (Exception e) {
                throwable.getMessage();
            }
        }
        System.out.println("Connection to broker reestablished!");
        System.out.println(middleware.isConnected());
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
    }

    /**
     * For simplicity we here assume that the message payload is equal to the filter
     * sink. It is important that the sink is part of the message, as the filter
     * itself is stateless. It does not know where it is (subscription topic, set on
     * instantiation), nor where it publishes (publishing topic, set per message).
     */
    @Override
    public void messageArrived(String topic, MqttMessage incoming) throws Exception {

        ReceivedBooking receivedBooking = null;
        System.out.println("MESSAGE RECEIVED");
        //receivedBooking = makeReceivedBooking(incoming); // We take in the booking request from the frontend and
        // create a new received booking item with certain fields
        // System.out.println(receivedBooking);
        //checkAvailability1(receivedBooking);

        System.out.println("Starting to check..");

        // String sinkTopic = extractTopic(incoming);

        switch (topic)
        {
            case "BookingRequest":

                System.out.println("Found booking request");
                receivedBooking = makeReceivedBooking(incoming);
                System.out.println("Line 101 " + receivedBooking);
                break;
            case "BookingRegistry":
                System.out.println("Found booking registry");
                receivedBookingRegistry = makeBookingArray((incoming));
                System.out.println("BOOKING REG DONE in messageArrived");
                System.out.println("Line 107 " + receivedBookingRegistry);
                break;
            case "Dentists":
                System.out.println("Found dentist request");
                receivedDentistRegistry = makeDentistArray(incoming);
                System.out.println("DENTIST REG DONE in messageArrived");
                System.out.println("Line 113 " + receivedDentistRegistry);
                break;
            default:
                System.out.println("Topic not found");
        }

        ReceivedBooking demandedBooking = receivedBooking;
        System.out.println("Line 120 " + demandedBooking);
        ArrayList<Dentist> dentistList = receivedDentistRegistry;
        System.out.println("Line 122 " + dentistList);
        ArrayList<Booking> bookingList = receivedBookingRegistry;
        System.out.println("Line 124 " + bookingList);

       /** if (topic.equals("BookingRequest")) { // Request json from frontend
            System.out.println("Found booking request");

            receivedBooking = makeReceivedBooking(incoming); // We take in the booking request from the frontend and
            // create a new received booking item with certain fields
            System.out.println("BOOKING REQ DONE in messageArrived");
        } else if (topic.equals("BookingRegistry")) { // Booking json from Booking component
            System.out.println("Found booking registry");

            receivedBookingRegistry = makeBookingArray((incoming));
            System.out.println("BOOKING REG DONE in messageArrived");
            System.out.println(receivedBookingRegistry);

        } else if (topic.equals("Dentists")) { // Dentists json from Dentists component
            System.out.println("Found dentist request");

            receivedDentistRegistry = makeDentistArray(incoming);
            System.out.println("DENTIST REG DONE in messageArrived");
        }
        */
        System.out.println("All info acquired, checking availability...");

        if (receivedBooking != null) {
            checkAvailability(demandedBooking, dentistList, bookingList);
            System.out.println("Availability check finished");
            System.out.println(receivedDentistRegistry);
            System.out.println(receivedBookingRegistry);
        } else {
            System.out.println("MORE INFO NEEEEEEDED! D:");
        }


    }

    private String extractTopic(MqttMessage incoming) {
        return incoming.toString();
    }

    private void dump(ReceivedBooking receivedBooking, String sinkTopic) throws MqttException {
        MqttMessage outgoing = new MqttMessage();
        outgoing.setPayload(receivedBooking.toString().getBytes());
        middleware.publish(sinkTopic, outgoing);
    }


    public void checkAvailability(ReceivedBooking requestBooking, ArrayList<Dentist> dentists,
                                  ArrayList<Booking> bookings) throws MqttException {
        // Needs to take in data from Dentist, Booking and booking response from Frontend and check them
        // Uses dentist id from booking response, iterates through dentist array for matching dentist id
        // if requestBooking.dentistid == bookings.dentistid AND resp.time!=bookings.time then booking OK
        // if requestBooking.dentistid == bookings.dentistid AND resp.time==bookings.time, then check dentists.dentists number
        // if dentists.dentists number > number of copies of bookings.dentistid + bookings.time then booking OK
        System.out.println("checkAvailability method starts running here");
        for (int i = 0; i < bookings.size(); i++) {
            System.out.println("This is the info we want, yay!" + bookings.get(i).getTime());
            System.out.println("This is the booking request info " + requestBooking);
            // if the requested dentist office has the request timeslot available, make the booking
            if ((requestBooking.dentistid == bookings.get(i).getDentistid()) && (!requestBooking.time.equals(bookings.get(i).getTime()))) {

            //** NOTE: sinkTopic and topic are different, these will likely need to be changed in all dump parameters

                // make booking, publish information to Booking component
                ReceivedBooking AcceptedBooking = new ReceivedBooking(requestBooking.userid, requestBooking.requestid, requestBooking.dentistid, requestBooking.issuance, requestBooking.time);
                dump(AcceptedBooking, "SuccessfulBooking");

                // if there is already an appointment in requested slot, check if there are other available dentists
            } if ((requestBooking.dentistid == bookings.get(i).getDentistid()) && (requestBooking.time.equals(bookings.get(i).getTime()))) {

                long sameSlot = 0;
                for (int j = 0; j < bookings.size(); j++) {

                    if ((requestBooking.dentistid == bookings.get(j).getDentistid()) && (requestBooking.time.equals(bookings.get(j).getTime()))) {
                        sameSlot = sameSlot + 1;
                    }
                }
                long workingDentists = 0;
                for (int k = 0; k < dentists.size(); k++) {
                    if (requestBooking.dentistid == dentists.get(k).getId()) {
                        workingDentists = dentists.get(k).getDentistNumber();
                    }
                }

                if (workingDentists > sameSlot) {
                   // make booking, publish information to Booking component
                    ReceivedBooking AcceptedBooking = new ReceivedBooking(requestBooking.userid, requestBooking.requestid, requestBooking.dentistid, requestBooking.issuance, requestBooking.time);
                    dump(AcceptedBooking, "SuccessfulBooking");

                } else {
                    // reject booking, send rejection back to user
                    ReceivedBooking rejectedBooking = new ReceivedBooking(requestBooking.userid, requestBooking.requestid, "none");
                    dump(rejectedBooking, "BookingResponse");
                    System.out.println("REJECTED");
                }

                // the requested dentist is not available for requested time
            } else {
                // reject booking, send rejection back to user
                ReceivedBooking rejectedBooking = new ReceivedBooking(requestBooking.userid, requestBooking.requestid, "none");
                dump(rejectedBooking, "BookingResponse");
                System.out.println("REJECTED");
            }

        }
        System.out.println("Finished the for-loop");
    }

    public ArrayList makeDentistArray(MqttMessage message) throws Exception {
        JSONParser jsonParser = new JSONParser();
        Object jsonObject = jsonParser.parse(message.toString());
        JSONObject dentistObj = (JSONObject) jsonObject;
        JSONArray dentistsJSON = (JSONArray) dentistObj.get("dentists");

        ArrayList<Dentist> dentistsRegistry = new ArrayList<>();

        for (Object dentist : dentistsJSON) {

            JSONObject dObj = (JSONObject) dentist;

            // Able to create a JSON object from the message but cannot get info from fields
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

            // Adding dentist objects created from using the fields from the parsed JSON to arraylist

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
        // Parsing message JSON
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
    // This method takes in the incoming MqttMessage and parses it, creating a new ConfirmedBooking object with filtered
    // fields
    /*public ConfirmedBooking makeConfirmedBooking(MqttMessage message) throws Exception {
        // Parsing message JSON
        JSONParser jsonParser = new JSONParser();
        Object jsonObject = jsonParser.parse(message.toString());
        JSONObject parser = (JSONObject) jsonObject;

        long userid = (Long) parser.get("userid");
        long requestid = (Long) parser.get("requestid");
        String time = (String) parser.get("time");

        // Creating a booking object using the fields from the parsed JSON
        ConfirmedBooking newBooking = new ConfirmedBooking(userid, requestid, time);
        return newBooking;
    }*/
}
