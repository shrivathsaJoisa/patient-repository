# Patient Management System

Patient Management System is a scalable, microservices-based backend application designed to manage patient data and related workflows.  
It follows modern backend engineering practices using Spring Boot microservices, secure authentication, event-driven architecture, and cloud-native deployment.

---

## 🛠 Tech Stack

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

## 🧩 Microservices Overview

- **Patient Service**
  - Handles patient CRUD operations
  - Publishes patient-related events to Kafka

- **Auth Service**
  - Handles user authentication
  - Generates and validates JWT tokens

- **Billing Service**
  - Manages billing accounts and workflows
  - Consumes patient events

- **Analytics Service**
  - Consumes Kafka events
  - Processes analytical data

- **API Gateway**
  - Single entry point for all client requests
  - Routes requests to respective microservices
  - Validates JWT tokens

---

## 📁 Repository Structure

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
```

# System Architecture Overview

This document describes the high-level architecture of the Patient Management System, built using a microservices-based approach.

---

## 🌐 High-Level Request Flow

```text
Client
  ↓
API Gateway
  ↓
Microservices (Patient / Billing / Auth / Analytics)
  ↓
PostgreSQL Database
  ↓
Patient Service
  ↓
Kafka Topics
  ↓
Billing & Analytics Services
```
## 🧱 Architecture Principles

- Loose coupling between services  
- Independent scalability  
- Fault isolation  
- Event-driven communication  
- Stateless authentication  

---

## 🔐 Authentication & Security (JWT)

### Authentication Flow

```text
Client
  ↓
Auth Service (Login Request)
  ↓
Validate Credentials
  ↓
Generate JWT Token
  ↓
Return Token to Client
  ↓
Client sends requests with JWT in Authorization Header
  ↓
API Gateway validates JWT and routes request
```

## 🔁 Kafka Event Flow

### Example: Patient Creation

- Patient Service creates a patient  
- `PatientCreatedEvent` is published to Kafka  
- Billing Service consumes the event  
- Analytics Service consumes the event  

---

## ✅ Advantages

- Asynchronous processing  
- Decoupled services  
- Improved fault tolerance  
- Better scalability  

---

## 🚀 Getting Started

### Prerequisites

- Java JDK 17+  
- Maven  
- Docker & Docker Compose  
- Apache Kafka  
- PostgreSQL  
## ▶️ Running the Application Locally

### Option 1: Run via IDE

- Import each service as a Maven project
- Run the main Spring Boot application class for each service

### Option 2: Docker (Recommended)

```bash
docker compose up --build
```

- API Gateway: http://localhost:8080
- Swagger UI (per service): /swagger-ui.html

## 🐳 Docker Support

- Each microservice includes a Dockerfile
- Services can be containerized independently

```bash
docker build -t patient-service .
docker run patient-service
```
## ☁️ Deployment (AWS ECS)

- Docker images are pushed to Amazon ECR
- ECS Task Definitions are created per service
- Services are deployed using ECS with Load Balancers
- Environment variables are configured in ECS

### Benefits

- Horizontal scaling
- Managed infrastructure
- High availability
- Fault tolerance

### Testing

- Integration tests validate inter-service communication
- Kafka event flow testing ensures async processing
- API endpoints can be tested using Postman or Swagger

