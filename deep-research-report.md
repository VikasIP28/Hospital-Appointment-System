# High-Level Architecture  
We propose a microservices-based Hospital Appointment System comprising **Appointment Service**, **Doctor Service**, **Notification Service** (and optionally an **Auth Service**). Each service runs on its own port and has its own datastore (polyglot persistence: e.g. MySQL for appointment/doctor data and MongoDB for notifications). Services communicate via **REST APIs** and asynchronously via **Apache Kafka**. For example, when a patient books an appointment (Appointment Service), the service publishes an `appointment-created` event to Kafka. Downstream services (Doctor and Notification) consume relevant topics (e.g. `appointment-confirmed`, `appointment-rejected`, `appointment-reminder`) to react to events. This **event-driven architecture** decouples services: producers publish events without knowing which consumers will process them【31†L225-L234】, making the system more scalable and fault-tolerant【31†L225-L234】【31†L231-L239】. Circuit breakers (Resilience4j) protect synchronous REST calls (e.g. from Appointment to Doctor), providing fail-fast behavior and graceful fallbacks【2†L38-L42】【25†L106-L115】. Retry and timeout policies further harden inter-service calls. Below is a **logical flow**:

- **Appointment Service** (port 8081): Manages booking. On creation it saves to MySQL and publishes an `appointment-created` event (JSON) to Kafka.
- **Doctor Service** (port 8082): Manages doctors. Doctors authenticate and see pending appointments. They confirm/reject via REST, which updates MySQL and publishes `appointment-confirmed` or `appointment-rejected` events.
- **Notification Service** (port 8083): Consumes Kafka events (e.g. confirmed/rejected) and logs or sends notifications (optionally via email/SMS). It uses MongoDB to store notification records (demonstrating use of a NoSQL store).
- *(Optional Auth Service)* handles login/authentication (e.g. JWT tokens); for simplicity we can stub authentication or use Spring Security in each service.

All services use **Spring Boot 3.x** (with Spring Web, Spring Data JPA/Mongo) on Java 21. They have clean layered packages (`controller`, `service`, `repository`, `model`, etc.) and use **application.yml** for configuration only (per constraint). We use **Gradle** builds (no Maven) and **Lombok** for boilerplate. Below we detail each concern, with code snippets and configurations.

# Kafka Event-Driven Communication  
## Kafka Configuration (YAML)  
In each Spring Boot app, Kafka is auto-configured via `spring.kafka.*` properties【42†L370-L379】. For example, in `application.yml`:

```yaml
spring:
  kafka:
    bootstrap-servers: "localhost:9092"        # Kafka broker address
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      group-id: "notification-group"            # consumer group for notifications
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.value.default.type: com.hospital.dto.AppointmentEvent
        spring.json.trusted.packages: "*"
      auto-offset-reset: earliest
```

This sets the Kafka server and serializers. The `JsonSerializer`/`JsonDeserializer` enable sending Java objects (Jackson JSON) over Kafka【45†L231-L239】. We also ensure the consumer trusts our DTO package (`trusted.packages`) or sets the target type (`value.default.type`). Topics can be auto-created by defining `@Bean NewTopic` in a `@Configuration`, or created manually via CLI. 

## Kafka Producer Example  
In the Appointment Service, after saving an appointment, we publish an event:

```java
@Component
public class AppointmentProducer {
    private final KafkaTemplate<String, AppointmentEvent> kafkaTemplate;
    public AppointmentProducer(KafkaTemplate<String, AppointmentEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendAppointmentCreated(AppointmentEvent event) {
        // Async send; KafkaTemplate is auto-configured by Spring Boot【42†L416-L424】
        kafkaTemplate.send("appointment-created", event);
        // Log or handle future if needed
    }
}
```
This uses `KafkaTemplate.send(topic, value)` to publish (per Spring Boot docs)【42†L416-L424】. We define DTO `AppointmentEvent` (e.g. fields: `appointmentId, doctorId, patientEmail, dateTime, status`) which gets serialized to JSON.

The Doctor Service similarly can produce to `"appointment-confirmed"` or `"appointment-rejected"` when a doctor confirms/rejects an appointment. For example:

```java
@CircuitBreaker(name="doctorService", fallbackMethod="fallbackConfirm")
public Appointment confirmAppointment(Long apptId) {
    Appointment appt = appointmentRepo.findById(apptId).get();
    appt.setStatus(Status.CONFIRMED);
    appointmentRepo.save(appt);
    // Publish event for notification
    AppointmentEvent evt = new AppointmentEvent(appt);
    kafkaTemplate.send("appointment-confirmed", evt);
    return appt;
}
```

## Kafka Consumer Example  
In the Notification Service, we listen for events:

```java
@Component
public class NotificationListener {
    @KafkaListener(topics = "appointment-confirmed", groupId = "notification-group")
    public void onConfirmed(AppointmentEvent evt) {
        // Handle confirmed appointment (e.g., create a notification record, send email)
        String msg = "Appointment " + evt.getAppointmentId() + " confirmed for patient " + evt.getPatientEmail();
        notificationService.sendNotification(evt.getPatientEmail(), msg);
    }

    @KafkaListener(topics = "appointment-rejected", groupId = "notification-group")
    public void onRejected(AppointmentEvent evt) {
        String msg = "Appointment " + evt.getAppointmentId() + " was rejected by the doctor.";
        notificationService.sendNotification(evt.getPatientEmail(), msg);
    }
}
```

Here `@KafkaListener` automatically creates a consumer (with configured deserializer) and invokes the method when messages arrive【42†L469-L476】. We use different topics for each event type (appointment-created, -confirmed, -rejected, -reminder) to clearly separate concerns. **Serialization/Deserialization:** We rely on Spring’s `JsonSerializer/JsonDeserializer` which uses Jackson under the hood【45†L231-L239】. In `application.yml` we ensure `spring.kafka.consumer.properties.spring.json.value.default.type` is set to our DTO class (or trust all packages) so Spring knows how to convert JSON to `AppointmentEvent`.

## Kafka Topics and Event Flow  
We define four topics: 
- `appointment-created` (published by Appointment Service when new booking created), 
- `appointment-confirmed`, `appointment-rejected` (published by Doctor Service on doctor action), 
- `appointment-reminder` (used by Notification Service or a scheduler to send reminders for upcoming appointments). 

The **event flow** is thus: when an appointment is booked, services can independently react. For example, the Notification Service could listen on `appointment-created` to send a booking confirmation email to the patient, while the Appointment Service notifies the Doctor via a separate channel (or via REST) to take action. Once the doctor confirms, the Appointment Service updates status (CONFIRMED) and publishes `appointment-confirmed`. The Notification Service, upon receiving this, sends a final confirmation email. This asynchronous flow decouples components and allows horizontal scaling. As Confluent’s reference architecture notes, “services publish events to Kafka while downstream services react to those events instead of being called directly. Event-producing services are decoupled from event-consuming services”【31†L225-L234】. In summary, Kafka acts as a **broker** to buffer and forward events, aiding resilience (e.g. if Notification is down, events queue up until it recovers).

# Resilience4j (Circuit Breaker, Retry, Timeout)  
To handle failures in inter-service calls, we integrate **Resilience4j**. This lightweight Java library provides fault-tolerance patterns via annotations, leveraging Spring AOP【25†L82-L90】【25†L106-L115】. Key modules used are **Circuit Breaker**, **Retry**, and **Time Limiter (Timeout)**.

## Circuit Breaker (Appointment→Doctor)  
The Appointment Service must call the Doctor Service (e.g. to check doctor availability or fetch doctor data). We wrap this REST call with a Resilience4j circuit breaker to prevent cascading failures. The breaker has three states: 
- **Closed:** calls flow normally; failures are counted.  
- **Open:** if failure rate exceeds threshold, breaker opens and further calls are blocked immediately.  
- **Half-Open:** after a wait duration, a few test calls are allowed; success closes the circuit, failures open it again【2†L45-L53】.

For example, in `AppointmentService`:

```java
@Service
public class AppointmentService {
    private static final String DOCTOR_SERVICE = "doctorService";

    @CircuitBreaker(name = DOCTOR_SERVICE, fallbackMethod = "doctorFallback")
    @Retry(name = "doctorRetry", fallbackMethod = "doctorRetryFallback")
    public Doctor getDoctor(Long doctorId) {
        // Call Doctor Service over HTTP (e.g. RestTemplate or WebClient)
        return doctorClient.getDoctorById(doctorId);
    }

    // Fallback when circuit is open
    public Doctor doctorFallback(Long doctorId, Throwable ex) {
        // return default or throw custom exception
        return new Doctor(doctorId, "Unavailable", ...);
    }
    
    // Fallback when retries fail
    public Doctor doctorRetryFallback(Long doctorId, Throwable ex) {
        // Could log and return default as well
        return doctorFallback(doctorId, ex);
    }
}
```

Here, `@CircuitBreaker(name = "doctorService", fallbackMethod = "doctorFallback")` creates a circuit breaker with instance name "doctorService" and a fallback. The circuit breaker settings (threshold, wait duration, etc.) are configured in `application.yml` (see below). If calls to `getDoctor` keep failing (e.g. Doctor Service down), the circuit opens and immediately directs calls to `doctorFallback`. This prevents our service from hanging or overloading a failing endpoint【2†L45-L53】. In normal (closed) state, calls pass through.

We configure Resilience4j in YAML under `resilience4j.circuitbreaker.instances`:

```yaml
resilience4j:
  circuitbreaker:
    instances:
      doctorService:
        registerHealthIndicator: true
        slidingWindowSize: 5
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
        permittedNumberOfCallsInHalfOpenState: 2
```

This matches best practices: e.g. `failureRateThreshold: 50`% means 3 of 5 failures opens the circuit; the health indicator exposes metrics via Actuator【2†L129-L137】. Resilience4j will automatically expose metrics (via Spring Boot Actuator) and events, so operations can be monitored.

**Circuit States Demo:** We can simulate failures (e.g. Doctor Service randomly throwing exceptions) to demonstrate transitions:  
- Initially, calls succeed (Closed).  
- On repeated failures (e.g. force 5 calls to fail quickly), the breaker trips to Open.  
- After `waitDuration`, the breaker moves to Half-Open, allowing a couple of test calls. If they succeed, it closes; if one fails, it re-opens【2†L45-L53】.  
Logging of state transitions can be done via EventConsumer or built-in logs.

## Retry Mechanism  
We configure Resilience4j Retry to automatically retry transient failures. For example, annotate the same method with `@Retry(name="doctorRetry")` (as above). YAML:

```yaml
resilience4j:
  retry:
    instances:
      doctorRetry:
        maxRetryAttempts: 3
        waitDuration: 500ms
        retryExceptions:
          - java.io.IOException
        ignoreExceptions:
          - com.hospital.exception.NotFoundException
```

This says: on failure (IOException) retry up to 3 times with 500ms interval【8†L145-L153】. If all retries fail, the fallback `doctorRetryFallback` is invoked. We can also log each retry attempt (e.g. via `Retry.EventPublisher`). Reflectoring notes that Spring Boot lets us configure retries in YAML and simply annotate methods【8†L170-L178】. The snippet above demonstrates `@Retry(name="flightSearch")`; our use is analogous.

**Best Practice:** Place `@Retry` on methods calling external services. Don’t retry idempotent writes blindly. Combine with Circuit Breaker: in our example, both annotations are on the same method. Retries will occur within the Circuit’s closed state window. If retries still fail beyond threshold, the Circuit opens.

## Timeout (TimeLimiter)  
To avoid hanging calls, we add timeouts. Resilience4j’s **TimeLimiter** can wrap a `CompletableFuture`. In practice, using a non-blocking WebClient makes it easier. For example:

```java
@TimeLimiter(name = "doctorTimeLimiter")
public CompletableFuture<Doctor> getDoctorAsync(Long doctorId) {
    return CompletableFuture.supplyAsync(() -> doctorClient.getDoctorById(doctorId));
}
```

YAML config:

```yaml
resilience4j:
  timelimiter:
    instances:
      doctorTimeLimiter:
        timeoutDuration: 2s
```

This means if the call takes longer than 2 seconds, a `TimeoutException` is thrown【27†L112-L121】. Reflectoring shows exactly this pattern (annotate a method returning `CompletableFuture`, configure `timeoutDuration`)【27†L115-L123】【27†L120-L128】. If a timeout occurs, the circuit breaker or retry fallback would handle it. The sample output in the TimeLimiter guide shows a `TimeoutException` logged when a call exceeds the duration【27†L169-L177】.

We must use an asynchronous call (or wrap a synchronous call in `supplyAsync`). Alternatively, if using synchronous `RestTemplate`, one can configure the HTTP timeout in `application.yml` (`spring.rest.connection-timeout`, etc.) as an alternative.

## Logging and Monitoring  
We enable Spring Boot Actuator (via Gradle dependency) so we can expose metrics and health. Resilience4j automatically adds health indicators and metrics endpoints for Circuit Breakers and Retries. For example, `/actuator/health` will show Circuit Breaker state (`UP`/`DOWN`) if `registerHealthIndicator` is true【2†L129-L137】. We should log at each step (e.g. in fallbacks, retries) to trace flow. In production, we could hook these metrics into Prometheus/Grafana for real-time monitoring of failure rates and retry counts.

# Doctor Login & Appointment Workflow  
## Entities and Status Flow  
We define the following core entities (with Spring Data/JPA for MySQL):

- **Doctor**: `(id, name, specialty, email, passwordHash, availabilitySchedule, etc.)`. Marked `@Entity` for JPA. E.g.:
  ```java
  @Entity @Table(name="doctors")
  public class Doctor {
      @Id @GeneratedValue private Long id;
      private String name;
      private String specialty;
      // ... other fields, plus getters/setters
  }
  ```
- **Appointment**: `(id, doctorId, patientEmail, appointmentTime, status)`. Annotated `@Entity`:
  ```java
  @Entity @Table(name="appointments")
  public class Appointment {
      @Id @GeneratedValue private Long id;
      private Long doctorId;
      private String patientEmail;
      private LocalDateTime appointmentTime;
      @Enumerated(EnumType.STRING)
      private Status status;  // PENDING, CONFIRMED, REJECTED, COMPLETED, CANCELLED
      // ...
  }
  ```
  Here `Status` is an `enum`. The SQL schema (MySQL) might be:
  ```sql
  CREATE TABLE doctors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100),
    specialty VARCHAR(100)
    /* ... */
  );
  CREATE TABLE appointments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    doctor_id BIGINT,
    patient_email VARCHAR(100),
    appointment_time DATETIME,
    status VARCHAR(20),
    FOREIGN KEY (doctor_id) REFERENCES doctors(id)
  );
  ```
  (Alternatively, use JPA to auto-generate tables.)

- **Notification** (in Notification Service, stored in MongoDB): a document with fields like `(id, email, message, createdAt)`. For example:
  ```java
  @Document("notifications")
  public class Notification {
      @Id private String id;
      private String email;
      private String message;
      private LocalDateTime createdAt;
      // ...
  }
  ```

## Doctor Login API  
The Doctor Service exposes authentication endpoints. For simplicity (without full Spring Security setup), one could implement a `POST /login` that checks credentials against stored doctor records and returns a session token or success flag. (In production, use JWT or OAuth2). E.g.:

```java
@RestController @RequestMapping("/auth")
public class AuthController {
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        // validate doctor credentials, e.g. by email & password
        Doctor doc = doctorService.findByEmail(req.getEmail());
        if (doc != null && passwordMatches(req.getPassword(), doc.getPasswordHash())) {
            return ResponseEntity.ok(new AuthResponse("fake-jwt-token"));
        }
        return ResponseEntity.status(401).body("Invalid credentials");
    }
}
```
*(Use Lombok for getters/setters on `LoginRequest`, `AuthResponse`)*.

## Appointment Endpoints  
In **Appointment Service** (port 8081):
- `POST /api/appointments`: Patient books an appointment. Request body might include `doctorId`, `patientEmail`, `appointmentTime`. The controller saves an Appointment with status PENDING and calls `appointmentProducer.sendAppointmentCreated(evt)`. Example request:
  ```json
  {
    "doctorId": 42,
    "patientEmail": "alice@example.com",
    "appointmentTime": "2026-07-15T14:00:00"
  }
  ```
  Response returns the created appointment (with `status: PENDING`).

- `GET /api/appointments/{id}`: Get appointment details.

In **Doctor Service** (port 8082):
- `GET /api/doctors/{docId}/appointments/pending`: List all pending appointments for this doctor. It queries the `appointments` table (where `doctor_id=docId AND status='PENDING'`). Response is a JSON list of appointments.
- `POST /api/appointments/{id}/confirm`: Doctor confirms an appointment. The service updates status to CONFIRMED, publishes the Kafka event `appointment-confirmed`, and returns success.
- `POST /api/appointments/{id}/reject`: Similar, sets status to REJECTED and publishes `appointment-rejected`.
- (Optional) `POST /api/appointments/{id}/cancel`: If patient cancels before confirmation, status can be set to CANCELLED.

Flow: Initially appointments are **PENDING**. After doctor action, they become **CONFIRMED** or **REJECTED**. (Later, perhaps a scheduler or patient can complete/cancel). The Notification Service uses these events to notify the patient and doctor as needed.

## Appointment Statuses  
We enumerate statuses:
- **PENDING** (default on creation)
- **CONFIRMED** (doctor approved)
- **REJECTED** (doctor rejected)
- **COMPLETED** (after the appointment time has passed)
- **CANCELLED** (either patient cancelled before doctor action)
  
State transitions: `PENDING -> CONFIRMED/REJECTED`. From CONFIRMED, the patient may mark as CANCELLED, or it becomes COMPLETED after the scheduled time. These business rules can be enforced in services.

# Service Implementation Details  

## Folder Structure (per microservice)  
We follow clean layering. For example, **Appointment Service** might be:

```
src/
 └─ main/
     └─ java/com/hospital/appointmentsvc/
         ├─ controller/
         │   └─ AppointmentController.java
         ├─ service/
         │   └─ AppointmentService.java
         ├─ repository/
         │   └─ AppointmentRepository.java
         ├─ model/
         │   ├─ Appointment.java (@Entity)
         │   └─ Status.java (enum)
         ├─ dto/
         │   ├─ AppointmentRequest.java
         │   ├─ AppointmentResponse.java
         │   └─ AppointmentEvent.java (for Kafka)
         └─ config/
             ├─ KafkaConfig.java (if needed, or use auto-config)
             └─ ResilienceConfig.java (optional custom config)
```

Similarly, **Doctor Service**:
```
com/hospital/doctorservice/
    controller/ (DoctorController.java for login/appointments)
    service/ (DoctorService.java)
    repository/ (DoctorRepository.java, AppointmentRepository.java)
    model/ (Doctor.java, Appointment.java, Status.java, LoginRequest.java)
    dto/ (e.g. AuthResponse.java)
```

**Notification Service**:
```
com/hospital/notificationservice/
    listener/ (NotificationListener.java for Kafka @KafkaListener methods)
    service/ (NotificationService.java to save/send notifications)
    repository/ (NotificationRepository.java for MongoDB)
    model/ (Notification.java)
```

Each service has its own `Application.java` (SpringBootApplication). We use consistent package naming for beans.

## Gradle Dependencies  
In each service’s `build.gradle`, include:

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.x.x'
    id 'io.spring.dependency-management' version '1.0.15.RELEASE'
}

java { sourceCompatibility = '21' }

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'   // REST
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa' // JPA/MySQL
    implementation 'org.springframework.boot:spring-boot-starter-data-mongodb' // MongoDB (Notif svc)
    implementation 'org.springframework.kafka:spring-kafka'             // Kafka
    implementation 'io.github.resilience4j:resilience4j-spring-boot3:2.2.0' // Resilience4j
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-aop'
    compileOnly 'org.projectlombok:lombok:1.18.28'
    annotationProcessor 'org.projectlombok:lombok:1.18.28'
    runtimeOnly 'mysql:mysql-connector-java' // for MySQL
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

Each microservice adds only needed starters. For example, **Notification Service** may omit JPA and include Mongo starter, while **Doctor Service** and **Appointment Service** include JPA but might also have Mongo if needed. We rely on YAML config for DB connections.

## application.yml Samples  
### Appointment Service (application.yml)  
```yaml
server:
  port: 8081

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/appointmentdb
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

spring:
  kafka:
    bootstrap-servers: ${KAFKA_SERVERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

resilience4j:
  circuitbreaker:
    instances:
      doctorService:
        failureRateThreshold: 50
        slidingWindowSize: 5
        waitDurationInOpenState: 10s
  retry:
    instances:
      doctorRetry:
        maxRetryAttempts: 3
        waitDuration: 500ms

logging:
  level:
    root: INFO
```

### Doctor Service (application.yml)  
```yaml
server:
  port: 8082

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/doctordb
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update

spring:
  kafka:
    bootstrap-servers: ${KAFKA_SERVERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      group-id: "doctor-group"
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.value.default.type: com.hospital.dto.AppointmentEvent

resilience4j:
  circuitbreaker:
    instances:
      appointmentService:
        failureRateThreshold: 50
        slidingWindowSize: 5
        waitDurationInOpenState: 10s

logging:
  level:
    root: INFO
```

### Notification Service (application.yml)  
```yaml
server:
  port: 8083

spring:
  data:
    mongodb:
      host: localhost
      port: 27017
      database: notificationdb

spring:
  kafka:
    bootstrap-servers: ${KAFKA_SERVERS:localhost:9092}
    consumer:
      group-id: "notification-group"
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.value.default.type: com.hospital.dto.AppointmentEvent
        spring.json.trusted.packages: "*"

logging:
  level:
    root: INFO
```

Each service can share the same `KAFKA_SERVERS` environment variable. Note: Doctor and Notification services may not need Resilience4j if they are not calling other services, except Doctor Service calls the Appointment Service to update, but if appointments and doctors share one DB, that might be direct (no rest call needed).

## Entities, DTOs, Repositories  

- **Entity Classes:** As above, annotated with `@Entity`. Use Lombok `@Data` or `@Getter/@Setter` to reduce boilerplate.
- **DTOs:** For REST request/response and for Kafka events. E.g. `AppointmentRequest`, `AppointmentResponse` for APIs; `AppointmentEvent` (with only needed fields) for Kafka payloads.
- **Repositories:** Extend Spring Data interfaces. E.g. `public interface AppointmentRepository extends JpaRepository<Appointment,Long> { List<Appointment> findByDoctorIdAndStatus(Long doctorId, Status status); }`.
- **Mongo Repository:** `public interface NotificationRepository extends MongoRepository<Notification, String> {}`.

## REST Controllers  
Controllers map HTTP to service calls. Use proper annotations and return JSON. For example, in **DoctorController**:

```java
@RestController @RequestMapping("/api")
public class DoctorController {
    private final DoctorService doctorService;
    public DoctorController(DoctorService ds) { this.doctorService = ds; }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest req) {
        // ... authenticate
    }

    @GetMapping("/doctors/{id}/appointments/pending")
    public List<Appointment> getPending(@PathVariable Long id) {
        return doctorService.findPendingAppointments(id);
    }

    @PostMapping("/appointments/{id}/confirm")
    public Appointment confirm(@PathVariable Long id) {
        return doctorService.confirmAppointment(id);
    }
    
    @PostMapping("/appointments/{id}/reject")
    public Appointment reject(@PathVariable Long id) {
        return doctorService.rejectAppointment(id);
    }
}
```

Likewise, **AppointmentController** has `@PostMapping("/appointments")` for booking, etc. Use standard HTTP status codes (201 Created, 200 OK, 400/404 for errors). Include example requests in documentation or code comments.

# Running Locally (Windows)  

## Setting up Kafka on Windows  
Apache Kafka requires a JVM 17+. The *recommended* way to run Kafka on Windows is via **WSL2** (Windows Subsystem for Linux)【19†L191-L199】【22†L769-L776】. For development:

1. **Install Kafka:** Download the Kafka binary from the [official site](https://kafka.apache.org/downloads) (e.g. `kafka_2.13-3.5.0.tgz`) and extract it in WSL (or use the Confluent platform zip).  
2. **Initialize Kafka (KRaft mode):** In WSL, run:  
   ```
   bin/kafka-storage.sh random-uuid > uuid.txt
   bin/kafka-storage.sh format --cluster-id $(cat uuid.txt) --config config/kraft/server.properties
   bin/kafka-server-start.sh config/kraft/server.properties
   ```  
   (For older Kafka requiring Zookeeper, start Zookeeper first then start `kafka-server-start.sh`.)  
3. **Create topics:** In another terminal:  
   ```
   bin/kafka-topics.sh --create --topic appointment-created --partitions 1 --replication-factor 1 --bootstrap-server localhost:9092
   ```  
   Repeat for each topic (`appointment-confirmed`, `appointment-rejected`, `appointment-reminder`). The Spring Boot apps can also auto-create topics by defining a `NewTopic` bean.  
4. **Verify:** Use `kafka-console-producer.sh` and `kafka-console-consumer.sh` to test sending/receiving. For example, after creating `appointment-created`, run a producer and type a message to check it appears on a consumer. These steps are shown in the Confluent quickstart【22†L721-L730】【22†L739-L747】.

Confluent warns that running Kafka natively on Windows can be unreliable; WSL2 is more stable【22†L769-L776】. If WSL2 is not an option, one may run Kafka via Docker (though the requirement forbids Docker here) or on Linux/Mac. The key is to have a Kafka broker on `localhost:9092` matching your Spring configs.

## Starting the Services  
1. **Databases:** Start MySQL and MongoDB. Create schemas (`appointmentdb`, `doctordb`) or let Spring JPA auto-create tables (`ddl-auto: update`). For Mongo, ensure it runs on default port 27017.  
2. **Run Microservices:** In each project directory, run `./gradlew bootRun` (or use your IDE) to start the apps. They should log startup messages. Ensure `application.yml` ports do not clash (e.g. 8081, 8082, 8083).  
3. **Verify Connectivity:** Check that each service started without errors (particularly Kafka and DB connections). The logs should show KafkaTemplate or @KafkaListener initializing (or you may add log statements in `@PostConstruct`).  
4. **Scaling:** For local development, one instance of each is enough. In production, services would be containerized and scaled behind a load balancer.

# Testing & Flow Examples  

## Postman / API Flow  
1. **Book an Appointment:** POST to `http://localhost:8081/api/appointments` with JSON:
   ```json
   { "doctorId": 42, "patientEmail": "alice@example.com", "appointmentTime": "2026-07-15T14:00:00" }
   ```
   Response 201 with body `{ "id": 1001, "status": "PENDING", ... }`. The log or Kafka console (producer) should show that `AppointmentEvent` was sent to `appointment-created`.
2. **Doctor Login (if implemented):** POST to `http://localhost:8082/api/login` with doctor credentials. Receive a token or success.
3. **Get Pending Appointments:** GET `http://localhost:8082/api/doctors/42/appointments/pending`. The new appointment appears with status PENDING.
4. **Doctor Confirms:** POST `http://localhost:8082/api/appointments/1001/confirm`. On success, the appointment’s status is updated to CONFIRMED. The Doctor Service should publish `appointment-confirmed` to Kafka.
5. **Notification Triggered:** Notification Service’s listener on `appointment-confirmed` fires. It saves a `Notification` document in Mongo and (optionally) sends an email. You can verify by checking Mongo collection or logs.
6. **Simulate Failures:** To test resilience, you can temporarily shut down Doctor Service and attempt to book an appointment or call the doctor API. The Appointment Service’s circuit breaker should trip and return fallback responses after retries/failures.

## Example API Request/Response  
**Request:** Book appointment  
```
POST /api/appointments  
Content-Type: application/json

{
  "doctorId": 42,
  "patientEmail": "alice@example.com",
  "appointmentTime": "2026-07-15T14:00:00"
}
```  
**Response:**  
```json
{
  "id": 1001,
  "doctorId": 42,
  "patientEmail": "alice@example.com",
  "appointmentTime": "2026-07-15T14:00:00",
  "status": "PENDING"
}
```  
(Also, check Kafka: `appointment-created` topic receives a JSON event like `{"appointmentId":1001,"doctorId":42,...}`.)

# Production Considerations & Enhancements  

## Real-World Architecture  
In a production setup, one would further refine this design. For example:  
- **Service Discovery/Gateway:** Use a gateway (e.g. Spring Cloud Gateway) and a registry (Eureka) for routing and load-balancing between services.  
- **Security:** Implement OAuth2/JWT for API security, likely offloaded to an Auth Service.  
- **Databases:** Use managed cloud databases or clusters (MySQL, Mongo). Ensure each service has *its own* database (database-per-service pattern) to enforce loose coupling.  
- **Event Schema:** Use a Schema Registry (e.g. Confluent Schema Registry with Avro/JSON Schema) to manage event schemas across versions.  
- **Observability:** Integrate distributed tracing (e.g. OpenTelemetry), centralized logging (ELK/EFK), and metrics (Prometheus/Grafana). Resilience4j metrics should feed into dashboards.  
- **Scalability:** Kafka clusters (multi-broker, replication), and Kubernetes for container orchestration (even though initial requirement forbids Docker, in real world we would containerize).  
- **Cloud Services:** If not on-prem, use Kafka-as-a-service (Confluent Cloud) or AWS MSK, and managed DBs.

## Common Production Challenges  
- **Distributed Transactions:** Ensuring data consistency across services when an appointment status changes (potentially requiring Saga or compensation patterns).  
- **Idempotency:** Handling duplicate events (e.g. if Kafka retries deliver messages, or if a doctor double-clicks confirm). Use unique IDs and idempotent consumers.  
- **Ordering:** Kafka only guarantees order per partition. If ordering matters (e.g. processing confirmed/rejected in sequence), design partitioning keys appropriately.  
- **Error Handling:** Managing poison-pill messages in Kafka consumers (e.g. malformed events). Use dead-letter queues or skip logic.  
- **Monitoring:** Tracking consumer lag, broker health, circuit breaker state, memory usage.  
- **Versioning:** Rolling updates of services must be backward-compatible with message schemas.  
- **Partial Failures:** Network issues or partial outages where some services can’t connect (Circuit Breakers help mitigate).  
- **Security:** Secure Kafka topics (SSL/SASL), encrypt sensitive data.  

## Enhancements (Roadmap)  
- **Docker/Kubernetes:** Containerize each service and run on K8s (despite “no Docker” constraint, this is a natural evolution).  
- **API Gateway & Authentication:** Add a gateway with centralized auth. Use Spring Security with JWT.  
- **Event Store:** Persist all events (append-only log) for auditability or replay. Implement audit logging.  
- **Read Models:** Implement CQRS: separate read database (e.g. Elasticsearch) for dashboards or patient portals.  
- **GraphQL:** Build a GraphQL API for flexible frontend queries (e.g. patient apps).  
- **Machine Learning:** Use appointment data to predict no-shows and send dynamic reminders.  
- **Cache:** Add Redis caching for frequent reads (e.g. doctor availability).  
- **Bulkheads/Rate Limiting:** Use Resilience4j Bulkhead to isolate critical threads, and RateLimiter to throttle heavy traffic.  

# Key Points for Interviews and Best Practices  
- **Microservices & Kafka:** Emphasize decoupling: Kafka allows services to evolve independently; it acts as a durable message bus【31†L225-L234】. Highlight *At-Least-Once* delivery (consumer idempotency is needed) and topic design.  
- **Resilience Patterns:** Explain Circuit Breaker’s states and purpose (avoid cascading failures)【2†L45-L53】, and how fallback methods maintain UX. Discuss why Retry + Timeout are used for transient errors vs. letting Circuit Breaker handle systemic failures.  
- **Clean Architecture:** Packages are layered; each service has its own domain model and database. Controllers delegate to services which use repositories. DTOs separate API layer from internal models.  
- **Configuration Management:** YAML centralizes config. For secrets, use environment variables or config servers (not covered here).  
- **Logging & Monitoring:** Instrument logs with correlation IDs (for tracing a patient’s flow across services). Use Actuator and metrics for health checks.  
- **Error Simulation:** For demos, show how forcing Doctor Service to fail (e.g. by shutting it down or injecting errors) triggers the circuit breaker and logs retries.  
- **Deployment:** Although this guide runs locally, mention how CI/CD pipelines would build and deploy each microservice, run database migrations (Flyway/Liquibase), etc.  
- **Data Schema:** The MySQL tables (shown above) reflect normalized relations. In NoSQL (Mongo), schemas are flexible but still design collections (we use a “notifications” collection).  
- **Testing:** Write unit tests for services (mocking KafkaTemplate and RestTemplate). Integration tests can use Embedded Kafka and an in-memory DB (H2).  
- **Versioning:** If APIs evolve, use versioned endpoints (e.g. `/api/v1/...`).

Overall, this design demonstrates a **production-grade** microservices system: it uses asynchronous messaging (Kafka) for flexibility and resilience, and employs fault-tolerance (Resilience4j) to maintain uptime under partial failures. The code skeletons above serve as templates. Following **clean code** and **best practices** (layered architecture, descriptive YAML, solid exception handling) makes the system maintainable. For further study, consult the [Spring Boot Kafka documentation](https://docs.spring.io/spring-boot/reference/messaging/kafka.html)【42†L382-L390】【42†L416-L424】, the [Resilience4j guides](https://resilience4j.readme.io/)【8†L170-L178】【27†L115-L123】, and Kafka tutorials (e.g. Apache Kafka Quickstart【15†L83-L92】).

**Sources:** Official docs and guides for Kafka and Resilience4j were used (Spring Boot docs【42†L382-L390】【42†L416-L424】, Resilience4j examples【2†L45-L53】【8†L170-L178】【27†L115-L123】, Confluent Kafka setup guide【22†L769-L776】, etc.) to ensure accurate configuration and patterns.