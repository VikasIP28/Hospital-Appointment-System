# 🚀 Step-by-Step Guide to Run the Hospital Appointment System

This guide provides the exact steps needed to get the entire microservices architecture up and running on your local Windows machine.

---

## Step 1: Verify Prerequisites

### 1.1 Check Java 21
The project is built with Java 21. Since your system's default `JAVA_HOME` points to Java 17, we will need to explicitly use your Java 21 installation located at `C:\Java21` for all build and run commands.

### 1.2 Check MongoDB
Ensure MongoDB (version 7.x+) is installed and running on default port `27017`.
If it's not running as a Windows Service, start it manually:
```powershell
"C:\Program Files\MongoDB\Server\7.0\bin\mongod.exe"
```

---

## Step 2: Start Apache Kafka (Windows)

Kafka is required for the event-driven communication (Appointment → Notification).

### 2.1 Start Zookeeper
Open a **new PowerShell terminal**, navigate to your Kafka installation folder (e.g., `C:\kafka`), and run:
```powershell
D:\kafka_2.13-3.9.2
```
```
.\bin\windows\zookeeper-server-start.bat .\config\zookeeper.properties
```
*Leave this terminal open and running.*

### 2.2 Start Kafka Broker
Open a **second PowerShell terminal**, navigate to Kafka, and run:
```powershell
cd D:\kafka_2.13-3.9.2
```
```
.\bin\windows\kafka-server-start.bat .\config\server.properties
```
*Leave this terminal open and running.*

### 2.3 Create Required Kafka Topics
Open a **third PowerShell terminal**, navigate to Kafka, and run these commands one by one to create the topics:
```powershell
cd D:\kafka_2.13-3.9.2
```
```
.\bin\windows\kafka-topics.bat --create --topic appointment-created --bootstrap-server localhost:9092 --partitions 3
.\bin\windows\kafka-topics.bat --create --topic appointment-confirmed --bootstrap-server localhost:9092 --partitions 3
.\bin\windows\kafka-topics.bat --create --topic appointment-rejected --bootstrap-server localhost:9092 --partitions 3
.\bin\windows\kafka-topics.bat --create --topic appointment-reminder --bootstrap-server localhost:9092 --partitions 3
```

---

## Step 3: Build the Project

Open a new PowerShell terminal and navigate to the project root directory. You must set `JAVA_HOME` to your Java 21 installation before building.

```powershell
cd "d:\POC\phase 4\hospital-appointment-system"
$env:JAVA_HOME = "C:\Java21"
.\gradlew.bat build
```
*(Note: This compiles the code and runs tests. It should say `BUILD SUCCESSFUL`).*

---

## Step 4: Start the Microservices

You need to start all 5 microservices. It is best to open a **separate PowerShell terminal for each service** so you can monitor their logs independently. 

For **every** terminal, you must first navigate to the project root and set the Java 21 environment variable.

### Terminal 1: Auth Service (Port 8084)
```powershell
cd "d:\POC\phase 4\hospital-appointment-system"
$env:JAVA_HOME = "C:\Java21"
.\gradlew.bat :auth-service:bootRun
```

### Terminal 2: Doctor Service (Port 8082)
```powershell
cd "d:\POC\phase 4\hospital-appointment-system"
$env:JAVA_HOME = "C:\Java21"
.\gradlew.bat :doctor-service:bootRun
```

### Terminal 3: Appointment Service (Port 8081)
```powershell
cd "d:\POC\phase 4\hospital-appointment-system"
$env:JAVA_HOME = "C:\Java21"
.\gradlew.bat :appointment-service:bootRun
```

### Terminal 4: Notification Service (Port 8083)
```powershell
cd "d:\POC\phase 4\hospital-appointment-system"
$env:JAVA_HOME = "C:\Java21"
.\gradlew.bat :notification-service:bootRun
```
*(Note: Emails are logged to the console by default. To send real emails, update `notification-service/src/main/resources/application.yml` with your Gmail App Password and set `notification.email.enabled: true`).*

### Terminal 5: API Gateway (Port 8080)
```powershell
cd "d:\POC\phase 4\hospital-appointment-system"
$env:JAVA_HOME = "C:\Java21"
.\gradlew.bat :api-gateway:bootRun
```

---

## Step 5: Verify Services are Running

Once all terminals show that the application has started (look for `Started Application in X seconds`), you can verify their health by opening these URLs in your browser:

- API Gateway: http://localhost:8080/actuator/health
- Appointment Service: http://localhost:8081/actuator/health
- Doctor Service: http://localhost:8082/actuator/health
- Notification Service: http://localhost:8083/actuator/health
- Auth Service: http://localhost:8084/actuator/health

All should respond with `{"status":"UP"}`.

---

## Step 6: Test the System

1. Open **Postman**.
2. Import the collection located at: 
   `d:\POC\phase 4\hospital-appointment-system\postman\Hospital-Appointment-System.postman_collection.json`
3. The collection is pre-configured with the correct flow. Run the requests in order starting from Folder `1. Authentication`.
   - The Postman collection automatically extracts JWT tokens from login/register responses and sets them as variables (`admin_token`, `doctor_token`, `patient_token`) for subsequent requests.

### To Test Resilience4j (Circuit Breaker):
1. Create an appointment normally using Postman (Request 3.1). It will succeed.
2. Go to your Doctor Service terminal and press `Ctrl+C` to stop it.
3. Try to create another appointment (Request 3.1 or 6.1). 
4. The request will still succeed, but the appointment will be created with a fallback doctor name (`Unknown Doctor (Service Unavailable)`).
5. Check the Circuit Breaker status (Request 5.2) to see it transition to the `OPEN` state.
