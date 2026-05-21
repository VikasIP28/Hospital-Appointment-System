package com.hospital.appointment.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * Resilience4j monitoring configuration.
 *
 * While the actual circuit breaker, retry, and time limiter instances
 * are configured via application.yml, this class registers event
 * listeners to log state transitions and metrics for observability.
 *
 * In production, these logs would be replaced or supplemented by
 * metrics exported to Prometheus/Grafana.
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class Resilience4jConfig {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;

    /**
     * Registers event listeners on all circuit breakers and retries
     * after the Spring context initializes.
     *
     * Logs state transitions (CLOSED -> OPEN, etc.) and retry attempts
     * for debugging and operational monitoring.
     */
    @PostConstruct
    public void registerEventListeners() {
        // Register listeners for all circuit breaker instances
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(circuitBreaker -> {
            String cbName = circuitBreaker.getName();

            circuitBreaker.getEventPublisher()
                    .onStateTransition(event ->
                            log.info("CircuitBreaker '{}' state transition: {} -> {}",
                                    cbName,
                                    event.getStateTransition().getFromState(),
                                    event.getStateTransition().getToState()))
                    .onFailureRateExceeded(event ->
                            log.warn("CircuitBreaker '{}' failure rate exceeded: {}%",
                                    cbName, event.getFailureRate()))
                    .onCallNotPermitted(event ->
                            log.warn("CircuitBreaker '{}' rejected call (circuit is OPEN)",
                                    cbName))
                    .onError(event ->
                            log.debug("CircuitBreaker '{}' recorded error: {}",
                                    cbName, event.getThrowable().getMessage()));
        });

        // Register listeners for all retry instances
        retryRegistry.getAllRetries().forEach(retry -> {
            String retryName = retry.getName();

            retry.getEventPublisher()
                    .onRetry(event ->
                            log.info("Retry '{}' attempt #{}: waiting {}ms before next attempt",
                                    retryName,
                                    event.getNumberOfRetryAttempts(),
                                    event.getWaitInterval().toMillis()))
                    .onSuccess(event ->
                            log.info("Retry '{}' succeeded after {} attempt(s)",
                                    retryName, event.getNumberOfRetryAttempts()))
                    .onError(event ->
                            log.error("Retry '{}' failed after {} attempt(s): {}",
                                    retryName,
                                    event.getNumberOfRetryAttempts(),
                                    event.getLastThrowable().getMessage()));
        });

        log.info("Resilience4j event listeners registered for {} circuit breaker(s) and {} retry instance(s)",
                circuitBreakerRegistry.getAllCircuitBreakers().size(),
                retryRegistry.getAllRetries().size());
    }
}
