package com.hospital.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Gateway Routing Configuration
 * ===============================
 * Provides the RestTemplate bean used by the GatewayController
 * to forward (proxy) HTTP requests to downstream microservices.
 *
 * The RestTemplate is configured with reasonable timeouts:
 *   - Connect timeout: 5 seconds (time to establish TCP connection)
 *   - Read timeout: 30 seconds (time to wait for response data)
 *
 * These timeouts prevent the gateway from hanging indefinitely
 * when a downstream service is slow or unresponsive.
 */
@Configuration
@Slf4j
public class GatewayRoutingConfig {

    /**
     * Creates a RestTemplate bean with configured timeouts.
     * Uses SimpleClientHttpRequestFactory for timeout configuration.
     *
     * @return configured RestTemplate instance
     */
    @Bean
    public RestTemplate restTemplate() {
        log.info("Initializing RestTemplate for gateway routing with connect timeout: 5s, read timeout: 30s");

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);  // 5 seconds
        factory.setReadTimeout(30000);    // 30 seconds

        return new RestTemplate(factory);
    }
}
