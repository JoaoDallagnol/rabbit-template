# Requirements Document

## Introduction

Rabbit Template is a Spring Boot REST API for order management, designed with a clean layered architecture and built to support a future event-driven integration with RabbitMQ (messaging not yet implemented). The API exposes three endpoints for creating and retrieving orders, persists data using JPA with an H2 in-memory database, and is documented via Springdoc OpenAPI. The project uses Java 25, Spring Boot 4.x, Maven, MapStruct for object mapping, and Lombok for boilerplate reduction.

## Glossary

- **API**: The HTTP REST interface exposed by the application.
- **Order**: The core domain entity representing a customer purchase, identified by a UUID.
- **OrderItem**: A line item within an Order, referencing a product and a quantity.
- **OrderRequest**: The inbound DTO carrying the data needed to create an Order.
- **OrderItemRequest**: The inbound DTO carrying the data for a single line item.
- **OrderResponse**: The outbound DTO returned to callers after an Order is created or retrieved.
- **Controller**: The Spring MVC layer that receives HTTP requests and delegates to the Service.
- **Service**: The business-logic layer that orchestrates domain operations.
- **Repository**: The Spring Data JPA layer responsible for persistence.
- **Mapper**: The MapStruct component that converts between domain entities and DTOs.
- **GlobalExceptionHandler**: The `@ControllerAdvice` component that translates exceptions into HTTP error responses.
- **H2**: The in-memory relational database used for development and testing.
- **Springdoc**: The library that generates and serves the OpenAPI documentation UI.

---

## Requirements

### Requirement 1: Project Structure and Dependencies

**User Story:** As a developer, I want a well-structured Maven project with all required dependencies declared, so that the application compiles and runs without manual configuration.

#### Acceptance Criteria

1. THE Project SHALL use Java 25 as the source and target compilation level.
2. THE Project SHALL declare Spring Boot 4.x (latest stable compatible with Java 25) as the parent POM.
3. THE Project SHALL include the following dependencies: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `com.h2database:h2` (runtime scope), `springdoc-openapi-starter-webmvc-ui`, `org.mapstruct:mapstruct`, `org.projectlombok:lombok`, and `spring-boot-starter-test` (test scope).
4. THE Project SHALL configure the `maven-compiler-plugin` annotation processor paths to include both Lombok and MapStruct so that code generation runs correctly during compilation.
5. THE Project SHALL NOT include any RabbitMQ, AMQP, or messaging-related dependencies.

---

### Requirement 2: Domain Model

**User Story:** As a developer, I want clearly defined JPA entities, so that the application can persist and retrieve order data correctly.

#### Acceptance Criteria

1. THE Order_Entity SHALL be annotated with `@Entity` and map to a table named `orders`, with fields: `orderId` (UUID, primary key, generated), `customerId` (String, not null), `amount` (Double, not null), `status` (String, not null), and `createdAt` (LocalDateTime, not null).
2. THE OrderItem_Entity SHALL be annotated with `@Entity` and map to a table named `order_items`, with fields: `id` (Long, auto-generated primary key), `productId` (String, not null), and `quantity` (Integer, not null).
3. THE OrderItem_Entity SHALL declare a `@ManyToOne` relationship to Order, with the foreign key column named `order_id`.
4. THE Order_Entity SHALL declare a `@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)` relationship to a list of OrderItem entities.
5. THE Order_Entity SHALL use constructor injection-compatible design (e.g., Lombok `@Builder` or all-args constructor) and SHALL NOT rely on field injection.

---

### Requirement 3: Data Transfer Objects

**User Story:** As a developer, I want clearly defined DTOs that decouple the API contract from the domain model, so that the API surface can evolve independently.

#### Acceptance Criteria

1. THE OrderRequest_DTO SHALL contain fields `customerId` (String), `amount` (Double), and `items` (List of OrderItemRequest), all annotated with `@NotNull` (and `@NotBlank` for String fields, `@NotEmpty` for the list).
2. THE OrderItemRequest_DTO SHALL contain fields `productId` (String, `@NotBlank`) and `quantity` (Integer, `@NotNull`, `@Min(1)`).
3. THE OrderResponse_DTO SHALL contain fields `orderId` (UUID), `status` (String), `amount` (Double), and `createdAt` (LocalDateTime), matching the OpenAPI `OrderResponse` schema exactly.
4. THE OrderResponse_DTO SHALL NOT include `customerId` or `items` fields, as these are intentionally excluded from the response per the API contract.
5. THE DTOs SHALL use Lombok annotations (`@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`) to reduce boilerplate.

---

### Requirement 4: Object Mapping

**User Story:** As a developer, I want a dedicated MapStruct mapper, so that conversions between entities and DTOs are type-safe and maintainable.

#### Acceptance Criteria

1. THE OrderMapper SHALL be a MapStruct interface annotated with `@Mapper(componentModel = "spring")`.
2. WHEN converting an Order entity to an OrderResponse DTO, THE OrderMapper SHALL map `orderId`, `status`, `amount`, and `createdAt` fields.
3. WHEN converting an OrderItemRequest DTO to an OrderItem entity, THE OrderMapper SHALL map `productId` and `quantity` fields.
4. THE OrderMapper SHALL be injectable as a Spring bean via constructor injection in the Service layer.

---

### Requirement 5: Repository Layer

**User Story:** As a developer, I want a Spring Data JPA repository for orders, so that CRUD operations are available without boilerplate SQL.

#### Acceptance Criteria

1. THE OrderRepository SHALL extend `JpaRepository<Order, UUID>`.
2. THE OrderRepository SHALL be located in the `repository` package under the base package `com.example.rabbit_template`.
3. THE OrderRepository SHALL NOT contain any custom query methods beyond those inherited from `JpaRepository`.

---

### Requirement 6: Service Layer

**User Story:** As a developer, I want a service class that encapsulates all business logic for order management, so that controllers remain thin and logic is testable in isolation.

#### Acceptance Criteria

1. THE OrderService SHALL be annotated with `@Service` and use constructor injection for all dependencies (OrderRepository, OrderMapper).
2. WHEN `createOrder` is called with a valid OrderRequest, THE OrderService SHALL generate a new UUID for `orderId`, set `status` to `"CREATED"`, set `createdAt` to the current timestamp, persist the Order via the repository, and return an OrderResponse.
3. WHEN `createOrder` is called, THE OrderService SHALL include the comment `// TODO: Publish OrderCreated event to RabbitMQ exchange (not implemented)` at the point where event publishing would occur, after the order is persisted.
4. WHEN `listOrders` is called, THE OrderService SHALL retrieve all Order entities from the repository and return them mapped to a list of OrderResponse DTOs.
5. WHEN `getOrderById` is called with a UUID that exists in the repository, THE OrderService SHALL return the corresponding OrderResponse.
6. WHEN `getOrderById` is called with a UUID that does not exist in the repository, THE OrderService SHALL throw an `OrderNotFoundException`.
7. THE OrderService SHALL NOT contain any RabbitMQ, AMQP, or messaging logic.

---

### Requirement 7: Controller Layer

**User Story:** As a developer, I want a REST controller that maps HTTP requests to service calls, so that the API endpoints defined in the OpenAPI spec are reachable.

#### Acceptance Criteria

1. THE OrderController SHALL be annotated with `@RestController` and `@RequestMapping("/orders")`, and SHALL use constructor injection for the OrderService.
2. WHEN a `POST /orders` request is received with a valid body, THE OrderController SHALL invoke `OrderService.createOrder`, and return HTTP 201 with the OrderResponse body.
3. WHEN a `POST /orders` request is received with an invalid body (validation failure), THE OrderController SHALL return HTTP 400.
4. WHEN a `GET /orders` request is received, THE OrderController SHALL invoke `OrderService.listOrders` and return HTTP 200 with the list of OrderResponse bodies.
5. WHEN a `GET /orders/{orderId}` request is received with a valid UUID, THE OrderController SHALL invoke `OrderService.getOrderById` and return HTTP 200 with the OrderResponse body.
6. WHEN a `GET /orders/{orderId}` request is received with a UUID that does not exist, THE OrderController SHALL return HTTP 404.
7. THE OrderController SHALL NOT contain any business logic; all logic SHALL be delegated to the OrderService.
8. THE OrderController SHALL use `ResponseEntity` as the return type for all handler methods.

---

### Requirement 8: Exception Handling

**User Story:** As a developer, I want a centralized exception handler, so that all error responses are consistent and informative.

#### Acceptance Criteria

1. THE GlobalExceptionHandler SHALL be annotated with `@ControllerAdvice` and handle exceptions globally across all controllers.
2. WHEN an `OrderNotFoundException` is thrown, THE GlobalExceptionHandler SHALL return HTTP 404 with a JSON error body containing a `message` field.
3. WHEN a `MethodArgumentNotValidException` is thrown (Jakarta Validation failure), THE GlobalExceptionHandler SHALL return HTTP 400 with a JSON error body listing the validation errors.
4. THE GlobalExceptionHandler SHALL use constructor injection if it has any dependencies.
5. THE OrderNotFoundException SHALL be a custom unchecked exception in the `exception` package under the base package.

---

### Requirement 9: Database Configuration

**User Story:** As a developer, I want the H2 in-memory database configured via application.yml, so that the application starts with a ready-to-use database and the H2 console is accessible for inspection.

#### Acceptance Criteria

1. THE Application SHALL configure the H2 datasource in `application.yml` using the JDBC URL `jdbc:h2:mem:rabbitdb`, with username `sa` and an empty password.
2. THE Application SHALL enable the H2 web console at the path `/h2-console` via `spring.h2.console.enabled: true`.
3. THE Application SHALL set `spring.jpa.hibernate.ddl-auto: create-drop` so that the schema is created on startup and dropped on shutdown.
4. THE Application SHALL set `spring.jpa.show-sql: true` for development visibility.
5. THE Application SHALL set `spring.jpa.database-platform` to the H2 dialect compatible with the configured Spring Boot version.

---

### Requirement 10: OpenAPI Documentation

**User Story:** As a developer, I want the API to be self-documented via Springdoc OpenAPI, so that consumers can explore and test endpoints through the Swagger UI.

#### Acceptance Criteria

1. THE Application SHALL expose the Swagger UI at `/swagger-ui.html` (or the Springdoc default path) when the `springdoc-openapi-starter-webmvc-ui` dependency is on the classpath.
2. THE Application SHALL expose the OpenAPI JSON/YAML descriptor at `/v3/api-docs`.
3. WHERE Springdoc is configured, THE Application SHALL set the API title to `"Rabbit Template"`, version to `"1.0.0"`, and description to `"Order management API designed for future event-driven architecture with RabbitMQ (messaging not implemented yet)"`.

---

### Requirement 11: Package Organization

**User Story:** As a developer, I want a clean, consistent package structure, so that the codebase is easy to navigate and each layer has a single responsibility.

#### Acceptance Criteria

1. THE Application SHALL organize source files under `com.example.rabbit_template` with the following sub-packages: `controller`, `service`, `repository`, `domain`, `dto`, `mapper`, and `exception`.
2. THE Application SHALL NOT place business logic in the `controller` package.
3. THE Application SHALL NOT place persistence logic in the `service` package.
4. THE Application SHALL use constructor injection exclusively; field injection via `@Autowired` SHALL NOT appear anywhere in the codebase.

---

### Requirement 12: Round-Trip Mapping Consistency

**User Story:** As a developer, I want to verify that the mapper correctly converts entities to response DTOs, so that data is not silently lost or corrupted during mapping.

#### Acceptance Criteria

1. FOR ALL valid Order entities, THE OrderMapper SHALL produce an OrderResponse where `orderId`, `status`, `amount`, and `createdAt` equal the corresponding fields on the source entity (round-trip mapping property).
2. FOR ALL valid OrderItemRequest DTOs, THE OrderMapper SHALL produce an OrderItem entity where `productId` and `quantity` equal the corresponding fields on the source DTO.
