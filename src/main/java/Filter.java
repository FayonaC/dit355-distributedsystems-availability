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
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Filter implements MqttCallback {

    private final static ExecutorService THREAD_POOL = Executors.newSingleThreadExecutor();

    private final IMqttClient middleware;

    public Filter(String userid, String broker) throws MqttException {
        middleware = new MqttClient(broker, userid);
        middleware.connect();
        middleware.setCallback(this);
    }

    public static void main(String[] args) throws MqttException, InterruptedException {
        Filter s = new Filter("test-filter", "tcp://localhost:1883");
        s.subscribeToMessages("BookingResponse");
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
        ConfirmedBooking confirmedBooking = makeConfirmedBooking(incoming); // We take in the booking request and
        // create a new confirmed booking item with certain fields
        String sinkTopic = extractTopic(incoming);

        dump(confirmedBooking, sinkTopic);
    }


    private String extractTopic(MqttMessage incoming) {
        return incoming.toString();
    }

    private void dump(ConfirmedBooking confirmedBooking, String sinkTopic) throws MqttPersistenceException, MqttException {
        MqttMessage outgoing = new MqttMessage();
        outgoing.setPayload(confirmedBooking.toString().getBytes());
        middleware.publish(sinkTopic, outgoing);
    }

    // This method takes in the incoming MqttMessage and parses it, creating a new ConfirmedBooking object with filtered
    // fields
    public ConfirmedBooking makeConfirmedBooking(MqttMessage message) throws Exception {
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
    }
}
