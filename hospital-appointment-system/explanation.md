# 🏥 Hospital Appointment System — Complete Technical Explanation

> **A deep-dive guide** covering every architectural decision, design pattern, code flow, configuration choice, and enterprise concept used in this project. Designed for learning, interview preparation, and code reviews.

---

## 📑 Table of Contents

1. [Project Overview](#1-project-overview)
2. [Gradle Multi-Module Project Structure](#2-gradle-multi-module-project-structure)
3. [Auth Service — Deep Dive](#3-auth-service--deep-dive)
4. [Doctor Service — Deep Dive](#4-doctor-service--deep-dive)
5. [Appointment Service — Deep Dive](#5-appointment-service--deep-dive)
6. [Notification Service — Deep Dive](#6-notification-service--deep-dive)
7. [API Gateway — Deep Dive](#7-api-gateway--deep-dive)
8. [JWT Authentication — End-to-End](#8-jwt-authentication--end-to-end)
9. [Apache Kafka — Event-Driven Architecture](#9-apache-kafka--event-driven-architecture)
10. [Resilience4j — Fault Tolerance Patterns](#10-resilience4j--fault-tolerance-patterns)
11. [Spring Cloud OpenFeign — Inter-Service Communication](#11-spring-cloud-openfeign--inter-service-communication)
12. [MongoDB — Data Layer](#12-mongodb--data-layer)
13. [Spring Security Configuration — How It Works](#13-spring-security-configuration--how-it-works)
14. [Exception Handling Strategy](#14-exception-handling-strategy)
15. [Lombok — Boilerplate Reduction](#15-lombok--boilerplate-reduction)
16. [application.yml — Configuration Explained](#16-applicationyml--configuration-explained)
17. [End-to-End Request Flows](#17-end-to-end-request-flows)
18. [Design Patterns Used](#18-design-patterns-used)
19. [What Happens When Things Fail?](#19-what-happens-when-things-fail)
20. [Production Considerations](#20-production-considerations)
21. [Key Annotations Reference](#21-key-annotations-reference)

---

## 1. Project Overview

### What Is This System?

A **Hospital Appointment System** where:
- **Patients** register, log in, and book appointments with doctors
- **Doctors** register, create their profiles, and confirm/reject appointments
- **Admins** view analytics, monitor system health, and manage notifications

### Why Microservices?

Instead of one monolithic application, the system is split into **5 independent services**:

| Service | Responsibility | Port |
|---|---|---|
| **auth-service** | User registration, login, JWT token generation | 8084 |
| **doctor-service** | Doctor profiles, availability, specializations | 8082 |
| **appointment-service** | Booking lifecycle, Kafka events, resilience | 8081 |
| **notification-service** | Email notifications via Kafka events | 8083 |
| **api-gateway** | Single entry point, request routing, JWT validation | 8080 |

**Benefits of this split:**
- Each service can be **deployed independently** — a bug in notification-service doesn't crash the entire system
- Each service can be **scaled independently** — if appointment bookings spike, only appointment-service needs more instances
- Teams can **develop in parallel** — one team works on notifications while another works on appointments
- Each service has its **own database** — no tight coupling between data models

---

## 2. Gradle Multi-Module Project Structure

### Why Multi-Module?

Instead of separate Git repositories for each service, we use a **single Gradle multi-module project**. This means:
- All services are in one repository (monorepo)
- Shared configuration (Java version, dependency versions) is defined once in the root
- A single `gradlew build` compiles everything

### Root `settings.gradle`

```groovy
rootProject.name = 'hospital-appointment-system'
include 'auth-service'
include 'doctor-service'
include 'appointment-service'
include 'notification-service'
include 'api-gateway'
```

This tells Gradle: "This project contains 5 sub-projects."

### Root `build.gradle` — The Parent Configuration

```groovy
plugins {
    id 'org.springframework.boot' version '3.2.5' apply false
    id 'io.spring.dependency-management' version '1.1.5' apply false
    id 'java'
}
```

**`apply false`** is critical here. It means "register these plugins but don't apply them to the root project." The root project itself has no source code — it only configures shared settings.

```groovy
ext {
    springCloudVersion = '2023.0.1'
    jjwtVersion = '0.11.5'
    resilience4jVersion = '2.2.0'
}
```

**`ext` block** defines version variables accessible by all sub-projects. This is how we ensure every service uses the same JJWT version — we define it once, and sub-projects reference `${jjwtVersion}`.

```groovy
subprojects {
    apply plugin: 'java'
    apply plugin: 'org.springframework.boot'
    apply plugin: 'io.spring.dependency-management'
    
    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    
    dependencies {
        compileOnly 'org.projectlombok:lombok'
        annotationProcessor 'org.projectlombok:lombok'
        implementation 'org.springframework.boot:spring-boot-starter-actuator'
    }
}
```

**`subprojects` block** applies plugins and dependencies to ALL sub-projects. This avoids duplicating the same configuration in 5 places. Every service automatically gets:
- Java 21 compilation
- Spring Boot & dependency management plugins
- Lombok
- Actuator

### Sub-Project `build.gradle`

Each service's `build.gradle` only defines **service-specific** dependencies:

```groovy
// auth-service/build.gradle
plugins {
    id 'java'
    id 'org.springframework.boot'          // No version — inherited from root
    id 'io.spring.dependency-management'   // No version — inherited from root
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-mongodb'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation "io.jsonwebtoken:jjwt-api:${jjwtVersion}"    // Variable from root
}
```

**Key point**: Sub-project build files reference `${jjwtVersion}` which was defined in the root's `ext` block. This is Gradle's property inheritance.

### Dependency Management BOM

```groovy
dependencyManagement {
    imports {
        mavenBom "org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}"
    }
}
```

A **BOM (Bill of Materials)** is like a "version catalog." It tells Gradle: "For any Spring Cloud dependency, use the versions defined in this BOM." This is why `spring-cloud-starter-openfeign` in appointment-service doesn't need an explicit version — the BOM resolves it.

---

## 3. Auth Service — Deep Dive

### Purpose
Handles user registration, login, and JWT token generation. This is the **only service that creates tokens** — all other services only validate them.

### Package Structure
```
com.hospital.auth/
├── AuthServiceApplication.java     ← Spring Boot entry point
├── config/
│   ├── SecurityConfig.java         ← HTTP security rules
│   └── MongoConfig.java            ← Enables @CreatedDate
├── controller/
│   └── AuthController.java         ← REST endpoints
├── dto/
│   ├── LoginRequest.java           ← Input: email + password
│   ├── RegisterRequest.java        ← Input: name + email + password + role
│   └── AuthResponse.java           ← Output: token + email + role + name
├── entity/
│   └── User.java                   ← MongoDB document
├── enums/
│   └── Role.java                   ← ADMIN, DOCTOR, PATIENT
├── exception/
│   ├── AuthException.java          ← 401 Unauthorized
│   ├── UserAlreadyExistsException  ← 409 Conflict
│   ├── ErrorResponse.java          ← Standard error DTO
│   └── GlobalExceptionHandler.java ← @RestControllerAdvice
├── repository/
│   └── UserRepository.java         ← MongoDB queries
├── security/
│   ├── JwtTokenProvider.java       ← Token create/parse/validate
│   ├── JwtAuthenticationFilter.java← Filter chain JWT check
│   └── CustomUserDetailsService.java ← Loads user from DB
├── service/
│   ├── AuthService.java            ← Interface
│   └── impl/AuthServiceImpl.java   ← Business logic
└── util/
    └── AppConstants.java           ← Centralized constants
```

### Registration Flow — Step by Step

1. **Client sends** `POST /auth/register` with body:
   ```json
   {
     "name": "Jane Doe",
     "email": "jane@email.com",
     "password": "patient123",
     "role": "PATIENT"
   }
   ```

2. **`AuthController.register()`** receives the request. The `@Valid` annotation triggers Jakarta Bean Validation:
   - `@NotBlank` on name → ensures it's not empty
   - `@Email` on email → ensures valid email format
   - `@Size(min=6)` on password → ensures minimum length
   - `@NotNull` on role → ensures a role is selected
   
   If validation fails, Spring throws `MethodArgumentNotValidException`, caught by `GlobalExceptionHandler` → returns 400.

3. **`AuthServiceImpl.register()`** executes the business logic:
   - Calls `userRepository.existsByEmail(email)` — if true, throws `UserAlreadyExistsException` (409)
   - Calls `passwordEncoder.encode(rawPassword)` — BCrypt hashes the password (e.g., `$2a$10$...`)
   - Builds a `User` entity and calls `userRepository.save(user)` — MongoDB persists it
   - Calls `jwtTokenProvider.generateToken(email, role)` — creates a JWT
   - Returns `AuthResponse` with the token

4. **Client receives** a response:
   ```json
   {
     "token": "eyJhbGciOiJIUzUxMiJ9...",
     "email": "jane@email.com",
     "role": "PATIENT",
     "name": "Jane Doe"
   }
   ```

### Login Flow

1. Client sends `POST /auth/login` with `{email, password}`
2. `AuthServiceImpl.login()`:
   - Finds user by email → throws `AuthException` (401) if not found
   - Calls `passwordEncoder.matches(rawPassword, hashedPassword)` → throws `AuthException` (401) if mismatch
   - Generates JWT token
   - Returns `AuthResponse`

### Why BCrypt?

BCrypt is a **one-way hash function** with a built-in salt. When a user registers with password `"patient123"`, BCrypt produces something like `$2a$10$xJ2Q8k...`. You cannot reverse this hash to get `"patient123"` back. During login, BCrypt re-hashes the input with the same salt and compares the result. This means even if the database is compromised, passwords are safe.

### Manual Two-Step Doctor Registration

Unlike patients who register once, doctors have a **two-step process**:

1. **Step 1 (Auth Service)**: Register as a user with `role: DOCTOR` → gets JWT token
2. **Step 2 (Doctor Service)**: Create doctor profile with specialization, phone, etc. → requires the JWT from Step 1

This separation exists because:
- **Auth concerns** (email, password, role) are separate from **domain concerns** (specialization, availability)
- Each service owns its own data
- In production, doctor profiles might require admin approval before activation

---

## 4. Doctor Service — Deep Dive

### Purpose
Manages doctor profiles, specializations, and availability. Also provides **simulation endpoints** for testing Resilience4j.

### Key Entity: Doctor

```java
@Document(collection = "doctors")
public class Doctor {
    @Id
    private String id;              // MongoDB auto-generates
    private String name;            // "Dr. John Smith"
    private String specialization;  // "CARDIOLOGY"
    private String email;           // "dr.john@hospital.com"
    private String phone;           // "+1-555-0101"
    private boolean availability;   // true/false
    @CreatedDate
    private LocalDateTime createdAt; // Auto-set by MongoDB auditing
}
```

### Security: JWT Validation Only

The doctor-service **does NOT have a `UserDetailsService`**. It doesn't store users. Instead, its `JwtAuthenticationFilter` creates authentication directly from JWT claims:

```java
// Extract claims from the JWT token
String email = tokenProvider.getEmailFromToken(jwt);
String role = tokenProvider.getRoleFromToken(jwt);

// Create Spring Security authentication without database lookup
List<SimpleGrantedAuthority> authorities = 
    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));
UsernamePasswordAuthenticationToken authentication = 
    new UsernamePasswordAuthenticationToken(email, null, authorities);
```

**Why this works**: The JWT is already signed and validated. We trust its claims because only the auth-service (which holds the same secret key) can create valid tokens.

### Simulation Endpoints

These endpoints intentionally fail or delay responses so you can test how appointment-service (via Resilience4j) handles problems:

```java
@GetMapping("/simulate/slow")
public ResponseEntity<?> simulateSlowResponse() {
    Thread.sleep(5000);  // 5-second delay → triggers TimeLimiter (3s timeout)
    return ResponseEntity.ok("Delayed response");
}

@GetMapping("/simulate/failure")
public ResponseEntity<?> simulateFailure() {
    throw new RuntimeException("Simulated failure!");  // → triggers CircuitBreaker
}

@GetMapping("/simulate/random")
public ResponseEntity<?> simulateRandomFailure() {
    if (Math.random() > 0.5) throw new RuntimeException("Random failure!");
    return ResponseEntity.ok("Success");  // 50% chance of failure
}
```

---

## 5. Appointment Service — Deep Dive

### Purpose
The **most complex service**. It manages the appointment lifecycle, communicates with Doctor Service (via Feign), publishes events to Kafka, and implements all Resilience4j patterns.

### Appointment Lifecycle (State Machine)

```
                ┌──────────┐
        ┌──────►│ CONFIRMED├──────►┌───────────┐
        │       └──────────┘       │ COMPLETED  │
┌───────┴──┐                       └───────────┘
│  PENDING  │
└───────┬──┘
        │       ┌──────────┐
        ├──────►│ REJECTED │
        │       └──────────┘
        │
        └──────►┌──────────┐
                │ CANCELLED│
                └──────────┘

Valid transitions:
  PENDING → CONFIRMED  (doctor confirms)
  PENDING → REJECTED   (doctor rejects)
  PENDING → CANCELLED  (patient cancels)
  CONFIRMED → CANCELLED (patient cancels before appointment)
  CONFIRMED → COMPLETED (after appointment occurs)

Invalid transitions:
  COMPLETED → anything  (final state)
  CANCELLED → anything  (final state)
  REJECTED → anything   (final state)
```

### Creating an Appointment — Complete Code Flow

```
POST /appointments
     │
     ▼
AppointmentController.createAppointment(@Valid @RequestBody AppointmentRequest)
     │
     ▼
AppointmentServiceImpl.createAppointment(request)
     │
     ├─── getDoctorWithResilience(doctorId)  ←── @CircuitBreaker + @Retry
     │         │
     │         ├── SUCCESS: DoctorServiceClient.getDoctorById(id) via Feign
     │         │                    │
     │         │                    ▼
     │         │           HTTP GET http://localhost:8082/doctors/{id}
     │         │
     │         └── FAILURE: doctorServiceFallback(doctorId, exception)
     │                      Returns: DoctorResponse("Unknown Doctor")
     │
     ├─── AppointmentMapper.toEntity(request)  →  Appointment entity
     │
     ├─── appointmentRepository.save(appointment)  →  MongoDB
     │
     ├─── AppointmentMapper.toEvent(appointment, "CREATED")  →  AppointmentEvent
     │
     ├─── eventProducer.publishAppointmentCreated(event)  →  Kafka topic
     │
     └─── return AppointmentMapper.toResponse(saved, doctorName)
```

### The `getDoctorWithResilience` Method

This is where the magic of Resilience4j happens:

```java
@CircuitBreaker(name = "doctorService", fallbackMethod = "doctorServiceFallback")
@Retry(name = "doctorService", fallbackMethod = "doctorServiceFallback")
public DoctorResponse getDoctorWithResilience(String doctorId) {
    return doctorServiceClient.getDoctorById(doctorId);
}
```

**Order of execution**:
1. First, **Retry** wraps the call — if it fails, retry up to 3 times (2s, 4s, 8s exponential backoff)
2. Then, **CircuitBreaker** wraps the retry — if too many calls fail, open the circuit
3. If everything fails, the **fallback method** returns a default response

### Kafka Event Publishing

When an appointment is created/confirmed/rejected, the service publishes an event:

```java
AppointmentEvent event = AppointmentMapper.toEvent(saved, "CREATED");
eventProducer.publishAppointmentCreated(event);
```

The `AppointmentEventProducer` uses `KafkaTemplate`:

```java
kafkaTemplate.send(topic, event.getAppointmentId(), event);
//              ↑topic  ↑key (for partitioning)  ↑value (JSON serialized)
```

The **appointmentId is used as the key**. Kafka guarantees that messages with the same key go to the same partition. This means all events for the same appointment are processed in order.

### Appointment Reminder Scheduler

```java
@Scheduled(fixedRate = 3600000)  // Every 1 hour
public void sendAppointmentReminders() {
    // Find CONFIRMED appointments within the next 24 hours
    // Publish reminder event for each
}
```

`@EnableScheduling` on the main application class activates Spring's task scheduler. `@Scheduled(fixedRate = 3600000)` means "run this method every 3,600,000 milliseconds (1 hour)."

---

## 6. Notification Service — Deep Dive

### Purpose
Consumes Kafka events from appointment-service and creates notifications. Can send **actual emails** via Gmail SMTP.

### Kafka Consumer

```java
@KafkaListener(
    topics = "appointment-created",
    groupId = "notification-group"
)
public void handleAppointmentCreated(AppointmentEvent event) {
    String message = String.format(
        "Dear %s, your appointment has been created for %s...",
        event.getPatientName(), event.getAppointmentDate()
    );
    notificationService.createAndSendNotification(
        event.getAppointmentId(), event.getPatientEmail(),
        message, NotificationType.APPOINTMENT_CREATED
    );
}
```

**`@KafkaListener`** tells Spring Kafka: "When a message arrives on the `appointment-created` topic, deserialize it as an `AppointmentEvent` and call this method."

**`groupId = "notification-group"`** means this consumer belongs to the `notification-group` consumer group. Kafka ensures each message is delivered to **exactly one consumer** within a group. If you scale to 3 notification-service instances, Kafka distributes partitions among them.

### Notification Processing Pipeline

```
Kafka Event Received
       │
       ▼
AppointmentEventConsumer.handleAppointmentCreated(event)
       │
       ▼
NotificationServiceImpl.createAndSendNotification(...)
       │
       ├─── Build Notification entity
       │    (appointmentId, recipientEmail, message, type, emailSent=false)
       │
       ├─── notificationRepository.save(notification)  →  MongoDB
       │
       ├─── emailService.sendEmail(to, subject, body)
       │         │
       │         ├── emailEnabled = false → LOG message, return false
       │         │
       │         └── emailEnabled = true → JavaMailSender.send()
       │                   │
       │                   ├── SUCCESS → return true
       │                   └── FAILURE → log error, return false
       │
       ├─── IF email sent: notification.setEmailSent(true), save again
       │
       └─── return NotificationResponse
```

### Email Configuration

```yaml
notification:
  email:
    enabled: false   # Set to true to actually send emails
    
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: your-email@gmail.com      # Replace with real Gmail
    password: your-app-password          # Gmail App Password (NOT regular password)
```

**Why `enabled: false` by default?** So the application starts without valid SMTP credentials. For local development, emails are just logged. In production, set `enabled: true` and configure real SMTP credentials.

**Gmail App Password**: Since Google disabled "less secure apps," you need to:
1. Enable 2-Factor Authentication on your Google account
2. Go to https://myaccount.google.com/apppasswords
3. Generate an app-specific password
4. Use that password in `spring.mail.password`

### Failed Notification Resend

Admin can view and resend failed notifications:

```
GET /admin/notifications/failed     → returns notifications where emailSent=false
POST /admin/notifications/{id}/resend → retries email for a specific notification
```

---

## 7. API Gateway — Deep Dive

### Purpose
Single entry point for all client requests. Validates JWT tokens and routes (proxies) requests to downstream services.

### Why Not Spring Cloud Gateway?

Spring Cloud Gateway is **reactive** (WebFlux-based). Our services are **servlet-based** (Spring MVC). Mixing reactive and blocking paradigms adds complexity. Instead, we use a simple `RestTemplate`-based proxy controller that forwards requests — simpler and easier to understand.

### Request Routing

```java
@RestController
@RequestMapping("/api")
public class GatewayController {

    @RequestMapping("/auth/**")
    public ResponseEntity<String> routeToAuth(HttpServletRequest request, ...) {
        String path = request.getRequestURI().replaceFirst("/api/auth", "/auth");
        String targetUrl = authServiceUrl + path;
        return proxyRequest(targetUrl, HttpMethod.valueOf(request.getMethod()), request, body);
    }
}
```

**How it works**:
1. Client calls `http://localhost:8080/api/auth/register`
2. Gateway strips `/api` prefix and routes to `http://localhost:8084/auth/register`
3. Gateway forwards the Authorization header, request body, and query parameters
4. Gateway returns the downstream response to the client

### Route Map

| Gateway URL | Downstream Service | Downstream URL |
|---|---|---|
| `/api/auth/**` | auth-service:8084 | `/auth/**` |
| `/api/appointments/**` | appointment-service:8081 | `/appointments/**` |
| `/api/doctors/**` | doctor-service:8082 | `/doctors/**` |
| `/api/notifications/**` | notification-service:8083 | `/notifications/**` |
| `/api/admin/analytics/doctors` | doctor-service:8082 | `/admin/analytics/doctors` |
| `/api/admin/notifications/**` | notification-service:8083 | `/admin/notifications/**` |
| `/api/admin/system/**` | appointment-service:8081 | `/admin/system/**` |
| `/api/admin/kafka/**` | appointment-service:8081 | `/admin/kafka/**` |

### Error Handling

```java
private ResponseEntity<String> proxyRequest(String targetUrl, ...) {
    try {
        return restTemplate.exchange(targetUrl, method, entity, String.class);
    } catch (Exception e) {
        return ResponseEntity.status(503)
            .body("{\"error\": \"Service unavailable\"}");
    }
}
```

If a downstream service is down, the gateway returns **503 Service Unavailable** instead of an ugly stacktrace.

---

## 8. JWT Authentication — End-to-End

### What Is JWT?

JWT (JSON Web Token) is a compact, self-contained token for securely transmitting information between parties. A JWT looks like:

```
eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJqYW5lQGVtYWlsLmNvbSIsInJvbGUiOiJQQVRJRU5UIiwiaWF0IjoxNzE2MjAwMDAwLCJleHAiOjE3MTYyODY0MDB9.SIGNATURE
```

It has 3 parts separated by dots:
1. **Header**: `{"alg": "HS512"}` — the signing algorithm
2. **Payload**: `{"sub": "jane@email.com", "role": "PATIENT", "iat": ..., "exp": ...}` — the claims
3. **Signature**: HMAC-SHA512 hash of (header + payload + secret key)

### Token Generation (Auth Service Only)

```java
public String generateToken(String email, String role) {
    return Jwts.builder()
        .setSubject(email)                              // "sub" claim
        .claim("role", role)                             // custom "role" claim
        .setIssuedAt(new Date())                         // "iat" claim
        .setExpiration(new Date(now + 86400000))         // "exp" claim (24h)
        .signWith(signingKey, SignatureAlgorithm.HS512)  // Sign with secret
        .compact();
}
```

### Token Validation (Every Service)

Every service has a `JwtAuthenticationFilter` that runs on **every request**:

```
HTTP Request arrives
       │
       ▼
JwtAuthenticationFilter.doFilterInternal()
       │
       ├── Extract "Authorization" header
       │
       ├── Check if it starts with "Bearer "
       │         │
       │         ├── No → skip filter, continue chain (unauthenticated)
       │         │
       │         └── Yes → extract token string
       │
       ├── jwtTokenProvider.validateToken(token)
       │         │
       │         ├── false → skip filter (Spring Security will return 401)
       │         │
       │         └── true → extract email + role from token
       │
       ├── Create UsernamePasswordAuthenticationToken with authorities
       │
       ├── Set in SecurityContextHolder.getContext()
       │
       └── Continue filter chain (authenticated)
```

### Shared Secret Key

All 5 services have the **same JWT secret** in their `application.yml`:

```yaml
jwt:
  secret: 9a4f2c8d3b7a1e5f...
```

This means a token generated by auth-service can be validated by any other service. In production, you'd use environment variables or a secrets vault instead of hardcoding.

### How `ROLE_` Prefix Works

Spring Security's `hasRole("ADMIN")` internally checks for `ROLE_ADMIN`. So when creating the authentication:

```java
// JWT stores: "ADMIN"
// We prefix with "ROLE_": "ROLE_ADMIN"
new SimpleGrantedAuthority("ROLE_" + role);
```

Then in controllers:
```java
@PreAuthorize("hasRole('ADMIN')")  // Matches "ROLE_ADMIN"
```

---

## 9. Apache Kafka — Event-Driven Architecture

### Why Kafka?

Without Kafka, when a patient books an appointment, the appointment-service would need to directly call notification-service to send an email. If notification-service is down, the appointment creation would **fail**. That's tight coupling.

With Kafka:
1. Appointment-service publishes an event to Kafka → **appointment creation succeeds immediately**
2. Notification-service consumes the event **whenever it's ready** (even if it was down earlier)
3. The event is **persisted in Kafka** until consumed, so nothing is lost

### Kafka Concepts Used

#### Topics
A topic is like a **category or channel** for messages:
- `appointment-created` — published when a new appointment is booked
- `appointment-confirmed` — published when a doctor confirms
- `appointment-rejected` — published when a doctor rejects
- `appointment-reminder` — published by the hourly scheduler

#### Partitions
Each topic has **3 partitions**. Messages with the same key (appointmentId) always go to the same partition, ensuring ordering per appointment.

#### Consumer Groups
Notification-service uses `groupId: notification-group`. If you run 3 instances of notification-service, Kafka assigns each instance different partitions. Each message is processed by **exactly one instance**.

#### Serialization/Deserialization

**Producer (appointment-service)**:
```yaml
spring.kafka.producer:
  key-serializer: StringSerializer        # Key is a String (appointmentId)
  value-serializer: JsonSerializer         # Value is JSON (AppointmentEvent)
  properties:
    spring.json.type.mapping: appointmentEvent:com.hospital.appointment.kafka.event.AppointmentEvent
```

**Consumer (notification-service)**:
```yaml
spring.kafka.consumer:
  key-deserializer: StringDeserializer
  value-deserializer: JsonDeserializer
  properties:
    spring.json.trusted.packages: '*'      # Trust all packages for deserialization
    spring.json.type.mapping: appointmentEvent:com.hospital.notification.kafka.event.AppointmentEvent
```

**`type.mapping`** is critical. The producer serializes `com.hospital.appointment.kafka.event.AppointmentEvent` and the consumer deserializes into `com.hospital.notification.kafka.event.AppointmentEvent`. Even though these are different classes in different packages, the mapping tells Kafka "treat them as the same type." The class structures must match.

#### AppointmentEvent DTO

Both producer and consumer have identical DTOs:
```java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AppointmentEvent {
    private String appointmentId;
    private String patientName;
    private String patientEmail;
    private String doctorId;
    private String appointmentDate;  // ISO-8601 String (not LocalDateTime!)
    private String symptoms;
    private String status;
    private String eventType;        // "CREATED", "CONFIRMED", etc.
    private String timestamp;        // When the event was published
}
```

**Why `String` for dates?** JSON serialization of `LocalDateTime` across different services can cause issues. ISO-8601 strings (`"2026-06-15T10:00:00"`) are universally parseable.

### Kafka Config Classes

**Producer Config** (`KafkaConfig.java` in appointment-service):
```java
@Bean
public ProducerFactory<String, AppointmentEvent> producerFactory() {
    Map<String, Object> config = new HashMap<>();
    config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    return new DefaultKafkaProducerFactory<>(config);
}

@Bean
public KafkaTemplate<String, AppointmentEvent> kafkaTemplate() {
    return new KafkaTemplate<>(producerFactory());
}
```

**Topic Config** (`KafkaTopicConfig.java`):
```java
@Bean
public NewTopic appointmentCreatedTopic() {
    return TopicBuilder.name("appointment-created")
        .partitions(3)
        .replicas(1)
        .build();
}
```

**Consumer Config** (`KafkaConfig.java` in notification-service):
```java
@Bean
public ConsumerFactory<String, AppointmentEvent> consumerFactory() {
    Map<String, Object> config = new HashMap<>();
    config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    config.put(ConsumerConfig.GROUP_ID_CONFIG, "notification-group");
    config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
    return new DefaultKafkaConsumerFactory<>(config);
}
```

---

## 10. Resilience4j — Fault Tolerance Patterns

### The Problem

Appointment-service calls Doctor-service via HTTP. If Doctor-service is:
- **Down** → HTTP connection refused → appointment creation fails
- **Slow** → response takes 30 seconds → threads pile up, system becomes unresponsive
- **Intermittently failing** → some calls succeed, some fail randomly

### Solution: Three Resilience4j Patterns

#### Pattern 1: Circuit Breaker

```yaml
resilience4j.circuitbreaker.instances.doctorService:
  slidingWindowSize: 10           # Monitor the last 10 calls
  failureRateThreshold: 50       # Open circuit if 50%+ fail
  waitDurationInOpenState: 10000  # Stay OPEN for 10 seconds
  permittedNumberOfCallsInHalfOpenState: 3  # Allow 3 test calls
  minimumNumberOfCalls: 5         # Don't evaluate until 5 calls made
```

**State machine**:
```
CLOSED ──(≥50% of last 10 calls failed)──→ OPEN ──(after 10s)──→ HALF_OPEN
  ↑                                                                    │
  └────────────(≥1 of 3 test calls succeeds)───────────────────────────┘
                                                                       │
                                        (all 3 test calls fail)        │
                                              OPEN ◄───────────────────┘
```

- **CLOSED**: Normal operation. All calls go through. Tracks success/failure.
- **OPEN**: ALL calls immediately fail with `CallNotPermittedException`. No HTTP call is made. Returns fallback instantly.
- **HALF_OPEN**: Allows 3 test calls. If they succeed → CLOSED. If they fail → OPEN again.

#### Pattern 2: Retry

```yaml
resilience4j.retry.instances.doctorService:
  maxAttempts: 3                       # Try up to 3 times
  waitDuration: 2000                   # Wait 2s between retries
  enableExponentialBackoff: true        # Double the wait each time
  exponentialBackoffMultiplier: 2       # 2s → 4s → 8s
```

**Timeline**: `Call → Fail → Wait 2s → Retry → Fail → Wait 4s → Retry → Fail → Fallback`

#### Pattern 3: TimeLimiter

```yaml
resilience4j.timelimiter.instances.doctorService:
  timeoutDuration: 3s                  # Cancel if no response in 3s
```

If Doctor-service takes more than 3 seconds, the call is cancelled with a `TimeoutException`.

#### Fallback Method

```java
public DoctorResponse doctorServiceFallback(String doctorId, Exception ex) {
    log.warn("Fallback triggered for doctorId={}: {}", doctorId, ex.getMessage());
    return DoctorResponse.builder()
        .id(doctorId)
        .name("Unknown Doctor (Service Unavailable)")
        .availability(true)  // Assume available to not block booking
        .build();
}
```

**Important design decision**: The fallback returns `availability = true` rather than blocking appointment creation. This is **graceful degradation** — the appointment is created even when Doctor-service is down, with the doctor name showing as "Unknown."

---

## 11. Spring Cloud OpenFeign — Inter-Service Communication

### What Is Feign?

Feign is a **declarative HTTP client**. Instead of writing `RestTemplate` code to call Doctor-service, you define an interface:

```java
@FeignClient(
    name = "doctor-service",
    url = "${doctor.service.url}",        // http://localhost:8082
    fallback = DoctorServiceFallback.class // Used when service is down
)
public interface DoctorServiceClient {

    @GetMapping("/doctors/{id}")
    DoctorResponse getDoctorById(@PathVariable("id") String id);

    @GetMapping("/doctors/availability/{id}")
    DoctorResponse checkAvailability(@PathVariable("id") String id);
}
```

At runtime, Spring generates an implementation that:
1. Builds the HTTP request: `GET http://localhost:8082/doctors/abc123`
2. Sends it
3. Deserializes the JSON response into `DoctorResponse`

### Feign Request Interceptor

The `FeignConfig` adds JWT token forwarding:

```java
@Bean
public RequestInterceptor requestInterceptor() {
    return template -> {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // Forward the JWT token to downstream services
        template.header("Authorization", "Bearer " + token);
    };
}
```

This ensures when appointment-service calls doctor-service, the patient's JWT token is forwarded so doctor-service can authenticate the request.

---

## 12. MongoDB — Data Layer

### Why MongoDB?

- **Schema flexibility**: Each service defines its own document structure
- **JSON-native**: Spring Boot serializes/deserializes Java objects to/from BSON automatically
- **No migrations needed**: Add a field to an entity → it just works

### Spring Data MongoDB

```java
@Document(collection = "users")
public class User {
    @Id
    private String id;    // MongoDB assigns ObjectId if null

    @Indexed(unique = true)
    private String email;  // Creates a unique index for fast lookups
}
```

**`@Document(collection = "users")`**: Maps this class to the `users` collection in MongoDB.

**`@Id`**: The primary key. MongoDB generates a unique ObjectId (e.g., `"6651a2b3c4d5e6f7a8b9c0d1"`) if the `id` is null when saving.

**`@Indexed(unique = true)`**: Creates a MongoDB index on the `email` field. `unique = true` prevents duplicate emails at the database level (even if the application-level check somehow fails).

### Repository Pattern

```java
public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

**No implementation needed!** Spring Data generates the queries automatically from the method names:
- `findByEmail(email)` → `db.users.find({email: "jane@email.com"})`
- `existsByEmail(email)` → `db.users.count({email: "jane@email.com"}) > 0`

### `@EnableMongoAuditing`

```java
@Configuration
@EnableMongoAuditing
public class MongoConfig {}
```

This enables `@CreatedDate` annotation on entity fields. When you save a new document, MongoDB auditing automatically sets `createdAt = LocalDateTime.now()`.

### Database Per Service

| Service | Database | Collections |
|---|---|---|
| auth-service | auth_db | users |
| doctor-service | doctor_db | doctors |
| appointment-service | appointment_db | appointments |
| notification-service | notification_db | notifications |

Each service has its own database. This is the **Database per Service** pattern from microservices architecture. Services cannot directly query each other's databases — they must use APIs.

---

## 13. Spring Security Configuration — How It Works

### SecurityConfig Explained

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // Enables @PreAuthorize annotations
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())                    // Disable CSRF (stateless API)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(STATELESS))    // No sessions (JWT-based)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**").permitAll()      // Public endpoints
                .requestMatchers("/actuator/**").permitAll()  // Health checks
                .anyRequest().authenticated()                 // Everything else needs JWT
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
```

**Why disable CSRF?** CSRF protection is for browser-based sessions with cookies. Since we use JWT tokens in the Authorization header (not cookies), CSRF is irrelevant and would block our API calls.

**Why STATELESS sessions?** In traditional web apps, the server stores session data. With JWT, all session information is in the token itself. The server doesn't store anything — each request is independently authenticated.

**Filter ordering**: `addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)` means "run our JWT filter BEFORE Spring's default username/password filter." Our filter sets the SecurityContext with JWT-based authentication, so the default filter finds the user already authenticated and skips.

### `@PreAuthorize` Role-Based Access

```java
@PutMapping("/{id}/confirm")
@PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
public ResponseEntity<AppointmentResponse> confirmAppointment(@PathVariable String id) {
    // Only DOCTOR and ADMIN can reach here
}
```

`@EnableMethodSecurity` activates `@PreAuthorize`. Before the method executes, Spring checks if the authenticated user has the required role. If not, it throws `AccessDeniedException` (403 Forbidden).

---

## 14. Exception Handling Strategy

### The `@RestControllerAdvice` Pattern

Instead of try-catch blocks in every controller, we have a **centralized exception handler**:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppointmentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(AppointmentNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        // Collect all field errors into a single message
        String errors = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .collect(Collectors.joining(", "));
        return buildResponse(HttpStatus.BAD_REQUEST, errors);
    }
}
```

**How it works**: When any `@RestController` method throws an exception, Spring looks for a matching `@ExceptionHandler`. The handler converts the exception into a structured JSON response:

```json
{
  "status": 404,
  "message": "Appointment not found with id: abc123",
  "timestamp": "2026-05-20T10:30:00"
}
```

### Exception Mapping

| Exception | HTTP Status | When |
|---|---|---|
| `AppointmentNotFoundException` | 404 | Appointment ID doesn't exist |
| `DoctorNotFoundException` | 404 | Doctor ID doesn't exist |
| `AuthException` | 401 | Invalid credentials |
| `UserAlreadyExistsException` | 409 | Duplicate email registration |
| `InvalidStatusTransitionException` | 400 | e.g., confirming a CANCELLED appointment |
| `MethodArgumentNotValidException` | 400 | `@Valid` validation failures |
| `CallNotPermittedException` | 503 | Circuit breaker is OPEN |
| `TimeoutException` | 504 | TimeLimiter expired |
| `KafkaException` | 500 | Kafka publish failure |
| `Exception` | 500 | Any unhandled exception |

---

## 15. Lombok — Boilerplate Reduction

### What Lombok Does

Lombok generates code at compile time via annotation processing:

| Annotation | Generates |
|---|---|
| `@Data` | getters, setters, toString, equals, hashCode |
| `@Builder` | Builder pattern (e.g., `User.builder().name("John").build()`) |
| `@NoArgsConstructor` | Empty constructor |
| `@AllArgsConstructor` | Constructor with all fields |
| `@RequiredArgsConstructor` | Constructor with `final` fields (used for dependency injection) |
| `@Slf4j` | `private static final Logger log = LoggerFactory.getLogger(...)` |

### Constructor Injection via `@RequiredArgsConstructor`

```java
@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl {
    private final AppointmentRepository appointmentRepository;  // final → required
    private final DoctorServiceClient doctorServiceClient;       // final → required
    private final AppointmentEventProducer eventProducer;        // final → required
}
```

Lombok generates:
```java
public AppointmentServiceImpl(AppointmentRepository repo, DoctorServiceClient client, AppointmentEventProducer producer) {
    this.appointmentRepository = repo;
    this.doctorServiceClient = client;
    this.eventProducer = producer;
}
```

Spring sees the constructor and automatically injects the beans. This is **constructor injection** — the recommended pattern over `@Autowired` field injection.

---

## 16. application.yml — Configuration Explained

### Why YAML over .properties?

YAML supports nested structures, making configurations more readable:

```yaml
# YAML (used in this project)
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/appointment_db

# Equivalent .properties (harder to read)
spring.data.mongodb.uri=mongodb://localhost:27017/appointment_db
```

### Key Configuration Sections

#### Server Port
```yaml
server:
  port: 8081
```
Each service listens on a different port. In production with service discovery, ports might be dynamic.

#### MongoDB
```yaml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/appointment_db
```
The database (`appointment_db`) is created automatically by MongoDB when data is first written.

#### JWT
```yaml
jwt:
  secret: 9a4f2c8d3b7a1...
  expiration: 86400000
```
Injected via `@Value("${jwt.secret}")` into `JwtTokenProvider`. The **same secret** in all services enables cross-service token validation.

#### Kafka
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
```
`bootstrap-servers` is the Kafka broker address. Serializers define how keys/values are converted to bytes for network transmission.

#### Resilience4j
```yaml
resilience4j:
  circuitbreaker:
    instances:
      doctorService:               # Instance name — matches @CircuitBreaker(name = "doctorService")
        slidingWindowSize: 10
        failureRateThreshold: 50
```
The instance name `doctorService` in YAML must match the `name` parameter in the annotation.

#### Actuator
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,info,circuitbreakers,retries
```
Exposes monitoring endpoints at `/actuator/health`, `/actuator/metrics`, etc.

---

## 17. End-to-End Request Flows

### Flow 1: Complete Appointment Booking

```
1. Patient registers:
   POST http://localhost:8080/api/auth/register
   Body: {name, email, password, role: "PATIENT"}
   → auth-service creates user, returns JWT token
   
2. Doctor registers:
   POST http://localhost:8080/api/auth/register
   Body: {name, email, password, role: "DOCTOR"}
   → auth-service creates user, returns JWT token
   
3. Doctor creates profile:
   POST http://localhost:8080/api/doctors
   Header: Authorization: Bearer <doctor_token>
   Body: {name, specialization: "CARDIOLOGY", email, phone, availability: true}
   → doctor-service creates doctor record, returns doctorId
   
4. Patient books appointment:
   POST http://localhost:8080/api/appointments
   Header: Authorization: Bearer <patient_token>
   Body: {patientName, patientEmail, doctorId, appointmentDate, symptoms}
   → appointment-service:
     a. Calls doctor-service via Feign to validate doctorId
     b. Saves appointment with status PENDING
     c. Publishes "appointment-created" to Kafka
   → notification-service:
     d. Consumes Kafka event
     e. Creates notification in MongoDB
     f. Sends email (if enabled)
     
5. Doctor confirms appointment:
   PUT http://localhost:8080/api/appointments/{id}/confirm
   Header: Authorization: Bearer <doctor_token>
   → appointment-service:
     a. Changes status to CONFIRMED
     b. Publishes "appointment-confirmed" to Kafka
   → notification-service:
     c. Sends confirmation email to patient
```

### Flow 2: Circuit Breaker in Action

```
1. Stop doctor-service (kill the process)

2. Patient books appointment:
   POST /appointments
   → appointment-service tries to call doctor-service via Feign
   → Connection refused → Retry 1 (wait 2s)
   → Connection refused → Retry 2 (wait 4s)
   → Connection refused → Retry 3 (wait 8s)
   → All retries exhausted → Fallback triggered
   → Returns: doctorName = "Unknown Doctor (Service Unavailable)"
   → Appointment still created successfully!
   
3. Repeat step 2 multiple times (5+ times to meet minimumNumberOfCalls)
   → Circuit breaker evaluates: 100% failure rate > 50% threshold
   → Circuit breaker transitions to OPEN state
   
4. Next appointment request:
   → CircuitBreaker immediately returns CallNotPermittedException
   → No HTTP call is made (doctor-service is not contacted)
   → Fallback executes instantly
   → Response is much faster!
   
5. Check circuit breaker state:
   GET /admin/system/circuit-breakers
   → Response: {doctorService: {state: "OPEN", failureRate: 100%}}
   
6. Start doctor-service again

7. Wait 10 seconds (waitDurationInOpenState)
   → Circuit transitions to HALF_OPEN
   
8. Next 3 requests (permittedNumberOfCallsInHalfOpenState):
   → Real HTTP calls to doctor-service
   → If successful → Circuit transitions back to CLOSED
   → If failed → Circuit goes back to OPEN
```

---

## 18. Design Patterns Used

| Pattern | Where | Purpose |
|---|---|---|
| **Builder** | All DTOs and entities (`@Builder`) | Clean object construction |
| **Repository** | All `*Repository` interfaces | Data access abstraction |
| **DTO** | Request/Response classes | Decouple API from entities |
| **Service Layer** | Interface + Impl pattern | Separate business logic from controllers |
| **Factory** | `KafkaConfig` (ProducerFactory, ConsumerFactory) | Create configured Kafka instances |
| **Observer** | Kafka pub/sub | Decouple appointment events from notifications |
| **Circuit Breaker** | Resilience4j on Feign calls | Prevent cascading failures |
| **Fallback** | `doctorServiceFallback()` method | Graceful degradation |
| **Proxy** | API Gateway routing | Single entry point |
| **Filter Chain** | `JwtAuthenticationFilter` | Cross-cutting authentication |
| **Strategy** | `Role` enum for different user types | Different behavior per role |
| **Singleton** | Spring `@Component` beans (default scope) | One instance per service |
| **Template Method** | `OncePerRequestFilter` | Base class for JWT filter |
| **Mapper** | `AppointmentMapper` | Convert between entity/DTO/event |

---

## 19. What Happens When Things Fail?

| Failure Scenario | Behavior |
|---|---|
| **MongoDB is down** | Service fails to start. Spring Data throws connection exception. |
| **Kafka is down** | Appointment creation succeeds (saved to MongoDB), but event publish fails. Log shows error. Notification is not created. |
| **Auth service is down** | Users cannot register/login. Existing JWT tokens still work on other services (stateless validation). |
| **Doctor service is down** | Appointment creation uses fallback (doctor name = "Unknown"). Circuit breaker opens after 5 failures. |
| **Notification service is down** | Kafka retains events. When service restarts, `auto-offset-reset: earliest` processes all unread events. |
| **API Gateway is down** | Clients can still call services directly on their ports (8081-8084). Gateway is optional for direct access. |
| **Invalid JWT token** | `JwtAuthenticationFilter` returns false → Spring Security returns 401 Unauthorized. |
| **Expired JWT token** | Same as invalid. `ExpiredJwtException` caught in `validateToken()` → returns false. |
| **Duplicate email registration** | `UserAlreadyExistsException` → `GlobalExceptionHandler` → 409 Conflict. |

---

## 20. Production Considerations

### What Would Change in Production?

| Aspect | Current (Dev) | Production |
|---|---|---|
| JWT Secret | Hardcoded in YAML | Environment variable or Vault |
| MongoDB | localhost:27017 | MongoDB Atlas or replica set |
| Kafka | Single broker | Multi-broker cluster |
| Service URLs | Hardcoded localhost | Service Discovery (Eureka/Consul) |
| Email SMTP | Disabled | Enabled with SendGrid/SES |
| Logging | Console | ELK Stack (Elasticsearch, Logstash, Kibana) |
| Monitoring | Actuator endpoints | Prometheus + Grafana |
| Tracing | None | Zipkin/Jaeger distributed tracing |
| CORS | Allow all origins | Restrict to frontend domain |
| HTTPS | HTTP | TLS certificates |
| Secrets | In YAML files | HashiCorp Vault or AWS Secrets Manager |

### Missing Production Patterns

1. **Outbox Pattern**: Instead of publishing to Kafka directly, save events to a local "outbox" table in the same database transaction. A separate process publishes from the outbox. This guarantees atomicity.

2. **Saga Pattern**: For distributed transactions spanning multiple services. Compensating transactions undo partial work if a step fails.

3. **Dead Letter Queue (DLQ)**: Failed Kafka messages go to a DLQ for manual inspection instead of being lost.

4. **Rate Limiting**: Prevent API abuse with token bucket or sliding window algorithms.

5. **API Versioning**: `/api/v1/appointments` for backward compatibility.

6. **Health Check Dependencies**: Report downstream service health in actuator.

---

## 21. Key Annotations Reference

### Spring Boot
| Annotation | Purpose |
|---|---|
| `@SpringBootApplication` | Combines `@Configuration`, `@EnableAutoConfiguration`, `@ComponentScan` |
| `@EnableFeignClients` | Activates Feign client interface scanning |
| `@EnableScheduling` | Activates `@Scheduled` method execution |

### Spring Web
| Annotation | Purpose |
|---|---|
| `@RestController` | `@Controller` + `@ResponseBody` — all methods return JSON |
| `@RequestMapping("/path")` | Base URL path for all methods in the controller |
| `@GetMapping`, `@PostMapping`, `@PutMapping` | Maps HTTP methods to Java methods |
| `@PathVariable` | Extracts values from URL path segments |
| `@RequestBody` | Deserializes request body JSON into a Java object |
| `@Valid` | Triggers Jakarta Bean Validation on the parameter |

### Spring Security
| Annotation | Purpose |
|---|---|
| `@EnableWebSecurity` | Activates Spring Security configuration |
| `@EnableMethodSecurity` | Enables `@PreAuthorize` on methods |
| `@PreAuthorize("hasRole('ADMIN')")` | Method-level access control |

### Spring Data MongoDB
| Annotation | Purpose |
|---|---|
| `@Document(collection = "name")` | Maps a class to a MongoDB collection |
| `@Id` | Marks the primary key field |
| `@Indexed(unique = true)` | Creates a database index |
| `@CreatedDate` | Auto-populates with current timestamp on save |
| `@EnableMongoAuditing` | Enables `@CreatedDate` and `@LastModifiedDate` |

### Jakarta Validation
| Annotation | Purpose |
|---|---|
| `@NotBlank` | Field must not be null or whitespace |
| `@NotNull` | Field must not be null |
| `@Email` | Must be a valid email format |
| `@Size(min = 6)` | String length constraints |
| `@Future` | Date must be in the future |

### Kafka
| Annotation | Purpose |
|---|---|
| `@KafkaListener(topics, groupId)` | Marks a method as a Kafka consumer |

### Resilience4j
| Annotation | Purpose |
|---|---|
| `@CircuitBreaker(name, fallbackMethod)` | Wraps method with circuit breaker |
| `@Retry(name, fallbackMethod)` | Wraps method with retry logic |
| `@TimeLimiter(name)` | Wraps method with timeout |

### Lombok
| Annotation | Purpose |
|---|---|
| `@Data` | Generates getters, setters, toString, equals, hashCode |
| `@Builder` | Generates builder pattern |
| `@Slf4j` | Generates `log` field for logging |
| `@RequiredArgsConstructor` | Generates constructor for `final` fields |
| `@NoArgsConstructor` / `@AllArgsConstructor` | Generates empty / full constructors |

---

> **This document covers every aspect of the Hospital Appointment System. Use it for code reviews, interview preparation, and understanding enterprise microservices architecture.**
