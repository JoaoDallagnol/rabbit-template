# Design Document

## Feature: rabbit-template

---

## Overview

Rabbit Template is a Spring Boot 4.x REST API for order management. It exposes three HTTP endpoints (`POST /orders`, `GET /orders`, `GET /orders/{orderId}`), persists data with Spring Data JPA backed by an H2 in-memory database, and documents itself via Springdoc OpenAPI. The codebase is intentionally structured to support a future event-driven integration with RabbitMQ — a `TODO` comment marks the publish point in the service layer, but no messaging infrastructure is included.

The design follows a classic layered architecture: Controller → Service → Repository, with MapStruct handling all entity-to-DTO conversions and Lombok eliminating boilerplate. All dependency injection is constructor-based throughout.

**Key research findings:**
- Spring Boot 4.x (4.0.6 as declared in the existing pom.xml) requires Java 17+ and has first-class support for Java 25.
- Springdoc OpenAPI 2.x supports Spring Boot 3.x and 4.x with the `springdoc-openapi-starter-webmvc-ui` artifact.
- MapStruct 1.6.x is compatible with Spring Boot 4.x; the `maven-compiler-plugin` annotation processor path must list Lombok **before** MapStruct so Lombok generates getters/setters before MapStruct reads them.
- The existing `pom.xml` already declares Lombok, H2, devtools, and `spring-boot-starter-test`; the missing dependencies (`spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `springdoc-openapi-starter-webmvc-ui`, `mapstruct`, `mapstruct-processor`) must be added.

---

## Architecture

The application follows a strict layered architecture with unidirectional dependencies:

```
HTTP Client
     │
     ▼
┌─────────────────────┐
│   OrderController   │  @RestController — HTTP boundary, no business logic
└─────────┬───────────┘
          │ delegates to
          ▼
┌─────────────────────┐
│    OrderService     │  @Service — orchestrates domain operations
└──────┬──────┬───────┘
       │      │ uses
       │      ▼
       │  ┌──────────────┐
       │  │ OrderMapper  │  MapStruct — entity ↔ DTO conversion
       │  └──────────────┘
       │ uses
       ▼
┌─────────────────────┐
│  OrderRepository    │  JpaRepository — persistence
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│   H2 In-Memory DB   │
└─────────────────────┘

Cross-cutting:
┌──────────────────────────┐
│  GlobalExceptionHandler  │  @ControllerAdvice — translates exceptions to HTTP responses
└──────────────────────────┘
```

**Package structure** under `com.example.rabbit_template`:

| Package       | Contents                                      |
|---------------|-----------------------------------------------|
| `controller`  | `OrderController`                             |
| `service`     | `OrderService`                                |
| `repository`  | `OrderRepository`                             |
| `domain`      | `Order`, `OrderItem` (JPA entities)           |
| `dto`         | `OrderRequest`, `OrderItemRequest`, `OrderResponse` |
| `mapper`      | `OrderMapper`                                 |
| `exception`   | `OrderNotFoundException`                      |

---

## Components and Interfaces

### OrderController

```java
@RestController
@RequestMapping("/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) { ... }

    @PostMapping                                    // POST /orders → 201 or 400
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest request) { ... }

    @GetMapping                                     // GET /orders → 200
    public ResponseEntity<List<OrderResponse>> listOrders() { ... }

    @GetMapping("/{orderId}")                       // GET /orders/{orderId} → 200 or 404
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable UUID orderId) { ... }
}
```

### OrderService

```java
@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    public OrderService(OrderRepository orderRepository, OrderMapper orderMapper) { ... }

    public OrderResponse createOrder(OrderRequest request) {
        // Build Order entity, generate UUID, set status="CREATED", set createdAt=now()
        // Persist via repository
        // TODO: Publish OrderCreated event to RabbitMQ exchange (not implemented)
        // Return mapped OrderResponse
    }

    public List<OrderResponse> listOrders() { ... }

    public OrderResponse getOrderById(UUID orderId) {
        // Throws OrderNotFoundException if not found
    }
}
```

### OrderMapper

```java
@Mapper(componentModel = "spring")
public interface OrderMapper {
    OrderResponse toOrderResponse(Order order);       // maps orderId, status, amount, createdAt
    OrderItem toOrderItem(OrderItemRequest request);  // maps productId, quantity
}
```

### OrderRepository

```java
public interface OrderRepository extends JpaRepository<Order, UUID> {
    // No custom methods — inherits findAll, findById, save, etc.
}
```

### GlobalExceptionHandler

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFound(OrderNotFoundException ex) {
        // Returns 404 with { "message": "..." }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        // Returns 400 with { "errors": [...] }
    }
}
```

### OrderNotFoundException

```java
// package com.example.rabbit_template.exception
public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(UUID orderId) {
        super("Order not found: " + orderId);
    }
}
```

---

## Data Models

### JPA Entities

**Order** (`orders` table)

| Field       | Type          | Constraints                        |
|-------------|---------------|------------------------------------|
| orderId     | UUID          | PK, `@GeneratedValue` (UUID strategy) |
| customerId  | String        | `@Column(nullable = false)`        |
| amount      | Double        | `@Column(nullable = false)`        |
| status      | String        | `@Column(nullable = false)`        |
| createdAt   | LocalDateTime | `@Column(nullable = false)`        |
| items       | List\<OrderItem\> | `@OneToMany(mappedBy="order", cascade=ALL, orphanRemoval=true)` |

Lombok: `@Entity`, `@Table(name="orders")`, `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`

**OrderItem** (`order_items` table)

| Field     | Type    | Constraints                                      |
|-----------|---------|--------------------------------------------------|
| id        | Long    | PK, `@GeneratedValue(strategy = IDENTITY)`       |
| productId | String  | `@Column(nullable = false)`                      |
| quantity  | Integer | `@Column(nullable = false)`                      |
| order     | Order   | `@ManyToOne`, `@JoinColumn(name = "order_id")`   |

Lombok: `@Entity`, `@Table(name="order_items")`, `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`

### DTOs

**OrderRequest**

| Field      | Type                    | Validation                        |
|------------|-------------------------|-----------------------------------|
| customerId | String                  | `@NotBlank`                       |
| amount     | Double                  | `@NotNull`                        |
| items      | List\<OrderItemRequest\> | `@NotNull`, `@NotEmpty`           |

**OrderItemRequest**

| Field     | Type    | Validation              |
|-----------|---------|-------------------------|
| productId | String  | `@NotBlank`             |
| quantity  | Integer | `@NotNull`, `@Min(1)`   |

**OrderResponse** (intentionally excludes `customerId` and `items` per Requirement 3.4)

| Field     | Type          |
|-----------|---------------|
| orderId   | UUID          |
| status    | String        |
| amount    | Double        |
| createdAt | LocalDateTime |

All DTOs use `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`.

### application.yaml

```yaml
spring:
  application:
    name: rabbit-template
  datasource:
    url: jdbc:h2:mem:rabbitdb
    username: sa
    password:
  h2:
    console:
      enabled: true
      path: /h2-console
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    database-platform: org.hibernate.dialect.H2Dialect

springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
  info:
    title: "Rabbit Template"
    version: "1.0.0"
    description: "Order management API designed for future event-driven architecture with RabbitMQ (messaging not implemented yet)"
```

> **Note:** The `springdoc.info.*` properties are not natively supported by Springdoc's auto-configuration. The title, version, and description will be configured via a `@Bean` of type `OpenAPI` in a `@Configuration` class using the Springdoc API.

### pom.xml additions

The following dependencies must be added to the existing `pom.xml`:

```xml
<!-- Web -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- JPA -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- Validation -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<!-- Springdoc OpenAPI -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.9</version>
</dependency>

<!-- MapStruct runtime -->
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.6.3</version>
</dependency>
```

The `maven-compiler-plugin` annotation processor paths must list Lombok **before** MapStruct (so Lombok generates getters/setters first):

```xml
<annotationProcessorPaths>
    <path>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
    </path>
    <path>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct-processor</artifactId>
        <version>1.6.3</version>
    </path>
</annotationProcessorPaths>
```

---

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system — essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

The following properties are derived from the prework analysis. Properties covering the same logical concern have been consolidated to eliminate redundancy.

**Property reflection:** Requirements 4.2, 4.3, 12.1, and 12.2 all describe mapping correctness. Requirements 12.1 and 4.2 both assert that `Order → OrderResponse` mapping preserves the four fields — these are consolidated into Property 1. Requirements 12.2 and 4.3 both assert that `OrderItemRequest → OrderItem` mapping preserves two fields — consolidated into Property 2. Requirements 6.2 and 6.4/6.5 describe service-layer behavior that varies with input and can be tested as properties. Requirements 7.2, 7.3, 7.4, 7.5 describe HTTP-layer behavior that varies with input.

---

### Property 1: Order-to-OrderResponse mapping preserves all fields

*For any* valid `Order` entity with arbitrary `orderId`, `status`, `amount`, and `createdAt` values, mapping it to an `OrderResponse` via `OrderMapper` SHALL produce a response where `orderId`, `status`, `amount`, and `createdAt` are equal to the corresponding fields on the source entity.

**Validates: Requirements 4.2, 12.1**

---

### Property 2: OrderItemRequest-to-OrderItem mapping preserves all fields

*For any* valid `OrderItemRequest` DTO with arbitrary `productId` and `quantity` values, mapping it to an `OrderItem` entity via `OrderMapper` SHALL produce an entity where `productId` and `quantity` are equal to the corresponding fields on the source DTO.

**Validates: Requirements 4.3, 12.2**

---

### Property 3: createOrder always returns status "CREATED" with a non-null orderId and matching amount

*For any* valid `OrderRequest` with arbitrary `customerId`, `amount`, and non-empty `items`, calling `OrderService.createOrder` SHALL return an `OrderResponse` where `status` equals `"CREATED"`, `orderId` is non-null, `createdAt` is non-null, and `amount` equals the `amount` from the request.

**Validates: Requirements 6.2**

---

### Property 4: listOrders returns all persisted orders

*For any* set of N orders persisted in the repository, calling `OrderService.listOrders` SHALL return a list of exactly N `OrderResponse` objects whose `orderId` values match the persisted orders.

**Validates: Requirements 6.4**

---

### Property 5: getOrderById round-trip

*For any* `Order` persisted in the repository, calling `OrderService.getOrderById` with that order's `orderId` SHALL return an `OrderResponse` where `orderId`, `status`, `amount`, and `createdAt` match the persisted entity.

**Validates: Requirements 6.5**

---

### Property 6: POST /orders with valid body returns HTTP 201 with correct response shape

*For any* valid `OrderRequest` body, a `POST /orders` request SHALL return HTTP 201 with an `OrderResponse` body where `status` equals `"CREATED"`, `orderId` is non-null, and `amount` matches the request.

**Validates: Requirements 7.2**

---

### Property 7: POST /orders with invalid body returns HTTP 400

*For any* `OrderRequest` body that violates at least one validation constraint (null `customerId`, blank `customerId`, null `amount`, null `items`, empty `items`, or an `OrderItemRequest` with `quantity < 1`), a `POST /orders` request SHALL return HTTP 400.

**Validates: Requirements 7.3, 8.3**

---

### Property 8: GET /orders returns HTTP 200 with all persisted orders

*For any* set of N orders in the system, a `GET /orders` request SHALL return HTTP 200 with a JSON array of exactly N `OrderResponse` objects.

**Validates: Requirements 7.4**

---

### Property 9: GET /orders/{orderId} round-trip

*For any* order persisted in the system, a `GET /orders/{orderId}` request with that order's ID SHALL return HTTP 200 with an `OrderResponse` matching the persisted data.

**Validates: Requirements 7.5**

---

## Error Handling

| Scenario | Exception | HTTP Status | Response Body |
|---|---|---|---|
| `getOrderById` with unknown UUID | `OrderNotFoundException` | 404 | `{ "message": "Order not found: <uuid>" }` |
| Request body fails Jakarta Validation | `MethodArgumentNotValidException` | 400 | `{ "errors": ["field: message", ...] }` |
| Unexpected server error | `Exception` (optional catch-all) | 500 | `{ "message": "Internal server error" }` |

`OrderNotFoundException` is an unchecked exception (`extends RuntimeException`) in the `exception` package. It is thrown by `OrderService.getOrderById` and caught by `GlobalExceptionHandler`.

`MethodArgumentNotValidException` is thrown automatically by Spring MVC when `@Valid` fails on a `@RequestBody`. The handler extracts field errors from `ex.getBindingResult().getFieldErrors()` and returns them as a list.

---

## Testing Strategy

### Dual Testing Approach

Both unit/integration tests and property-based tests are used. Unit tests cover specific examples and error conditions; property-based tests verify universal properties across many generated inputs.

### Property-Based Testing Library

**jqwik** (`net.jqwik:jqwik`) is the chosen PBT library for Java. It integrates with JUnit 5 (already on the classpath via `spring-boot-starter-test`) and provides rich arbitraries for generating random domain objects.

Add to `pom.xml` (test scope):
```xml
<dependency>
    <groupId>net.jqwik</groupId>
    <artifactId>jqwik</artifactId>
    <version>1.9.3</version>
    <scope>test</scope>
</dependency>
```

Each property test runs a minimum of **100 iterations** (jqwik default is 1000, which is acceptable).

### Property Test Tag Format

Each property test is annotated with a label referencing the design property:

```java
// Feature: rabbit-template, Property 1: Order-to-OrderResponse mapping preserves all fields
@Property
void orderToOrderResponseMappingPreservesAllFields(...) { ... }
```

### Test Classes

| Test Class | Type | Covers |
|---|---|---|
| `OrderMapperTest` | Unit (jqwik) | Properties 1, 2 |
| `OrderServiceTest` | Unit (jqwik + Mockito) | Properties 3, 4, 5; example for Req 6.6 |
| `OrderControllerTest` | Spring MVC slice (`@WebMvcTest`, jqwik) | Properties 6, 7, 8, 9; example for Req 7.6 |
| `GlobalExceptionHandlerTest` | Unit / MVC slice | Req 8.2 (example) |
| `RabbitTemplateApplicationTests` | Spring context load | Smoke — context starts |

### Unit Test Focus Areas

- `OrderNotFoundException` is thrown for unknown UUIDs (example-based)
- HTTP 404 response shape for `OrderNotFoundException` (example-based)
- HTTP 400 response shape for validation failures (covered by Property 7)
- Context loads successfully (smoke)

### Integration / Smoke Checks

The following are verified by the context-load test and/or code review rather than property tests:
- pom.xml dependency declarations
- Entity annotations and field declarations
- Package structure and constructor injection
- `application.yaml` configuration values
- Springdoc endpoint availability (`/swagger-ui.html`, `/v3/api-docs`)
- Absence of RabbitMQ/AMQP dependencies and logic
- Presence of the `TODO` comment in `OrderService.createOrder`
