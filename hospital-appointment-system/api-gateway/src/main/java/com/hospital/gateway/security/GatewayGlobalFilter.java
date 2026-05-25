package com.hospital.gateway.security;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@Component
public class GatewayGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(GatewayGlobalFilter.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        // 1. Handle Correlation ID
        String correlationId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }
        
        // Add it to downstream request
        ServerHttpRequest modifiedRequest = request.mutate()
                .header(CORRELATION_ID_HEADER, correlationId)
                .build();
                
        ServerWebExchange modifiedExchange = exchange.mutate()
                .request(modifiedRequest)
                .build();

        long startTime = System.currentTimeMillis();
        String uri = request.getURI().getPath();
        String method = request.getMethod().name();

        // 2. Log incoming request (simulating MDC with structured logs)
        log.info("Request Started: method={}, uri={}, correlationId={}", method, uri, correlationId);

        // 3. Process the chain
        final String finalCorrelationId = correlationId;
        return chain.filter(modifiedExchange).then(Mono.fromRunnable(() -> {
            // 4. Log outgoing response
            long duration = System.currentTimeMillis() - startTime;
            int status = modifiedExchange.getResponse().getStatusCode() != null 
                         ? modifiedExchange.getResponse().getStatusCode().value() 
                         : 200;
                         
            modifiedExchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, finalCorrelationId);
            
            log.info("Request Finished: method={}, uri={}, status={}, executionTimeMs={}, correlationId={}", 
                     method, uri, status, duration, finalCorrelationId);
        }));
    }

    @Override
    public int getOrder() {
        return -1; // High precedence
    }
}
