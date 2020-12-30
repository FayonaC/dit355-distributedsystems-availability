import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.eclipse.paho.client.mqttv3.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Communicator handles the publishing and subscribing for the Availability component.
 * Communicator is based on the circuit-breaker-example: https://git.chalmers.se/dobslaw/circuit-breaker-example
 */
public class Communicator implements MqttCallback {
    private final static ExecutorService THREAD_POOL = Executors.newSingleThreadExecutor();

    private final static Duration OPEN_WAIT_TIME_IN = Duration.ofSeconds(3);

    private final static Filter SERVICE = new Filter();

    private Supplier<ReceivedBooking> service;

    private final CircuitBreaker circuitBreaker;

    private final IMqttClient middleware;

    public Communicator(String brokerstring, String userid) throws MqttException {
        middleware = new MqttClient(brokerstring, userid);
        middleware.connect();
        middleware.setCallback(this);

        int LOW_FAILURE_THRESHOLD = 10;
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(LOW_FAILURE_THRESHOLD)
                .slidingWindow(20, 5, CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .waitDurationInOpenState(OPEN_WAIT_TIME_IN).build();

        circuitBreaker = CircuitBreaker.of("availability", config);
        service = CircuitBreaker.decorateSupplier(circuitBreaker, SERVICE);
    }

    public static void main(String[] args) {
        try {
            Communicator c = new Communicator("tcp://localhost:1883", "bookings-filter");
            c.subscribeToMessages("BookingRegistry");
            c.subscribeToMessages("BookingRequest");
            c.subscribeToMessages("Dentists");
            c.subscribeToMessages("AvailabilityRequest");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Subscribes to messages in a thread pool.
     * @param fromTopic
     */
    private void subscribeToMessages(String fromTopic) {
        THREAD_POOL.submit(() -> {
            try {
                middleware.subscribe(fromTopic);
            } catch (MqttSecurityException e) {
                e.printStackTrace();
            } catch (MqttException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Method is called when a subscription receives a message.
     * It does different things depending on the topic received.
     * @param topic
     * @param incoming
     * @throws Exception
     */
    @Override
    public void messageArrived(String topic, MqttMessage incoming) throws Exception {
        try {
            ReceivedBooking receivedBooking = null;
            ArrayList<Schedule> schedules = new ArrayList<>();

            switch (topic) {
                case "BookingRequest":
                    System.out.println("State before makeReceivedBooking:" + circuitBreaker.getState());
                    SERVICE.makeReceivedBooking(incoming);
                    receivedBooking = service.get(); // Could be a successful or failed booking

                    if (!receivedBooking.getTime().equals("none")) {
                        dump("SuccessfulBooking", receivedBooking.toString());
                    } else {
                        dump("BookingResponse", receivedBooking.getBookingResponse()); // Time should be none
                    }

                    System.out.println("State after makeReceivedBooking:" + circuitBreaker.getState());
                    break;
                case "BookingRegistry":
                    SERVICE.makeBookingArray(incoming);
                    System.out.println("We have received an updated booking registry.");
                    break;
                case "Dentists":
                    SERVICE.makeDentistArray(incoming);
                    System.out.println("We have received an updated dentist registry.");
                    break;
                case "AvailabilityRequest":
                    SERVICE.setSelectedDate(incoming);
                    System.out.println("We have received a selected date.");
                    schedules = SERVICE.getAvailability();
                    dump("free-slots", "{ \"schedules\": " + schedules.toString() + "}");
                    break;
                default:
                    System.out.println("Topic not found");
            }

            System.out.println("state: " + circuitBreaker.getState());
        } catch (RuntimeException e) {
            System.err.println("state: " + circuitBreaker.getState());
            if (circuitBreaker.getState().equals(CircuitBreaker.State.OPEN)) {
                System.err.println("Request rejected! (buffer request or tell requester!)");
            } else {
                System.err.println("Oh boy, the service failed! (buffer request or tell requester!)");
            }
        }
    }

    /**
     * When a connection is lost, try to reconnect.
     * @param throwable
     */
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
                e.printStackTrace();
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

    /**
     * TODO: Look into this method
     * @param iMqttDeliveryToken
     */
    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
    }

    /**
     * Method to publish to the MQTT broker
     * @param sinkTopic
     * @param msg
     * @throws MqttException
     */
    public void dump(String sinkTopic, String msg) throws MqttException {
        MqttMessage outgoing = new MqttMessage();
        outgoing.setQos(1);
        outgoing.setPayload(msg.getBytes());
        System.out.println(outgoing);
        middleware.publish(sinkTopic, outgoing);
    }
}
