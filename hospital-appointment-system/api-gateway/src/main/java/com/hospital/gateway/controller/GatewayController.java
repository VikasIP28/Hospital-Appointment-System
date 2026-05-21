package com.hospital.gateway.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * Gateway Controller - Core Request Router
 * ==========================================
 * This is the heart of the API Gateway. It intercepts all incoming requests
 * under /api/** and proxies them to the appropriate downstream microservice.
 *
 * Route mappings:
 * ┌──────────────────────────────────────┬───────────────────────────────────────────────┐
 * │ Gateway Path                         │ Downstream Target                             │
 * ├──────────────────────────────────────┼───────────────────────────────────────────────┤
 * │ /api/auth/**                         │ auth-service:8084/auth/**                     │
 * │ /api/appointments/**                 │ appointment-service:8081/appointments/**       │
 * │ /api/doctors/**                      │ doctor-service:8082/doctors/**                 │
 * │ /api/notifications/**                │ notification-service:8083/notifications/**     │
 * │ /api/admin/system/**                 │ appointment-service:8081/admin/system/**       │
 * │ /api/admin/analytics/appointments    │ appointment-service:8081/admin/analytics/...   │
 * │ /api/admin/analytics/doctors         │ doctor-service:8082/admin/analytics/doctors    │
 * │ /api/admin/notifications/**          │ notification-service:8083/admin/notifications  │
 * │ /api/admin/kafka/**                  │ appointment-service:8081/admin/kafka/**        │
 * └──────────────────────────────────────┴───────────────────────────────────────────────┘
 *
 * Key behaviors:
 *   - Forwards the Authorization header to downstream services
 *   - Forwards request body for POST/PUT/PATCH methods
 *   - Preserves query parameters from the original request
 *   - Returns 503 if the downstream service is unreachable
 *   - Forwards HTTP error responses (4xx, 5xx) from downstream as-is
 *   - Logs all proxied requests for debugging and monitoring
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class GatewayController {

    private final RestTemplate restTemplate;

    // --- Downstream service URLs loaded from application.yml ---

    @Value("${spring.cloud.gateway.services.auth-service}")
    private String authServiceUrl;

    @Value("${spring.cloud.gateway.services.appointment-service}")
    private String appointmentServiceUrl;

    @Value("${spring.cloud.gateway.services.doctor-service}")
    private String doctorServiceUrl;

    @Value("${spring.cloud.gateway.services.notification-service}")
    private String notificationServiceUrl;

    // =========================================================================
    // Route Handler: Auth Service
    // =========================================================================

    /**
     * Routes all authentication requests to the Auth Service.
     * Handles: login, registration, token refresh, profile operations.
     *
     * Gateway path: /api/auth/**
     * Target path:  auth-service:8084/auth/**
     *
     * @param request the incoming HTTP request
     * @param body    the request body (for POST/PUT requests, null for GET/DELETE)
     * @return the proxied response from the Auth Service
     */
    @RequestMapping("/auth/**")
    public ResponseEntity<String> routeToAuth(HttpServletRequest request,
                                               @RequestBody(required = false) String body) {
        // Strip /api/auth from the URI and map to /auth on the downstream service
        String path = extractDownstreamPath(request.getRequestURI(), "/api/auth", "/auth");
        String queryString = request.getQueryString();
        String targetUrl = buildTargetUrl(authServiceUrl, path, queryString);

        log.debug("Routing auth request: {} {} -> {}", request.getMethod(), request.getRequestURI(), targetUrl);

        return proxyRequest(targetUrl, HttpMethod.valueOf(request.getMethod()), request, body);
    }

    // =========================================================================
    // Route Handler: Appointment Service
    // =========================================================================

    /**
     * Routes all appointment requests to the Appointment Service.
     * Handles: CRUD operations on appointments, booking, cancellation, etc.
     *
     * Gateway path: /api/appointments/**
     * Target path:  appointment-service:8081/appointments/**
     *
     * @param request the incoming HTTP request
     * @param body    the request body (for POST/PUT requests)
     * @return the proxied response from the Appointment Service
     */
    @RequestMapping("/appointments/**")
    public ResponseEntity<String> routeToAppointments(HttpServletRequest request,
                                                       @RequestBody(required = false) String body) {
        // Strip /api/appointments and map to /appointments on the downstream service
        String path = extractDownstreamPath(request.getRequestURI(), "/api/appointments", "/appointments");
        String queryString = request.getQueryString();
        String targetUrl = buildTargetUrl(appointmentServiceUrl, path, queryString);

        log.debug("Routing appointment request: {} {} -> {}", request.getMethod(), request.getRequestURI(), targetUrl);

        return proxyRequest(targetUrl, HttpMethod.valueOf(request.getMethod()), request, body);
    }

    // =========================================================================
    // Route Handler: Doctor Service
    // =========================================================================

    /**
     * Routes all doctor-related requests to the Doctor Service.
     * Handles: Doctor profiles, availability, schedule management, etc.
     *
     * Gateway path: /api/doctors/**
     * Target path:  doctor-service:8082/doctors/**
     *
     * @param request the incoming HTTP request
     * @param body    the request body (for POST/PUT requests)
     * @return the proxied response from the Doctor Service
     */
    @RequestMapping("/doctors/**")
    public ResponseEntity<String> routeToDoctors(HttpServletRequest request,
                                                  @RequestBody(required = false) String body) {
        // Strip /api/doctors and map to /doctors on the downstream service
        String path = extractDownstreamPath(request.getRequestURI(), "/api/doctors", "/doctors");
        String queryString = request.getQueryString();
        String targetUrl = buildTargetUrl(doctorServiceUrl, path, queryString);

        log.debug("Routing doctor request: {} {} -> {}", request.getMethod(), request.getRequestURI(), targetUrl);

        return proxyRequest(targetUrl, HttpMethod.valueOf(request.getMethod()), request, body);
    }

    // =========================================================================
    // Route Handler: Notification Service
    // =========================================================================

    /**
     * Routes all notification requests to the Notification Service.
     * Handles: Notification preferences, history, etc.
     *
     * Gateway path: /api/notifications/**
     * Target path:  notification-service:8083/notifications/**
     *
     * @param request the incoming HTTP request
     * @param body    the request body (for POST/PUT requests)
     * @return the proxied response from the Notification Service
     */
    @RequestMapping("/notifications/**")
    public ResponseEntity<String> routeToNotifications(HttpServletRequest request,
                                                        @RequestBody(required = false) String body) {
        // Strip /api/notifications and map to /notifications on the downstream service
        String path = extractDownstreamPath(request.getRequestURI(), "/api/notifications", "/notifications");
        String queryString = request.getQueryString();
        String targetUrl = buildTargetUrl(notificationServiceUrl, path, queryString);

        log.debug("Routing notification request: {} {} -> {}", request.getMethod(), request.getRequestURI(), targetUrl);

        return proxyRequest(targetUrl, HttpMethod.valueOf(request.getMethod()), request, body);
    }

    // =========================================================================
    // Route Handler: Admin - System (Appointment Service)
    // =========================================================================

    /**
     * Routes admin system requests to the Appointment Service.
     * Handles: System health, diagnostics, etc.
     *
     * Gateway path: /api/admin/system/**
     * Target path:  appointment-service:8081/admin/system/**
     *
     * @param request the incoming HTTP request
     * @param body    the request body
     * @return the proxied response
     */
    @RequestMapping("/admin/system/**")
    public ResponseEntity<String> routeToAdminSystem(HttpServletRequest request,
                                                      @RequestBody(required = false) String body) {
        String path = extractDownstreamPath(request.getRequestURI(), "/api/admin/system", "/admin/system");
        String queryString = request.getQueryString();
        String targetUrl = buildTargetUrl(appointmentServiceUrl, path, queryString);

        log.debug("Routing admin system request: {} {} -> {}", request.getMethod(), request.getRequestURI(), targetUrl);

        return proxyRequest(targetUrl, HttpMethod.valueOf(request.getMethod()), request, body);
    }

    // =========================================================================
    // Route Handler: Admin - Appointment Analytics (Appointment Service)
    // =========================================================================

    /**
     * Routes appointment analytics requests to the Appointment Service.
     *
     * Gateway path: /api/admin/analytics/appointments/**
     * Target path:  appointment-service:8081/admin/analytics/appointments/**
     *
     * @param request the incoming HTTP request
     * @param body    the request body
     * @return the proxied response
     */
    @RequestMapping("/admin/analytics/appointments/**")
    public ResponseEntity<String> routeToAppointmentAnalytics(HttpServletRequest request,
                                                               @RequestBody(required = false) String body) {
        String path = extractDownstreamPath(request.getRequestURI(), "/api/admin/analytics/appointments", "/admin/analytics/appointments");
        String queryString = request.getQueryString();
        String targetUrl = buildTargetUrl(appointmentServiceUrl, path, queryString);

        log.debug("Routing appointment analytics request: {} {} -> {}", request.getMethod(), request.getRequestURI(), targetUrl);

        return proxyRequest(targetUrl, HttpMethod.valueOf(request.getMethod()), request, body);
    }

    // =========================================================================
    // Route Handler: Admin - Doctor Analytics (Doctor Service)
    // =========================================================================

    /**
     * Routes doctor analytics requests to the Doctor Service.
     *
     * Gateway path: /api/admin/analytics/doctors/**
     * Target path:  doctor-service:8082/admin/analytics/doctors/**
     *
     * @param request the incoming HTTP request
     * @param body    the request body
     * @return the proxied response
     */
    @RequestMapping("/admin/analytics/doctors/**")
    public ResponseEntity<String> routeToDoctorAnalytics(HttpServletRequest request,
                                                          @RequestBody(required = false) String body) {
        String path = extractDownstreamPath(request.getRequestURI(), "/api/admin/analytics/doctors", "/admin/analytics/doctors");
        String queryString = request.getQueryString();
        String targetUrl = buildTargetUrl(doctorServiceUrl, path, queryString);

        log.debug("Routing doctor analytics request: {} {} -> {}", request.getMethod(), request.getRequestURI(), targetUrl);

        return proxyRequest(targetUrl, HttpMethod.valueOf(request.getMethod()), request, body);
    }

    // =========================================================================
    // Route Handler: Admin - Notifications (Notification Service)
    // =========================================================================

    /**
     * Routes admin notification requests to the Notification Service.
     *
     * Gateway path: /api/admin/notifications/**
     * Target path:  notification-service:8083/admin/notifications/**
     *
     * @param request the incoming HTTP request
     * @param body    the request body
     * @return the proxied response
     */
    @RequestMapping("/admin/notifications/**")
    public ResponseEntity<String> routeToAdminNotifications(HttpServletRequest request,
                                                             @RequestBody(required = false) String body) {
        String path = extractDownstreamPath(request.getRequestURI(), "/api/admin/notifications", "/admin/notifications");
        String queryString = request.getQueryString();
        String targetUrl = buildTargetUrl(notificationServiceUrl, path, queryString);

        log.debug("Routing admin notification request: {} {} -> {}", request.getMethod(), request.getRequestURI(), targetUrl);

        return proxyRequest(targetUrl, HttpMethod.valueOf(request.getMethod()), request, body);
    }

    // =========================================================================
    // Route Handler: Admin - Kafka (Appointment Service)
    // =========================================================================

    /**
     * Routes admin Kafka management requests to the Appointment Service.
     * Handles: Kafka topic inspection, dead letter queue management, etc.
     *
     * Gateway path: /api/admin/kafka/**
     * Target path:  appointment-service:8081/admin/kafka/**
     *
     * @param request the incoming HTTP request
     * @param body    the request body
     * @return the proxied response
     */
    @RequestMapping("/admin/kafka/**")
    public ResponseEntity<String> routeToAdminKafka(HttpServletRequest request,
                                                     @RequestBody(required = false) String body) {
        String path = extractDownstreamPath(request.getRequestURI(), "/api/admin/kafka", "/admin/kafka");
        String queryString = request.getQueryString();
        String targetUrl = buildTargetUrl(appointmentServiceUrl, path, queryString);

        log.debug("Routing admin Kafka request: {} {} -> {}", request.getMethod(), request.getRequestURI(), targetUrl);

        return proxyRequest(targetUrl, HttpMethod.valueOf(request.getMethod()), request, body);
    }

    // =========================================================================
    // Private Helper Methods
    // =========================================================================

    /**
     * Proxies an HTTP request to a downstream service.
     *
     * This is the central method that handles ALL request forwarding:
     * 1. Copies the Authorization header from the original request
     * 2. Sets Content-Type to application/json
     * 3. Sends the request via RestTemplate
     * 4. Returns the downstream response (including status code and body)
     * 5. Handles errors gracefully (returns 503 for unreachable services)
     *
     * @param targetUrl the full URL of the downstream service endpoint
     * @param method    the HTTP method (GET, POST, PUT, DELETE, etc.)
     * @param request   the original HTTP request (used to extract headers)
     * @param body      the request body (may be null for GET/DELETE)
     * @return the response from the downstream service, or a 503 error response
     */
    private ResponseEntity<String> proxyRequest(String targetUrl, HttpMethod method,
                                                 HttpServletRequest request, @Nullable String body) {
        // Build headers to forward to the downstream service
        HttpHeaders headers = new HttpHeaders();

        // Forward the Authorization header so downstream services can validate the JWT
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null) {
            headers.set("Authorization", authHeader);
        }

        // Set Content-Type to JSON (default for all our microservices)
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Create the HTTP entity with body and headers
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        try {
            // Forward the request to the downstream service
            log.info("Proxying {} {} -> {}", method, request.getRequestURI(), targetUrl);

            ResponseEntity<String> response = restTemplate.exchange(
                    targetUrl, method, entity, String.class
            );

            log.debug("Proxy response status: {} for {}", response.getStatusCode(), targetUrl);
            return response;

        } catch (HttpClientErrorException e) {
            // 4xx errors from downstream service - forward them as-is
            log.warn("Client error from downstream service {}: {} - {}",
                    targetUrl, e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity
                    .status(e.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(e.getResponseBodyAsString());

        } catch (HttpServerErrorException e) {
            // 5xx errors from downstream service - forward them as-is
            log.error("Server error from downstream service {}: {} - {}",
                    targetUrl, e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity
                    .status(e.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(e.getResponseBodyAsString());

        } catch (ResourceAccessException e) {
            // Connection refused, timeout, etc. - downstream service is unreachable
            log.error("Downstream service unavailable at {}: {}", targetUrl, e.getMessage());
            return ResponseEntity
                    .status(503)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\": \"Service unavailable\", \"message\": \"The downstream service at "
                            + targetUrl + " is not reachable. Please try again later.\"}");

        } catch (Exception e) {
            // Catch-all for unexpected errors
            log.error("Unexpected error proxying request to {}: {}", targetUrl, e.getMessage(), e);
            return ResponseEntity
                    .status(503)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\": \"Service unavailable\", \"message\": \"An unexpected error occurred while routing the request.\"}");
        }
    }

    /**
     * Extracts the downstream path from the original request URI.
     * Strips the gateway prefix and prepends the downstream prefix.
     *
     * Example:
     *   URI:              /api/auth/login
     *   gatewayPrefix:    /api/auth
     *   downstreamPrefix: /auth
     *   Result:           /auth/login
     *
     * @param requestUri      the full request URI from the client
     * @param gatewayPrefix   the prefix to strip (e.g., "/api/auth")
     * @param downstreamPrefix the prefix to prepend (e.g., "/auth")
     * @return the transformed path for the downstream service
     */
    private String extractDownstreamPath(String requestUri, String gatewayPrefix, String downstreamPrefix) {
        // Remove the gateway prefix from the URI
        String remainingPath = requestUri.substring(gatewayPrefix.length());

        // If there's no remaining path, just return the downstream prefix
        if (remainingPath.isEmpty()) {
            return downstreamPrefix;
        }

        // Combine downstream prefix with the remaining path
        return downstreamPrefix + remainingPath;
    }

    /**
     * Builds the full target URL for the downstream service,
     * including the base URL, path, and query string.
     *
     * @param baseUrl     the base URL of the downstream service (e.g., "http://localhost:8081")
     * @param path        the path to append (e.g., "/appointments/123")
     * @param queryString the query string from the original request (may be null)
     * @return the complete target URL
     */
    private String buildTargetUrl(String baseUrl, String path, @Nullable String queryString) {
        StringBuilder url = new StringBuilder(baseUrl);
        url.append(path);

        // Append query parameters if present (e.g., ?page=0&size=10)
        if (queryString != null && !queryString.isEmpty()) {
            url.append("?").append(queryString);
        }

        return url.toString();
    }
}
