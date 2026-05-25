# Enterprise Centralized Logging Architecture

This document explains the centralized logging infrastructure implemented in the Hospital Appointment System and provides a step-by-step guide to testing the APIs and tracing logs across microservices.

## 🏗️ What Was Implemented

We introduced a true Enterprise Centralized Logging strategy to ensure that every request across the microservices ecosystem can be easily tracked, debugged, and monitored. 

### 1. The `common-logging` Module
Instead of duplicating logging configuration in every microservice, a shared library `common-logging` was created. All Spring Web MVC microservices (`auth`, `appointment`, `doctor`, `notification`) import this library to automatically inherit the logging standards.

### 2. Structured JSON Logging (Logback + SLF4J)
All console and file outputs have been converted from plain text to **Structured JSON** using the `logstash-logback-encoder`. This makes the logs machine-readable, ready to be ingested by centralized log aggregators like Elasticsearch, Datadog, or Splunk.

### 3. Distributed Tracing via Correlation IDs
Every incoming HTTP request is assigned a unique `X-Correlation-Id` using the **MDC (Mapped Diagnostic Context)**.
- **API Gateway**: Generates the `X-Correlation-Id` for incoming external requests.
- **Microservices**: Read the ID from the HTTP headers and add it to their MDC context, ensuring all local logs for that request share the same ID.
- **Feign Interceptor**: A custom `FeignCorrelationInterceptor` ensures that when one microservice calls another (e.g., `appointment-service` calling `doctor-service`), the Correlation ID is injected into the outgoing request headers.

### 4. AOP Execution Time Logging
An Aspect-Oriented Programming (AOP) interceptor `ExecutionTimeLoggerAspect` was added. It automatically calculates and logs the `executionTimeMs` of all requests and critical service methods.

### 5. Centralized Global Exception Handling
A single, unified `@RestControllerAdvice` resides in the `common-logging` module. It catches all exceptions (`MethodArgumentNotValidException`, `ResponseStatusException`, etc.), logs them at the appropriate level (`WARN` vs `ERROR`), and returns a standardized JSON `ErrorResponse` that includes the Correlation ID.

### 6. WebFlux API Gateway Migration
Because the API Gateway must handle high concurrency, it was migrated from a blocking Tomcat server to a non-blocking **Spring Cloud Gateway (WebFlux)**. Since traditional ThreadLocal MDC doesn't work in WebFlux, the gateway uses custom reactive `GlobalFilter`s to generate correlation IDs and log requests/responses efficiently.

---

## 📁 Where to Find the Logs

While logs are printed to the console in JSON format, they are also saved to physical files via a Rolling File Appender.

Check the `logs/` directory inside each microservice's root folder:
- `logs/[service-name].log` -> Contains all `INFO`, `DEBUG`, `WARN`, and `ERROR` logs.
- `logs/[service-name]-error.log` -> Contains **only** `ERROR` level logs for quick debugging.

Log files are rotated daily and kept for 30 days.

---

## 🧪 How to Test and Trace Requests

To see the centralized logging and correlation IDs in action, follow these steps using your provided Postman collection.

### Prerequisites
Ensure MongoDB, Zookeeper, and Kafka are running, and start all 5 microservices.

### Scenario 1: Single Service Request (User Registration)
1. Open Postman and run **1. Authentication -> Register Patient**.
2. Look at the console for the **API Gateway** and **Auth Service**.
3. **What to look for**:
   - In the API Gateway console, you will see a `Request Started` and `Request Finished` JSON log. Notice the `correlationId` field (e.g., `abc123...`).
   - In the Auth Service console, you will see the exact same `correlationId` attached to the log: `Processing registration request for email...`.

### Scenario 2: Cross-Service Request via Feign (Booking an Appointment)
This is where Distributed Tracing shines. Booking an appointment routes through the Gateway, hits the Appointment Service, which then makes a Feign HTTP call to the Doctor Service.

1. In Postman, ensure you are logged in as a Patient.
2. Run **3. Appointments -> Book Appointment**.
3. Open the logs for **API Gateway**, **Appointment Service**, and **Doctor Service**.
4. **What to look for**:
   - Copy the `correlationId` from the API Gateway's JSON log.
   - Search for that exact ID in the **Appointment Service** logs. You will see it tracking the incoming request.
   - Search for that exact ID in the **Doctor Service** logs. You will see it there as well, proving that the `FeignCorrelationInterceptor` successfully passed the header from the Appointment Service to the Doctor Service!
   - Look for the `executionTimeMs` field in the final `Finished processing request` log to see how long the entire chain took.

### Scenario 3: Event-Driven Tracing via Kafka (Notification)
When an appointment is booked, an event is sent to Kafka, which the Notification Service picks up.

1. Still looking at the Book Appointment request, copy the `correlationId`.
2. *(Note: While HTTP headers pass correlation IDs automatically via Feign, Kafka requires manual header propagation. Currently, the Kafka events log the `appointmentId` which serves as a domain-specific correlation key).*
3. Look at the **Notification Service** logs. You will see structured JSON logs detailing the email sending process (`"message": "Sending email to: ..."`).

### Scenario 4: Error Handling & Stack Traces (Triggering a Circuit Breaker)
1. **Stop the Doctor Service** terminal (Ctrl+C).
2. In Postman, run **3. Appointments -> Book Appointment** again.
3. Look at the **Appointment Service** logs.
4. **What to look for**:
   - You will see standard `INFO` logs as it attempts the request.
   - You will then see `WARN` or `ERROR` logs generated by Resilience4j as the retry mechanism fails and the Fallback method is triggered.
   - Look at the `logs/appointment-service-error.log` file. You will see the exception stack traces neatly formatted in JSON, tied directly to the `correlationId` of the failed request.
   - The Postman response will still return a `201 Created`, but the doctor's name will show as `"Unknown Doctor (Service Unavailable)"`, demonstrating graceful degradation while maintaining perfect log visibility.
