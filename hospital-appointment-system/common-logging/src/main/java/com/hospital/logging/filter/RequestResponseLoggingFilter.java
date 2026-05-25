package com.hospital.logging.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.MDC;

import java.io.IOException;

@Component
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();

        // Skip actuator endpoints for logging
        if (uri.startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        long startTime = System.currentTimeMillis();
        
        MDC.put("httpMethod", request.getMethod());
        MDC.put("uri", uri);
        
        log.info("Started processing request");

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            MDC.put("status", String.valueOf(response.getStatus()));
            MDC.put("executionTimeMs", String.valueOf(duration));
            log.info("Finished processing request");
            
            // Cleanup MDC to prevent memory leaks in thread pools, 
            // though CorrelationIdFilter will clear everything, doing it here is safe.
            MDC.remove("httpMethod");
            MDC.remove("uri");
            MDC.remove("status");
            MDC.remove("executionTimeMs");
        }
    }
}
