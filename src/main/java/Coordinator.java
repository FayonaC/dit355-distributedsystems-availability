import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;

import java.time.Duration;

public class Coordinator {
    private static CircuitBreaker circuitBreaker;

    public static void main(String[] args) {

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)	// percentage of failed things, open circuit if half of the service calls fail
                .slidingWindow(20, 10, CircuitBreakerConfig.SlidingWindowType.TIME_BASED) // check last 10 seconds for failure rate (only if 10++ service calls)
                // .slowCallDurationThreshold(Duration.ofSeconds(1)) // calls with waiting time above 2 seconds are considered a failure
                // .slowCallRateThreshold(70)	// if half the service calls are too slow, open!
                // .permittedNumberOfCallsInHalfOpenState(5)
                .waitDurationInOpenState(Duration.ofSeconds(5))
                .build();

        circuitBreaker = CircuitBreaker.of("availability", config);

        // the service (business logic)
        // Filter s = new Filter("bookings-filter", "tcp://localhost:1883");

        try {
            Filter s = new Filter("bookings-filter", "tcp://localhost:1883");
            CircuitBreaker.decorateFunction(circuitBreaker, s);
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}
