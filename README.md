# Patient Management System

Patient Management System is a scalable, microservices-based backend that manages patient data and related workflows.  
It is built using Spring Boot and follows modern backend engineering practices including secure authentication, asynchronous event-driven communication, API gateway routing, and cloud-native deployment.

---

## Tech Stack

- **Language:** Java 17
- **Framework:** Spring Boot (Microservices)
- **API Gateway:** Spring Cloud Gateway
- **Authentication:** JWT (JSON Web Token)
- **Messaging:** Apache Kafka
- **Database:** PostgreSQL
- **Inter-service Communication:** REST, gRPC
- **Containerization:** Docker
- **Deployment:** AWS ECS

---

## Microservices

- **Patient Service** – Manages patient CRUD operations
- **Auth Service** – Handles authentication and JWT token generation
- **Billing Service** – Manages billing workflows and accounts
- **Analytics Service** – Consumes Kafka events for analytics
- **API Gateway** – Central entry point and request routing

---

## Repository Structure

```text
patient-repository/
 ├── analytics-service/
 ├── api-gateway/
 ├── auth-service/
 ├── billing-service/
 ├── patient-service/
 ├── integration-test/
 ├── Docker/
 ├── infrastructure/
 ├── api-requests/
 ├── README.md
