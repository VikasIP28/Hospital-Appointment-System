# 🏥 Hospital Appointment System — Microservices Architecture

A **production-grade**, enterprise-level backend system built with **Java 21**, **Spring Boot 3.x**, and **Microservices Architecture**. The system manages hospital appointments end-to-end with event-driven communication, resilience patterns, and JWT-based authentication.

---

## 📋 Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Tech Stack](#tech-stack)
3. [Microservices](#microservices)
4. [Prerequisites](#prerequisites)
5. [MongoDB Setup](#mongodb-setup)
6. [Kafka Setup (Windows)](#kafka-setup-windows)
7. [Running the Application](#running-the-application)
8. [API Documentation](#api-documentation)
9. [End-to-End Flow](#end-to-end-flow)
10. [Kafka Event Flow](#kafka-event-flow)
11. [Resilience4j Patterns](#resilience4j-patterns)
12. [MongoDB Collections](#mongodb-collections)
13. [Postman Collection](#postman-collection)
14. [Production Discussion](#production-discussion)
15. [Interview Questions & Answers](#interview-questions--answers)
16. [Future Enhancements](#future-enhancements)

---

## 🏗️ Architecture Overview

```
                    ┌──────────────────────┐
                    │    API Gateway        │
                    │    (Port 8080)        │
                    └──────┬───────────────┘
                           │
          ┌────────────────┼────────────────┐
          │                │                │
   ┌──────▼──────┐  ┌─────▼──────┐  ┌──────▼──────┐  ┌──────────────┐
   │ Appointment  │  │  Doctor    │  │Notification │  │    Auth      │
   │  Service     │  │  Service   │  │  Service    │  │   Service    │
   │ (Port 8081)  │  │(Port 8082) │  │(Port 8083)  │  │ (Port 8084)  │
   └──────┬───────┘  └────────────┘  └──────▲──────┘  └──────────────┘
          │                                  │
          │         ┌──────────────┐         │
          └────────►│  Apache      ├─────────┘
                    │  Kafka       │
                    └──────────────┘
          
          ┌──────────────────────────────────┐
          │          MongoDB                  │
          │  auth_db | appointment_db |       │
          │  doctor_db | notification_db      │
          └──────────────────────────────────┘
```

### Communication Patterns
- **Synchronous**: Appointment Service → Doctor Service (via OpenFeign + Resilience4j)
- **Asynchronous**: Appointment Service → Kafka → Notification Service
- **Authentication**: All services validate JWT tokens with shared secret

---

## 🛠️ Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 21 | Language |
| Spring Boot | 3.2.5 | Framework |
| Gradle | 8.7 | Build tool |
| MongoDB | 7.x+ | NoSQL database |
| Apache Kafka | 3.x | Event streaming |
| Resilience4j | 2.2.0 | Circuit Breaker, Retry, TimeLimiter |
| JJWT | 0.11.5 | JWT authentication |
| Spring Cloud OpenFeign | 2023.0.1 | Declarative REST client |
| Spring Boot Actuator | 3.2.5 | Monitoring |
| Lombok | Latest | Boilerplate reduction |
| Spring Boot Mail | 3.2.5 | Email notifications |

---

## 🔧 Microservices

| Service | Port | Database | Description |
|---|---|---|---|
| **api-gateway** | 8080 | — | Routes requests, JWT validation |
| **appointment-service** | 8081 | appointment_db | Appointment CRUD, Kafka producer, Resilience4j |
| **doctor-service** | 8082 | doctor_db | Doctor profiles, availability, simulation |
| **notification-service** | 8083 | notification_db | Kafka consumer, email notifications |
| **auth-service** | 8084 | auth_db | Registration, login, JWT tokens |

---

## 📦 Prerequisites

1. **Java 21** — [Download](https://adoptium.net/)
2. **MongoDB 7.x+** — [Download](https://www.mongodb.com/try/download/community)
3. **Apache Kafka 3.x** — [Download](https://kafka.apache.org/downloads)
4. **Postman** (optional) — [Download](https://www.postman.com/downloads/)

Verify installations:
```bash
java --version    # Should show 21+
mongod --version  # Should show 7.x+
```

---

## 🍃 MongoDB Setup

### 1. Start MongoDB
```bash
# Windows (default installation)
"C:\Program Files\MongoDB\Server\7.0\bin\mongod.exe"

# Or if configured as a service, it starts automatically
```

### 2. MongoDB Collections

The databases are created automatically when each service first connects. Here are the collections and their schemas:

#### auth_db.users
```json
{
  "_id": "ObjectId",
  "email": "admin@hospital.com",
  "password": "$2a$10$...(BCrypt hash)",
  "name": "System Admin",
  "role": "ADMIN",
  "createdAt": "2026-05-20T10:00:00"
}
```
**Indexes**: `email` (unique)

#### appointment_db.appointments
```json
{
  "_id": "ObjectId",
  "patientName": "Jane Doe",
  "patientEmail": "jane.doe@email.com",
  "doctorId": "665...",
  "appointmentDate": "2026-06-15T10:00:00",
  "symptoms": "Chest pain and shortness of breath",
  "status": "PENDING",
  "createdAt": "2026-05-20T10:30:00"
}
```
**Status values**: `PENDING`, `CONFIRMED`, `REJECTED`, `COMPLETED`, `CANCELLED`

#### doctor_db.doctors
```json
{
  "_id": "ObjectId",
  "name": "Dr. John Smith",
  "specialization": "CARDIOLOGY",
  "email": "dr.john@hospital.com",
  "phone": "+1-555-0101",
  "availability": true,
  "createdAt": "2026-05-20T10:15:00"
}
```

#### notification_db.notifications
```json
{
  "_id": "ObjectId",
  "appointmentId": "665...",
  "recipientEmail": "jane.doe@email.com",
  "message": "Your appointment has been confirmed...",
  "notificationType": "APPOINTMENT_CONFIRMED",
  "emailSent": false,
  "sentAt": null,
  "createdAt": "2026-05-20T10:35:00"
}
```
**NotificationType values**: `APPOINTMENT_CREATED`, `APPOINTMENT_CONFIRMED`, `APPOINTMENT_REJECTED`, `APPOINTMENT_REMINDER`, `APPOINTMENT_CANCELLED`

---

## 🔥 Kafka Setup (Windows)

### 1. Download Kafka
Download Apache Kafka from https://kafka.apache.org/downloads (Binary download, Scala 2.13).

Extract to `C:\kafka` (or any preferred directory).

### 2. Start Zookeeper
```bash
cd C:\kafka
.\bin\windows\zookeeper-server-start.bat .\config\zookeeper.properties
```

### 3. Start Kafka Broker
Open a **new terminal**:
```bash
cd C:\kafka
.\bin\windows\kafka-server-start.bat .\config\server.properties
```

### 4. Create Topics
Open a **new terminal**:
```bash
cd C:\kafka

# Create topics with 3 partitions
.\bin\windows\kafka-topics.bat --create --topic appointment-created --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
.\bin\windows\kafka-topics.bat --create --topic appointment-confirmed --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
.\bin\windows\kafka-topics.bat --create --topic appointment-rejected --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
.\bin\windows\kafka-topics.bat --create --topic appointment-reminder --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
```

### 5. Verify Topics
```bash
.\bin\windows\kafka-topics.bat --list --bootstrap-server localhost:9092
```

### 6. Monitor Events (Optional)
```bash
# Watch appointment-created events
.\bin\windows\kafka-console-consumer.bat --topic appointment-created --from-beginning --bootstrap-server localhost:9092
```

---

## 🚀 Running the Application

### Step 1: Ensure MongoDB and Kafka are running

### Step 2: Build the project
```bash
cd hospital-appointment-system
.\gradlew.bat build
```

### Step 3: Start services (each in a separate terminal)

```bash
# Terminal 1: Auth Service
.\gradlew.bat :auth-service:bootRun

# Terminal 2: Doctor Service
.\gradlew.bat :doctor-service:bootRun

# Terminal 3: Appointment Service
.\gradlew.bat :appointment-service:bootRun

# Terminal 4: Notification Service
.\gradlew.bat :notification-service:bootRun

# Terminal 5: API Gateway
.\gradlew.bat :api-gateway:bootRun
```

### Step 4: Verify all services are healthy
```bash
curl http://localhost:8084/actuator/health  # Auth Service
curl http://localhost:8082/actuator/health  # Doctor Service
curl http://localhost:8081/actuator/health  # Appointment Service
curl http://localhost:8083/actuator/health  # Notification Service
curl http://localhost:8080/actuator/health  # API Gateway
```

---

## 📡 API Documentation

### Auth Service (Port 8084)
| Method | Endpoint | Role | Description |
|---|---|---|---|
| POST | `/auth/register` | Public | Register new user |
| POST | `/auth/login` | Public | Login & get JWT |
| GET | `/auth/validate?token=` | Public | Validate JWT token |

### Doctor Service (Port 8082)
| Method | Endpoint | Role | Description |
|---|---|---|---|
| POST | `/doctors` | DOCTOR/ADMIN | Create doctor profile |
| GET | `/doctors` | Any | List all doctors |
| GET | `/doctors/{id}` | Any | Get doctor by ID |
| GET | `/doctors/availability/{id}` | Any | Check availability |
| GET | `/doctors/simulate/slow` | Public | Simulate slow response (5s) |
| GET | `/doctors/simulate/failure` | Public | Simulate service failure |
| GET | `/doctors/simulate/random` | Public | Simulate random failure (50%) |
| GET | `/admin/analytics/doctors` | ADMIN | Doctor workload analytics |

### Appointment Service (Port 8081)
| Method | Endpoint | Role | Description |
|---|---|---|---|
| POST | `/appointments` | PATIENT | Create appointment |
| GET | `/appointments` | Any auth | List all appointments |
| GET | `/appointments/{id}` | Any auth | Get appointment by ID |
| PUT | `/appointments/{id}/confirm` | DOCTOR/ADMIN | Confirm appointment |
| PUT | `/appointments/{id}/reject` | DOCTOR/ADMIN | Reject appointment |
| PUT | `/appointments/{id}/cancel` | Any auth | Cancel appointment |
| GET | `/appointments/doctor/{id}/pending` | DOCTOR/ADMIN | Pending appointments |
| GET | `/admin/system/health` | ADMIN | System health |
| GET | `/admin/system/circuit-breakers` | ADMIN | Circuit breaker states |
| GET | `/admin/system/retries` | ADMIN | Retry metrics |
| GET | `/admin/analytics/appointments` | ADMIN | Appointment statistics |
| GET | `/admin/kafka/events` | ADMIN | Recent Kafka events |

### Notification Service (Port 8083)
| Method | Endpoint | Role | Description |
|---|---|---|---|
| GET | `/notifications` | Any auth | All notifications |
| GET | `/notifications/appointment/{id}` | Any auth | By appointment ID |
| GET | `/admin/notifications` | ADMIN | All notifications (admin) |
| GET | `/admin/notifications/failed` | ADMIN | Failed notifications |
| POST | `/admin/notifications/{id}/resend` | ADMIN | Resend notification |
| GET | `/admin/notifications/report` | ADMIN | Delivery report |

---

## 🔄 End-to-End Flow

### Flow 1: Patient Books Appointment
1. Patient calls `POST /auth/register` (role: PATIENT) → gets JWT token
2. Patient calls `POST /appointments` with JWT → appointment-service
3. appointment-service calls doctor-service via Feign (with Resilience4j)
4. Appointment saved with status `PENDING` in MongoDB
5. Kafka event published to `appointment-created` topic
6. notification-service consumes event → creates notification → sends email

### Flow 2: Doctor Confirms Appointment
1. Doctor calls `POST /auth/login` → gets JWT token
2. Doctor calls `GET /appointments/doctor/{id}/pending` → sees pending list
3. Doctor calls `PUT /appointments/{id}/confirm` → status changes to `CONFIRMED`
4. Kafka event published to `appointment-confirmed` topic
5. notification-service consumes event → notifies patient

### Flow 3: Doctor Rejects Appointment
1. Doctor calls `PUT /appointments/{id}/reject` → status changes to `REJECTED`
2. Kafka event published to `appointment-rejected` topic
3. notification-service consumes event → notifies patient

### Flow 4: Appointment Reminder
1. Scheduler runs every 1 hour
2. Finds CONFIRMED appointments within next 24 hours
3. Publishes events to `appointment-reminder` topic
4. notification-service consumes events → sends reminder emails

---

## 📨 Kafka Event Flow

```
┌──────────────────┐    appointment-created     ┌────────────────────┐
│                  │ ─────────────────────────► │                    │
│   Appointment    │    appointment-confirmed   │   Notification     │
│   Service        │ ─────────────────────────► │   Service          │
│                  │    appointment-rejected    │                    │
│   (Producer)     │ ─────────────────────────► │   (Consumer)       │
│                  │    appointment-reminder    │                    │
│                  │ ─────────────────────────► │                    │
└──────────────────┘                            └────────────────────┘
```

### Kafka Concepts Used
- **Serialization**: `JsonSerializer` converts AppointmentEvent to JSON bytes
- **Deserialization**: `JsonDeserializer` converts JSON bytes back to AppointmentEvent
- **Consumer Groups**: `notification-group` ensures each event is processed once
- **Topic Partitions**: 3 partitions per topic for parallelism
- **At-Least-Once Delivery**: Kafka guarantees events are delivered at least once
- **Idempotency**: Notification service checks for duplicate events by appointmentId

---

## 🛡️ Resilience4j Patterns

### Circuit Breaker (appointment-service → doctor-service)
```
State Transitions:
  CLOSED ──(50% failure rate)──► OPEN ──(10s wait)──► HALF_OPEN
    ▲                                                       │
    └──────────────(success)────────────────────────────────┘
    
CLOSED:    All calls go through (monitoring failure rate)
OPEN:      All calls are blocked (returns fallback immediately)
HALF_OPEN: Limited calls (3) to test if service recovered
```

**Configuration**:
- Sliding window: 10 calls (COUNT_BASED)
- Failure threshold: 50%
- Wait in OPEN: 10 seconds
- Permitted calls in HALF_OPEN: 3
- Minimum calls before evaluation: 5

### Retry
- Max attempts: 3
- Wait duration: 2 seconds
- Exponential backoff multiplier: 2 (2s, 4s, 8s)

### Testing Resilience4j
1. Start all services normally
2. Create an appointment (should work fine)
3. **Stop doctor-service**
4. Create another appointment → observe fallback response
5. Check circuit breaker state: `GET /admin/system/circuit-breakers`
6. **Restart doctor-service**
7. Wait 10 seconds for circuit to transition to HALF_OPEN
8. Create appointment → circuit should close

---

## 📬 Postman Collection

Import `postman/Hospital-Appointment-System.postman_collection.json` into Postman.

### Test Execution Order:
1. Register Admin → auto-saves `admin_token`
2. Register Doctor → auto-saves `doctor_token`
3. Register Patient → auto-saves `patient_token`
4. Create Doctor Profile → auto-saves `doctor_id`
5. Create Appointment → auto-saves `appointment_id`
6. View Pending Appointments (as Doctor)
7. Confirm Appointment (as Doctor)
8. View Notifications
9. Check Admin Analytics
10. Test Circuit Breaker (stop doctor-service first)

---

## 🏭 Production Discussion

### Why Kafka?
Kafka provides **asynchronous, decoupled communication** between microservices. The appointment service doesn't need to wait for notifications to be sent — it publishes events and moves on.

### Eventual Consistency
The system embraces eventual consistency. When an appointment is created, the notification might not exist immediately in the notification database. It will be consistent "eventually" once the Kafka consumer processes the event.

### Distributed Transactions
Instead of distributed transactions (2PC), we use the **Saga pattern** via Kafka events. Each service manages its own data, and compensating transactions handle failures.

### Scaling Strategy
- Each service can scale independently
- Kafka partitions allow parallel event processing
- MongoDB supports horizontal scaling via sharding
- Stateless services (JWT) allow easy horizontal scaling

### Dead Letter Queue (DLQ)
In production, failed Kafka messages should be sent to a Dead Letter Topic for later investigation and reprocessing.

### API Gateway Usage
The API Gateway provides:
- Single entry point for all clients
- JWT validation at the edge
- Request routing and load balancing
- CORS handling

### Observability
- Spring Boot Actuator for health checks and metrics
- SLF4J structured logging across all services
- Circuit breaker monitoring endpoints

---

## 🎓 Interview Questions & Answers

### Q1: Why use microservices instead of a monolith?
**A**: Microservices allow independent deployment, scaling, and technology choices per service. Each team can own a service. In this system, the notification service can scale independently during high appointment volume.

### Q2: How does the circuit breaker pattern work?
**A**: When the Doctor Service fails repeatedly (50% failure rate), the circuit breaker transitions from CLOSED to OPEN, stopping all calls for 10 seconds. This prevents cascading failures. After 10 seconds, it moves to HALF_OPEN, allowing 3 test calls to check if the service has recovered.

### Q3: Why Kafka over RabbitMQ?
**A**: Kafka provides higher throughput, message persistence, replayability, and better support for event sourcing. It's ideal for event-driven architectures where consumers might need to replay events.

### Q4: How is eventual consistency handled?
**A**: We accept that data across services may be temporarily inconsistent. Kafka guarantees at-least-once delivery, and we implement idempotency in consumers to handle duplicate messages.

### Q5: How does JWT authentication work across services?
**A**: The auth-service generates JWT tokens containing user email and role. All other services share the same secret key and validate tokens independently without calling auth-service again — this is stateless authentication.

### Q6: What happens if Kafka is down?
**A**: Appointment creation still succeeds (saved to MongoDB). The Kafka event publish will fail, but the appointment is not lost. In production, we'd implement a retry mechanism or outbox pattern.

### Q7: How would you implement idempotency?
**A**: Use the appointmentId as a deduplication key. Before processing a Kafka event, check if a notification with that appointmentId and eventType already exists in MongoDB. If so, skip processing.

### Q8: What is the Outbox Pattern?
**A**: Instead of publishing to Kafka directly, save the event to a local "outbox" table in the same database transaction as the business data. A separate process polls the outbox and publishes to Kafka. This ensures atomicity between data persistence and event publishing.

### Q9: How would you add rate limiting?
**A**: Use Spring Cloud Gateway rate limiter, or implement a custom filter using Resilience4j's RateLimiter. In production, use Redis-backed rate limiting for distributed environments.

### Q10: How would you handle service discovery?
**A**: In production, use Netflix Eureka or Consul for service discovery. Services register themselves, and the API gateway looks up service locations dynamically instead of using hardcoded URLs.

---

## 🚀 Future Enhancements

1. **Docker & Docker Compose** — Containerize all services
2. **Kubernetes** — Orchestrate containers with K8s
3. **Spring Cloud Config** — Centralized configuration management
4. **Eureka/Consul** — Service discovery
5. **Zipkin/Jaeger** — Distributed tracing
6. **ELK Stack** — Centralized logging (Elasticsearch, Logstash, Kibana)
7. **Prometheus + Grafana** — Metrics and dashboards
8. **Redis** — Caching layer and rate limiting
9. **WebSocket** — Real-time notification delivery
10. **OAuth2/OpenID Connect** — Advanced authentication
11. **API Versioning** — Version management for REST APIs
12. **Database Migrations** — Schema versioning with Mongock
13. **Integration Tests** — End-to-end test suites
14. **CI/CD Pipeline** — Automated build, test, and deploy

---

## 📁 Project Structure

```
hospital-appointment-system/
├── settings.gradle
├── build.gradle
├── gradlew.bat
├── README.md
├── postman/
│   └── Hospital-Appointment-System.postman_collection.json
│
├── auth-service/           (Port 8084)
│   ├── build.gradle
│   └── src/main/
│       ├── resources/application.yml
│       └── java/com/hospital/auth/
│           ├── AuthServiceApplication.java
│           ├── config/     (SecurityConfig, MongoConfig)
│           ├── controller/ (AuthController)
│           ├── dto/        (LoginRequest, RegisterRequest, AuthResponse)
│           ├── entity/     (User)
│           ├── enums/      (Role)
│           ├── exception/  (GlobalExceptionHandler, AuthException, etc.)
│           ├── repository/ (UserRepository)
│           ├── security/   (JwtTokenProvider, JwtAuthFilter, UserDetailsService)
│           ├── service/    (AuthService, impl/)
│           └── util/       (AppConstants)
│
├── doctor-service/         (Port 8082)
│   ├── build.gradle
│   └── src/main/
│       ├── resources/application.yml
│       └── java/com/hospital/doctor/
│           ├── DoctorServiceApplication.java
│           ├── config/     (SecurityConfig, MongoConfig)
│           ├── controller/ (DoctorController, AdminDoctorController)
│           ├── dto/        (DoctorRequest, DoctorResponse, DoctorAvailabilityResponse)
│           ├── entity/     (Doctor)
│           ├── enums/      (Specialization)
│           ├── exception/  (GlobalExceptionHandler, DoctorNotFoundException)
│           ├── repository/ (DoctorRepository)
│           ├── security/   (JwtTokenProvider, JwtAuthFilter)
│           ├── service/    (DoctorService, impl/)
│           └── util/       (AppConstants)
│
├── appointment-service/    (Port 8081)
│   ├── build.gradle
│   └── src/main/
│       ├── resources/application.yml
│       └── java/com/hospital/appointment/
│           ├── AppointmentServiceApplication.java
│           ├── client/     (DoctorServiceClient, DoctorServiceFallback)
│           ├── config/     (SecurityConfig, MongoConfig, KafkaConfig, etc.)
│           ├── controller/ (AppointmentController, AdminController)
│           ├── dto/        (AppointmentRequest, AppointmentResponse, DoctorResponse)
│           ├── entity/     (Appointment)
│           ├── enums/      (AppointmentStatus)
│           ├── exception/  (GlobalExceptionHandler, custom exceptions)
│           ├── kafka/      (event/AppointmentEvent, producer/AppointmentEventProducer)
│           ├── mapper/     (AppointmentMapper)
│           ├── repository/ (AppointmentRepository)
│           ├── scheduler/  (AppointmentReminderScheduler)
│           ├── security/   (JwtTokenProvider, JwtAuthFilter)
│           ├── service/    (AppointmentService, impl/)
│           └── util/       (AppConstants)
│
├── notification-service/   (Port 8083)
│   ├── build.gradle
│   └── src/main/
│       ├── resources/application.yml
│       └── java/com/hospital/notification/
│           ├── NotificationServiceApplication.java
│           ├── config/     (SecurityConfig, MongoConfig, KafkaConfig)
│           ├── controller/ (NotificationController, AdminNotificationController)
│           ├── dto/        (NotificationResponse)
│           ├── entity/     (Notification)
│           ├── enums/      (NotificationType)
│           ├── exception/  (GlobalExceptionHandler, NotificationNotFoundException)
│           ├── kafka/      (event/AppointmentEvent, consumer/AppointmentEventConsumer)
│           ├── repository/ (NotificationRepository)
│           ├── security/   (JwtTokenProvider, JwtAuthFilter)
│           ├── service/    (NotificationService, impl/, EmailService, EmailServiceImpl)
│           └── util/       (AppConstants)
│
└── api-gateway/            (Port 8080)
    ├── build.gradle
    └── src/main/
        ├── resources/application.yml
        └── java/com/hospital/gateway/
            ├── ApiGatewayApplication.java
            ├── config/     (SecurityConfig, CorsConfig, GatewayRoutingConfig)
            ├── controller/ (GatewayController, FallbackController)
            ├── exception/  (GlobalExceptionHandler, ErrorResponse)
            ├── security/   (JwtTokenProvider, JwtAuthFilter)
            └── util/       (AppConstants)
```

---

## 📄 License

This project is developed for educational and demonstration purposes.
