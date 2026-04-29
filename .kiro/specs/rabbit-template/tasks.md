# Tasks

## Task List

- [x] 1. Update pom.xml with all required dependencies and build configuration
  - [x] 1.1 Add `spring-boot-starter-web` dependency
  - [x] 1.2 Add `spring-boot-starter-data-jpa` dependency
  - [x] 1.3 Add `spring-boot-starter-validation` dependency
  - [x] 1.4 Add `springdoc-openapi-starter-webmvc-ui` dependency (version 2.8.9)
  - [x] 1.5 Add `org.mapstruct:mapstruct` dependency (version 1.6.3)
  - [x] 1.6 Add `net.jqwik:jqwik` dependency (version 1.9.3, test scope)
  - [x] 1.7 Update `maven-compiler-plugin` annotation processor paths to include Lombok (first) and `mapstruct-processor` (second)
  - [x] 1.8 Verify no RabbitMQ/AMQP dependencies are present

- [x] 2. Configure application.yaml
  - [x] 2.1 Add H2 datasource configuration (`jdbc:h2:mem:rabbitdb`, username `sa`, empty password)
  - [x] 2.2 Enable H2 console at `/h2-console`
  - [x] 2.3 Set `spring.jpa.hibernate.ddl-auto: create-drop`
  - [x] 2.4 Set `spring.jpa.show-sql: true`
  - [x] 2.5 Set `spring.jpa.database-platform` to H2 dialect
  - [x] 2.6 Add Springdoc configuration for api-docs and swagger-ui paths

- [x] 3. Create domain entities
  - [x] 3.1 Create `Order` entity in `com.example.rabbit_template.domain` with fields `orderId` (UUID PK), `customerId`, `amount`, `status`, `createdAt`, and `items` list; annotate with `@Entity`, `@Table(name="orders")`, Lombok `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`
  - [x] 3.2 Create `OrderItem` entity in `com.example.rabbit_template.domain` with fields `id` (Long PK), `productId`, `quantity`, and `order` (ManyToOne); annotate with `@Entity`, `@Table(name="order_items")`, Lombok `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`
  - [x] 3.3 Add `@OneToMany(mappedBy="order", cascade=CascadeType.ALL, orphanRemoval=true)` on `Order.items`
  - [x] 3.4 Add `@ManyToOne` and `@JoinColumn(name="order_id")` on `OrderItem.order`

- [x] 4. Create DTOs
  - [x] 4.1 Create `OrderRequest` DTO in `com.example.rabbit_template.dto` with fields `customerId` (`@NotBlank`), `amount` (`@NotNull`), `items` (`@NotNull`, `@NotEmpty`); add Lombok `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`
  - [x] 4.2 Create `OrderItemRequest` DTO in `com.example.rabbit_template.dto` with fields `productId` (`@NotBlank`), `quantity` (`@NotNull`, `@Min(1)`); add Lombok annotations
  - [x] 4.3 Create `OrderResponse` DTO in `com.example.rabbit_template.dto` with fields `orderId` (UUID), `status` (String), `amount` (Double), `createdAt` (LocalDateTime) — no `customerId` or `items`; add Lombok annotations

- [x] 5. Create OrderMapper
  - [x] 5.1 Create `OrderMapper` interface in `com.example.rabbit_template.mapper` annotated with `@Mapper(componentModel = "spring")`
  - [x] 5.2 Declare `OrderResponse toOrderResponse(Order order)` method mapping `orderId`, `status`, `amount`, `createdAt`
  - [x] 5.3 Declare `OrderItem toOrderItem(OrderItemRequest request)` method mapping `productId`, `quantity`

- [x] 6. Create OrderRepository
  - [x] 6.1 Create `OrderRepository` interface in `com.example.rabbit_template.repository` extending `JpaRepository<Order, UUID>` with no custom methods

- [x] 7. Create exception classes
  - [x] 7.1 Create `OrderNotFoundException` in `com.example.rabbit_template.exception` extending `RuntimeException` with a constructor accepting a `UUID orderId`

- [x] 8. Create OrderService
  - [x] 8.1 Create `OrderService` class in `com.example.rabbit_template.service` annotated with `@Service`, using constructor injection for `OrderRepository` and `OrderMapper`
  - [x] 8.2 Implement `createOrder(OrderRequest request)`: generate UUID, set `status="CREATED"`, set `createdAt=LocalDateTime.now()`, map items, persist via repository, add `// TODO: Publish OrderCreated event to RabbitMQ exchange (not implemented)` comment after persist, return mapped `OrderResponse`
  - [x] 8.3 Implement `listOrders()`: retrieve all orders from repository, map each to `OrderResponse`, return list
  - [x] 8.4 Implement `getOrderById(UUID orderId)`: find by ID in repository, throw `OrderNotFoundException` if absent, return mapped `OrderResponse`

- [x] 9. Create GlobalExceptionHandler
  - [x] 9.1 Create `GlobalExceptionHandler` class in `com.example.rabbit_template.exception` (or a dedicated `handler` sub-package) annotated with `@ControllerAdvice`
  - [x] 9.2 Add handler for `OrderNotFoundException` returning HTTP 404 with JSON body `{ "message": "..." }`
  - [x] 9.3 Add handler for `MethodArgumentNotValidException` returning HTTP 400 with JSON body listing field validation errors

- [x] 10. Create OrderController
  - [x] 10.1 Create `OrderController` class in `com.example.rabbit_template.controller` annotated with `@RestController` and `@RequestMapping("/orders")`, using constructor injection for `OrderService`
  - [x] 10.2 Implement `POST /orders` handler: accept `@Valid @RequestBody OrderRequest`, call `orderService.createOrder`, return `ResponseEntity` with HTTP 201 and `OrderResponse` body
  - [x] 10.3 Implement `GET /orders` handler: call `orderService.listOrders`, return `ResponseEntity` with HTTP 200 and list body
  - [x] 10.4 Implement `GET /orders/{orderId}` handler: accept `@PathVariable UUID orderId`, call `orderService.getOrderById`, return `ResponseEntity` with HTTP 200 and `OrderResponse` body

- [x] 11. Configure OpenAPI documentation
  - [x] 11.1 Create an `OpenApiConfig` `@Configuration` class that exposes an `OpenAPI` `@Bean` setting title to `"Rabbit Template"`, version to `"1.0.0"`, and description to `"Order management API designed for future event-driven architecture with RabbitMQ (messaging not implemented yet)"`

- [x] 12. Write property-based tests for OrderMapper (Properties 1 and 2)
  - [x] 12.1 Create `OrderMapperTest` using jqwik; write property test for Property 1: generate random `Order` entities, map to `OrderResponse`, assert `orderId`, `status`, `amount`, `createdAt` match — tag: `Feature: rabbit-template, Property 1: Order-to-OrderResponse mapping preserves all fields`
  - [x] 12.2 Write property test for Property 2: generate random `OrderItemRequest` DTOs, map to `OrderItem`, assert `productId` and `quantity` match — tag: `Feature: rabbit-template, Property 2: OrderItemRequest-to-OrderItem mapping preserves all fields`

- [x] 13. Write property-based and unit tests for OrderService (Properties 3, 4, 5)
  - [x] 13.1 Create `OrderServiceTest` using jqwik + Mockito; write property test for Property 3: generate random valid `OrderRequest` objects, call `createOrder`, assert `status="CREATED"`, non-null `orderId`, non-null `createdAt`, matching `amount` — tag: `Feature: rabbit-template, Property 3: createOrder always returns status CREATED with a non-null orderId and matching amount`
  - [x] 13.2 Write property test for Property 4: seed mock repository with N random orders, call `listOrders`, assert returned list size equals N and all `orderId` values match — tag: `Feature: rabbit-template, Property 4: listOrders returns all persisted orders`
  - [x] 13.3 Write property test for Property 5: persist random order, call `getOrderById` with its UUID, assert returned `OrderResponse` fields match — tag: `Feature: rabbit-template, Property 5: getOrderById round-trip`
  - [x] 13.4 Write example-based unit test for Requirement 6.6: call `getOrderById` with a random UUID not in the repository, assert `OrderNotFoundException` is thrown

- [x] 14. Write property-based and integration tests for OrderController (Properties 6, 7, 8, 9)
  - [x] 14.1 Create `OrderControllerTest` using `@WebMvcTest` + jqwik; write property test for Property 6: generate random valid `OrderRequest` bodies, POST to `/orders`, assert HTTP 201 and `OrderResponse` with `status="CREATED"` — tag: `Feature: rabbit-template, Property 6: POST /orders with valid body returns HTTP 201 with correct response shape`
  - [x] 14.2 Write property test for Property 7: generate invalid `OrderRequest` bodies (null/blank fields, empty items, quantity < 1), POST to `/orders`, assert HTTP 400 — tag: `Feature: rabbit-template, Property 7: POST /orders with invalid body returns HTTP 400`
  - [x] 14.3 Write property test for Property 8: seed with N random orders, GET `/orders`, assert HTTP 200 and array of N items — tag: `Feature: rabbit-template, Property 8: GET /orders returns HTTP 200 with all persisted orders`
  - [x] 14.4 Write property test for Property 9: persist random order, GET `/orders/{orderId}`, assert HTTP 200 and matching `OrderResponse` — tag: `Feature: rabbit-template, Property 9: GET /orders/{orderId} round-trip`
  - [x] 14.5 Write example-based test for Requirement 7.6: GET `/orders/{randomUUID}` not in system, assert HTTP 404

- [x] 15. Verify application context loads and run all tests
  - [x] 15.1 Confirm `RabbitTemplateApplicationTests` context-load test passes
  - [x] 15.2 Run `mvn test` and confirm all tests pass with no compilation errors
