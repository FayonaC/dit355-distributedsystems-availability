import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
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
    //The circuit breaker settings below may need to be changed depending on your computer processing speed
    private final static Duration OPEN_WAIT_TIME = Duration.ofSeconds(2);
    private final static long FAILURE_RATE_THRESHOLD = 10; //Failures over 10% will open the circuit breaker
    private final static int SLIDING_WINDOW_SIZE = 5; //Evaluates per 5 calls
    private final static int MINIMUM_CALLS = 3; // The minimum number of calls that need to be executed per sliding window
    private final static int PERMITTED_CALLS = 5; // The number of calls permitted when the circuit breaker is half open
    private final static Duration SLOW_CALL_DURATION_THRESHOLD = Duration.ofNanos(800000); // Calls with waiting time above this are considered a failure
    private final static long SLOW_CALL_RATE_THRESHOLD = 1; // The circuit breaker will open is over 1% of calls are slow

    private final static ExecutorService THREAD_POOL = Executors.newSingleThreadExecutor();

    private final static Filter SERVICE = new Filter();

    private Supplier<ReceivedBooking> service;

    private final CircuitBreaker circuitBreaker;

    private final IMqttClient middleware;

    public Communicator(String brokerstring, String userid) throws MqttException {
        middleware = new MqttClient(brokerstring, userid);
        middleware.connect();
        middleware.setCallback(this);

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(FAILURE_RATE_THRESHOLD)
                .slidingWindow(SLIDING_WINDOW_SIZE, MINIMUM_CALLS, CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .waitDurationInOpenState(OPEN_WAIT_TIME)
                .permittedNumberOfCallsInHalfOpenState(PERMITTED_CALLS)
                .slowCallDurationThreshold(SLOW_CALL_DURATION_THRESHOLD)
                .slowCallRateThreshold(SLOW_CALL_RATE_THRESHOLD)
                .build();

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
        } catch(Throwable throwable) {
            throwable.printStackTrace();
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
                    SERVICE.makeReceivedBooking(incoming);
                    receivedBooking = service.get(); // Could be a successful or failed booking
                    System.out.println("State after receivedBooking:" + circuitBreaker.getState());
                    if (!receivedBooking.getTime().equals("none")) {
                        dump("SuccessfulBooking", receivedBooking.toString());
                    } else {
                        dump("BookingResponse", receivedBooking.getBookingResponse()); // Time should be none
                    }
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

            System.out.println("State of circuit breaker: " + circuitBreaker.getState());
        } catch (RuntimeException e) {
            System.err.println("State when there is a runtime exception: " + circuitBreaker.getState());
            if (circuitBreaker.getState().equals(CircuitBreaker.State.OPEN)) {
                System.err.println("Request rejected!");
            } else {
                System.err.println("Service has failed!");
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
                Thread.sleep(8000); //original number is 3000
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
        System.out.println("Message to be published: " + outgoing);
        middleware.publish(sinkTopic, outgoing);
        System.out.println("Message published!");
    }
}
