
# Hospital Appointment System - Microservices Architecture

**Project Name: Hospital Appointment System**


A production-style backend microservices project built using Java, Spring Boot, Apache Kafka, and Resilience4j to demonstrate event-driven communication, fault tolerance, retry mechanisms, timeout handling, and distributed system concepts.

---

# 🚀 Project Overview

This project simulates a real-world Hospital Appointment Management System where patients can book appointments, doctors can confirm/reject appointments, and notification services process appointment events asynchronously using Apache Kafka.

The system is designed using a microservices architecture and demonstrates enterprise backend concepts such as:

- Event-Driven Architecture
- Kafka Producer & Consumer
- Circuit Breaker Pattern
- Retry Mechanism
- Timeout Handling
- Fallback Methods
- REST API Communication
- Failure Simulation
- Distributed System Design

---

# 🏗️ Microservices

## 1. Appointment Service
Responsible for:
- Booking appointments
- Publishing Kafka events
- Calling Doctor Service
- Applying Circuit Breaker, Retry, and Timeout

### Port
`8081`

---

## 2. Doctor Service
Responsible for:
- Doctor login
- Appointment approval/rejection
- Doctor availability validation
- Simulating service failures

### Port
`8082`

---

## 3. Notification Service
Responsible for:
- Consuming Kafka events
- Sending appointment confirmations
- Sending reminders and notifications

### Port
`8083`

---

# 🔥 Key Features

## ✅ Event-Driven Communication using Kafka

- Kafka Producer implementation
- Kafka Consumer implementation
- Asynchronous communication between services
- Multiple Kafka topics

### Kafka Topics
- `appointment-created`
- `appointment-confirmed`
- `appointment-rejected`
- `appointment-reminder`

---

## ✅ Circuit Breaker using Resilience4j

- Prevents cascading failures
- Handles Doctor Service downtime
- Includes fallback methods
- Demonstrates:
  - CLOSED state
  - OPEN state
  - HALF_OPEN state

---

## ✅ Retry Mechanism

- Automatically retries failed requests
- Configurable retry attempts
- Retry wait durations

---

## ✅ Timeout Handling

- Prevents long-running service calls
- Improves system responsiveness
- Configurable timeout durations

---

## ✅ Failure Simulation

Doctor Service intentionally simulates:
- Random failures
- Delayed responses
- Service downtime

Used to demonstrate:
- Retry behavior
- Circuit breaker activation
- Timeout handling
- Fallback execution

---

# 🛠️ Tech Stack

| Technology | Version |
|---|---|
| Java | 21 |
| Spring Boot | 3.x |
| Gradle | Latest |
| Apache Kafka | 4.x |
| MongoDb | 8.x |
| Resilience4j | Latest |
| Spring Web | REST APIs |
| Spring Data MongoDb | Nosql |
| Lombok | Boilerplate reduction |

---

# 📂 Project Structure

```bash
hospital-appointment-system/
│
├── appointment-service
│
├── doctor-service
│
├── notification-service
│
└── common-dto
```

---

# 🔄 System Workflow

```text
Patient Books Appointment
        ↓
Appointment Service
        ↓
Calls Doctor Service
(Circuit Breaker + Retry + Timeout)
        ↓
Appointment Saved
        ↓
Kafka Producer Publishes Event
        ↓
Notification Service Consumes Event
        ↓
Confirmation Notification Sent
```

---

# 📌 Appointment Status Flow

```text
PENDING
CONFIRMED
REJECTED
COMPLETED
CANCELLED
```

---

# 📡 REST APIs

## Appointment Service

| Method | Endpoint | Description |
|---|---|---|
| POST | `/appointments/book` | Book appointment |
| GET | `/appointments/{id}` | Get appointment |

---

## Doctor Service

| Method | Endpoint | Description |
|---|---|---|
| POST | `/doctors/login` | Doctor login |
| GET | `/doctors/appointments/pending` | View pending appointments |
| PUT | `/doctors/appointments/{id}/confirm` | Confirm appointment |
| PUT | `/doctors/appointments/{id}/reject` | Reject appointment |

---

# ⚙️ Resilience4j Features

- Circuit Breaker
- Retry
- Timeout


---

---

# ▶️ Running the Project

## Start Kafka

```bash
bin/windows/zookeeper-server-start.sh config/zookeeper.properties
```

```bash
bin/windows/kafka-server-start.sh config/server.properties
```

---

## Start Services

Run each microservice separately:

- Appointment Service → `8081`
- Doctor Service → `8082`
- Notification Service → `8083`

---

# 🧪 Testing Flow

## 1. Book Appointment

```http
POST /appointments/book
```

Status:
```text
PENDING
```

---

## 2. Doctor Confirms Appointment

```http
PUT /doctors/appointments/{id}/confirm
```

Status:
```text
CONFIRMED
```

---

## 3. Notification Service Consumes Event

Kafka consumer processes:
```text
appointment-confirmed
```

---

# 📚 Concepts Demonstrated

- Microservices Architecture
- Distributed Systems
- Event-Driven Communication
- Apache Kafka
- Kafka Producer & Consumer
- Circuit Breaker Pattern
- Retry Pattern
- Timeout Handling
- Fault Tolerance
- REST Communication
- Failure Recovery
- Asynchronous Processing

---

# 🚀 Future Enhancements

- API Gateway
- Eureka Service Registry
- JWT Authentication
