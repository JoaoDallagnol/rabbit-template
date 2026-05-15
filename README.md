# 🐰 Rabbit Template - RabbitMQ Event-Driven Architecture

An educational project that demonstrates the main patterns of integration with **RabbitMQ**, including Topic Exchange, Fanout Exchange, Retry with Exponential Backoff, Dead Letter Queue (DLQ) and Idempotency.

## 📋 Table of Contents

- [What is this project?](#what-is-this-project)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [How to Run](#how-to-run)
- [API Endpoints](#api-endpoints)
- [Processing Flows](#processing-flows)
- [Project Structure](#project-structure)
- [Technologies](#technologies)

---

## 🎯 What is this project?

This is an **educational project** that implements an order processing system with different RabbitMQ integration scenarios:

### **Implemented Scenarios:**

1. **Topic Exchange** - Selective routing based on routing keys
2. **Fanout Exchange** - Broadcast to multiple listeners
3. **Retry with Exponential Backoff** - Automatic retries with increasing delay
4. **Dead Letter Queue (DLQ)** - Storage of messages that failed
5. **Idempotency** - Prevention of duplicate processing

### **Use Case:**

A customer creates an order via API. The system publishes an event that is consumed by different services (payment, notification, etc.) asynchronously. If something fails, the system automatically retries. If it fails permanently, the message goes to a dead letter queue for analysis.

---

## 🏗️ Architecture

### **Topic Exchange Flow (Selective Routing)**

```
POST /orders/topic
    ↓
OrderService.createOrderTopic()
    ↓
Saves Order to database
    ↓
Publishes OrderCreatedEvent
    ↓
orders.exchange (routing key: "orders.created")
    ↓
payment.queue.topic
    ↓
PaymentListener (processes payment)
    ↓
IdempotencyService (prevents duplication)
    ↓
ACK (confirms consumption)
```

### **Fanout Exchange Flow (Broadcast)**

```
POST /orders/fanout
    ↓
OrderService.createOrderFanout()
    ↓
Saves Order to database
    ↓
Publishes OrderCreatedEvent
    ↓
orders.fanout.exchange (no routing key)
    ↓
├─ payment.queue.fanout → PaymentListener
└─ notification.queue.fanout → NotificationListener
    ↓
Both process in parallel
    ↓
IdempotencyService (each listener has its own record)
    ↓
ACK (confirms consumption)
```

### **Retry and DLQ Flow**

```
Listener processes message
    ↓
❌ Fails
    ↓
Waits 1s (initial delay)
    ↓
Tries again (Retry 1)
    ↓
❌ Fails again
    ↓
Waits 2s (exponential backoff)
    ↓
Tries again (Retry 2)
    ↓
❌ Fails on 3rd attempt
    ↓
Goes to DLQ (Dead Letter Queue)
    ↓
DLQListener (logs error for analysis)
```

---

## 📦 Prerequisites

- **Java 21+**
- **Maven 3.8+**
- **RabbitMQ 3.x** (with Management Plugin)
- **Docker** (optional, to run RabbitMQ)

---

## 🚀 How to Run

### **1. Start RabbitMQ with Docker**

Use the provided docker-compose:

```bash
version: "3.8"
services:
  rabbitmq:
    image: rabbitmq:3-management
    container_name: rabbitmq
    restart: no
    ports:
      - "5672:5672"    # Default
      - "15672:15672"  # Web Admin
    environment:
      RABBITMQ_DEFAULT_USER: admin
      RABBITMQ_DEFAULT_PASS: admin123
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq
    networks:
      - microservices_net

volumes:
  rabbitmq_data:

networks:
  microservices_net:
    driver: bridge
```

**Save as `docker-compose.yml` and execute:**

```bash
docker-compose up -d
```

**Check if it's running:**

```bash
docker ps | grep rabbitmq
```

**Access Management Console:**

- URL: `http://localhost:15672`
- Username: `admin`
- Password: `admin123`

---

### **2. Compile the Project**

```bash
mvn clean install
```

---

### **3. Run the Application**

```bash
mvn spring-boot:run
```

Or execute the main class:

```bash
java -jar target/rabbit-template-0.0.1-SNAPSHOT.jar
```

**Check if it's running:**

```bash
curl http://localhost:8080/orders
```

---

## 📡 API Endpoints

### **1. Create Order via Topic Exchange**

```bash
POST http://localhost:8080/orders/topic
Content-Type: application/json

{
  "customerId": "CUST-001",
  "amount": 150.50,
  "items": [
    {
      "productId": "PROD-001",
      "quantity": 2
    }
  ]
}
```

**Response (201 Created):**

```json
{
  "orderId": "550e8400-e29b-41d4-a716-446655440000",
  "customerId": "CUST-001",
  "amount": 150.50,
  "status": "CREATED",
  "createdAt": "2026-05-12T15:30:00"
}
```

---

### **2. Create Order via Fanout Exchange**

```bash
POST http://localhost:8080/orders/fanout
Content-Type: application/json

{
  "customerId": "CUST-002",
  "amount": 250.00,
  "items": [
    {
      "productId": "PROD-002",
      "quantity": 1
    }
  ]
}
```

**Response (201 Created):**

```json
{
  "orderId": "550e8400-e29b-41d4-a716-446655440001",
  "customerId": "CUST-002",
  "amount": 250.00,
  "status": "CREATED",
  "createdAt": "2026-05-12T15:35:00"
}
```

---

### **3. List All Orders**

```bash
GET http://localhost:8080/orders
```

**Response (200 OK):**

```json
[
  {
    "orderId": "550e8400-e29b-41d4-a716-446655440000",
    "customerId": "CUST-001",
    "amount": 150.50,
    "status": "CREATED",
    "createdAt": "2026-05-12T15:30:00"
  },
  {
    "orderId": "550e8400-e29b-41d4-a716-446655440001",
    "customerId": "CUST-002",
    "amount": 250.00,
    "status": "CREATED",
    "createdAt": "2026-05-12T15:35:00"
  }
]
```

---

### **4. Get Order by ID**

```bash
GET http://localhost:8080/orders/550e8400-e29b-41d4-a716-446655440000
```

**Response (200 OK):**

```json
{
  "orderId": "550e8400-e29b-41d4-a716-446655440000",
  "customerId": "CUST-001",
  "amount": 150.50,
  "status": "CREATED",
  "createdAt": "2026-05-12T15:30:00"
}
```

---

## 🔄 Processing Flows

### **Topic Exchange - PaymentListener**

1. Receives event via `payment.queue.topic`
2. Checks idempotency (already processed?)
3. If not: processes payment and marks as COMPLETED
4. If yes: ignores (prevents duplication)
5. If fails: retries (retry with backoff)
6. If fails 3x: goes to DLQ

### **Fanout Exchange - PaymentListener + NotificationListener**

1. Both receive the same event simultaneously
2. Each has its own idempotency record
3. PaymentListener processes payment
4. NotificationListener sends notification
5. If one fails, the other continues (independent)
6. Each has its own retry and DLQ

### **Retry with Exponential Backoff**

- **Attempt 1:** Waits 1 second
- **Attempt 2:** Waits 2 seconds
- **Attempt 3:** Waits 4 seconds
- **Maximum:** 10 seconds

Configured in `application.yaml`:

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        retry:
          enabled: true
          max-attempts: 3
          initial-interval: 1000
          multiplier: 2.0
          max-interval: 10000
```

### **Idempotency**

Each listener has a record in `processed_events`:

```
eventId: 550e8400-e29b-41d4-a716-446655440000
listenerName: PaymentListener
status: SUCCESS
processedAt: 2026-05-12T15:30:05
```

If the same message arrives twice, the listener checks this record and ignores it.

---

## 📁 Project Structure

```
src/main/java/com/example/rabbit_template/
├── config/
│   └── RabbitMQConfig.java          # Configuration of exchanges, queues, bindings
├── constants/
│   ├── RabbitConstants.java         # Names of exchanges, queues, routing keys
│   ├── EventType.java               # Event types
│   └── Status.java                  # Order and processing status
├── controller/
│   └── OrderController.java         # REST endpoints
├── domain/
│   ├── Order.java                   # Order entity
│   ├── OrderItem.java               # Order items
│   └── ProcessedEvent.java          # Idempotency tracking
├── dto/
│   ├── OrderRequest.java            # Input DTO
│   ├── OrderResponse.java           # Output DTO
│   └── OrderItemRequest.java        # Item DTO
├── event/
│   └── OrderCreatedEvent.java       # Event published to RabbitMQ
├── exception/
│   ├── GlobalExceptionHandler.java  # Exception handling
│   └── OrderNotFoundException.java   # Custom exception
├── listener/
│   └── PaymentListener.java         # Consumes events (Topic + Fanout)
│   └── NotificationListener.java    # Consumes events (Fanout + DLQ)
├── mapper/
│   └── OrderMapper.java             # Entity and DTO conversion
├── publisher/
│   └── OrderCreatedEventPublisher.java  # Publishes events to RabbitMQ
├── repository/
│   ├── OrderRepository.java         # Order data access
│   └── ProcessedEventRepository.java # Idempotency data access
├── scheduler/
│   └── QueueHealthMonitor.java      # Monitors queue health
├── service/
│   ├── OrderService.java            # Business logic
│   └── IdempotencyService.java      # Manages idempotency
└── utils/
    └── JsonUtils.java               # JSON utilities

src/main/resources/
└── application.yaml                 # Application configuration
```

---

## 🛠️ Technologies

| Technology | Version | Purpose |
|-----------|---------|---------|
| Java | 21+ | Language |
| Spring Boot | 3.x | Framework |
| Spring AMQP | 3.x | RabbitMQ Integration |
| RabbitMQ | 3.x | Message Broker |
| H2 Database | 2.x | In-memory database |
| Hibernate | 7.x | ORM |
| MapStruct | 1.x | Object mapping |
| Lombok | 1.x | Boilerplate reduction |
| Maven | 3.8+ | Build tool |

---

## 📚 Concepts Learned

### **Topic Exchange**
- Routing based on routing key patterns
- Ideal for different types of events
- One listener per queue

### **Fanout Exchange**
- Broadcast to all listeners
- No routing (everyone receives)
- Multiple listeners process in parallel

### **Retry with Exponential Backoff**
- Automatic retries with increasing delay
- Prevents system overload
- Configurable via `application.yaml`

### **Dead Letter Queue (DLQ)**
- Stores messages that failed permanently
- Allows analysis and manual reprocessing
- Form 1: Retry goes back to original exchange
- Form 2: Retry goes to specific DLQ

### **Idempotency**
- Each listener has its own record
- Prevents duplicate processing
- Supports multiple listeners in Fanout
- Allows selective reprocessing

---

## 🧪 Testing with Postman

Import the provided collection: `rabbit-template.postman_collection.json`

Or use curl commands:

```bash
# Topic
curl -X POST http://localhost:8080/orders/topic \
  -H "Content-Type: application/json" \
  -d '{"customerId":"CUST-001","amount":150.50,"items":[{"productId":"PROD-001","quantity":2}]}'

# Fanout
curl -X POST http://localhost:8080/orders/fanout \
  -H "Content-Type: application/json" \
  -d '{"customerId":"CUST-002","amount":250.00,"items":[{"productId":"PROD-002","quantity":1}]}'

# List
curl http://localhost:8080/orders

# Get by ID
curl http://localhost:8080/orders/550e8400-e29b-41d4-a716-446655440000
```

---

## 📊 Monitoring RabbitMQ

Access the Management Console: `http://localhost:15672`

You can view:
- Created exchanges
- Queues and their messages
- Bindings
- Active connections
- Message rate

---

## 🎓 Next Steps

To deepen your knowledge:

1. Implement more listeners (Analytics, Audit, etc.)
2. Add event persistence (Event Sourcing)
3. Implement Circuit Breaker for failures
4. Add metrics with Micrometer
5. Implement integration tests
6. Use Docker Compose for complete environment

---

## 📝 License

This project is provided as educational material.

---

## 👨‍💻 Author

Educational project to learn RabbitMQ and event-driven architecture.

---

## 🤝 Contributions

Feel free to fork, study and improve this project!

---

**Last updated:** May 2026
