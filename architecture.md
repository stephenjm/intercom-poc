# Intercom POC Architecture & Generation Guide

## Project Goal
To build a robust, observable Spring Boot microservice for a customer support ticketing system (Intercom clone). The system features role-based escalation workflows (Customer -> Agent -> Manager), event sourcing for audit trails, header-based simulated identity, and a production-ready observability stack (Prometheus & Grafana) using Docker Compose.

---

## Anti-Drift & Determinism Locks
*To prevent AI hallucination and ecosystem drift when regenerating this project in the future, prepend the following constraints to your system prompt. This ensures the AI uses compatible library versions and avoids mixing legacy (javax) and modern (jakarta) namespaces.*

**System Constraints Prompt:**
> "Act as a Senior Java Developer. You are constrained to the following tech stack and rules:
> - **Java Version:** Java 17 strictly.
> - **Framework:** Spring Boot 3.3.4 (This requires using `jakarta.persistence.*` instead of `javax.persistence.*` for all JPA entities).
> - **Build Tool:** Gradle 8.x.
> - **Lombok Hack:** You MUST override the Spring Boot BOM Lombok version by adding `ext['lombok.version'] = '1.18.38'` to the `build.gradle` to prevent `TypeTag :: UNKNOWN` compiler crashes.
> - **Dependencies:**
    >   - `spring-boot-starter-web`
>   - `spring-boot-starter-data-jpa`
>   - `spring-boot-starter-aop`
>   - `spring-boot-starter-actuator`
>   - `io.micrometer:micrometer-registry-prometheus`
>   - `com.h2database:h2` (runtimeOnly)
>   - `org.junit.platform:junit-platform-launcher` (testRuntimeOnly, REQUIRED for Gradle 8.x + JUnit 5)
> - **No Security:** Do NOT include `spring-boot-starter-security`. We are using a custom `X-Caller-Id` header for identity routing.
> - **Port Mapping:** The Spring application must run internally on `8080`, mapped to `8180` in docker-compose. Prometheus runs on `9090` (mapped to `9190`). Grafana runs on `3000` (mapped to `3100`)."

---



## Component Generation Prompts
If this project needs to be recreated from scratch, feeding the following prompts sequentially to an AI assistant will yield a highly probable recreation of the current architecture.

### Phase 1: Project Scaffolding & Domain Model
**Prompt:**
> "Generate a Spring Boot 3.3.x project using Java 17+ and Gradle. Include dependencies for Web, Data JPA, H2 database, and Lombok. To avoid JDK compatibility issues, explicitly override the Spring Boot Lombok version to `1.18.38` globally in `build.gradle` using the `ext` property. 
> 
> Create the core domain entities with JPA annotations: 
> 1. `User` (id, name, email with a unique constraint, role [CUSTOMER, AGENT, MANAGER])
> 2. `Conversation` (id, many-to-one mapping to a customer User, many-to-one mapping to an agent User, `ConversationStatus` enum [UNASSIGNED, OPEN, ESCALATED, CLOSED], createdAt)
> 3. `Message` (id, many-to-one to Conversation, many-to-one to sender User, content, timestamp)
> 4. `EventLog` (id, many-to-one to Conversation, action string, details string, timestamp). 
> 
> Finally, create standard Spring Data `JpaRepository` interfaces for each entity."

### Phase 2: Core Services & Event Sourcing (Audit Logs)
**Prompt:**
> "Create a `ConversationService` and a `MessageService` with `@Transactional` methods. The `ConversationService` should handle creating a conversation, assigning an agent, and updating the status. 
> 
> Implement an event sourcing pattern: every time a conversation's state changes (created, assigned, status updated), the service must append a descriptive record to the `EventLog` table using the `EventLogRepository`. 
> 
> Create a `ConversationController` and an `EventLogController` to expose these actions via REST. Ensure the controllers map the database entities to DTOs before returning them in the response to prevent infinite recursion and data leakage."

### Phase 3: Header-Based Identity & Workflow Constraints
**Prompt:**
> "We need to enforce workflow rules without the overhead of full Spring Security. Update the REST Controllers to accept an `X-Caller-Id` HTTP header, representing the ID of the User making the request. Pass this caller ID down into the `ConversationService` methods.
> 
> Update the `ConversationService` to enforce these business rules:
> 1. Create a new `escalateToManager(conversationId, managerId, callerId)` method.
> 2. If a user with the `AGENT` role calls the standard `assignAgent` method and tries to assign the ticket to *another* agent, throw an `IllegalStateException`. Agents cannot reassign to peers.
> 3. If an agent wants to transfer a ticket, they must use the `escalateToManager` method, which verifies the target user has the `MANAGER` role, updates the conversation status to `ESCALATED`, and logs the event.
> 
> Create a `CommandLineRunner` bean that seeds the database on startup with three users: Alice (CUSTOMER), Bob (AGENT), and Charlie (MANAGER) so we have valid IDs to pass in the headers."

### Phase 4: Observability (AOP Logging & Metrics)
**Prompt:**
> "Add `spring-boot-starter-actuator`, `spring-boot-starter-aop`, and `micrometer-registry-prometheus` to the Gradle dependencies. 
> Configure `application.yml` to expose the `/actuator/prometheus` endpoint and tag the metrics with the application name.
> 
> Write a Spring AOP `@Aspect` named `LoggingAspect`. Use an `@Around` pointcut to intercept every method execution in the `ConversationService`. The aspect should dynamically extract the `X-Caller-Id` header from the `RequestContextHolder`, start a `StopWatch`, execute the method, and log a formatted message to standard output containing the caller ID, the class name, the method name, and the execution time in milliseconds. If an exception occurs, it should log the failure and rethrow it.
> 
> Finally, generate a `Dockerfile` for the Spring Boot app and a `docker-compose.yml` file. The compose file should define three services: the Java app (port 8180:8080), a Prometheus container (port 9190:9090) configured via a `prometheus.yml` file to scrape the Java app, and a Grafana container (port 3100:3000) for visualization."

### Phase 5: Exception Handling & Integration Testing
**Prompt:**
> "Create a `GlobalExceptionHandler` annotated with `@RestControllerAdvice` to gracefully handle exceptions. Map `IllegalStateException` to an HTTP 403 Forbidden response using a custom `ErrorResponse` DTO.
>
> Create a `src/test/resources/application-test.yml` file to override the database configuration, forcing the use of an in-memory H2 database (`jdbc:h2:mem:testdb`) to avoid file-locking conflicts with the local environment. Explicitly set `spring.sql.init.mode=never` to prevent Spring from running legacy `data.bak` scripts before Hibernate schema generation.
>
> Write a JUnit 5 `@SpringBootTest` named `ConversationIntegrationTest`. Use `@ActiveProfiles("test")` to load the in-memory database. Use `@AutoConfigureMockMvc` and an `ObjectMapper` to test the full API workflow. Ensure the `@BeforeEach` setup block explicitly deletes all data from the repositories before recreating the test users, guaranteeing test idempotency.
>
> The test should execute the following sequence via simulated HTTP requests:
> 1. Send a POST to create a conversation as a Customer.
> 2. Send a PATCH to assign it to an Agent (passing the Manager's ID in the `X-Caller-Id` header).
> 3. Send a PATCH to assign it to *another* Agent (passing the first Agent's ID in the header). Assert that this request returns a clean HTTP 403 Forbidden JSON payload containing the text 'Agents cannot reassign'.
> 4. Send a PATCH to escalate the conversation to the Manager. Assert that this succeeds, the status is `ESCALATED`, and the agent ID matches the Manager.
> 5. Finally, query the `EventLog` endpoint to verify that all state changes were logged correctly with the appropriate caller IDs and action descriptions."
> 6. Assert that the AOP logging aspect correctly logs the execution times for each service method, and that the Prometheus metrics are being collected when the endpoints are hit.
> 7. Optionally, you can also include assertions to verify that the Prometheus metrics endpoint is returning the expected metrics data after the test sequence is executed.

### Phase 6: Automatic Call Distribution (ACD) & Event-Driven Routing
**Prompt:**
> "Implement an Automatic Call Distribution (ACD) system using an Event-Driven architecture to push tickets to agents based on capacity.
> 1. **Domain:** Create an `AgentProfile` entity (One-to-One mapping to `User`) to decouple routing state from the core identity. It must include `activeConversationCount` (Integer), `escalatedConversationCount` (Integer), `isOnline` (Boolean), and `lastAssignedAt` (Timestamp).
> 2. **Concurrency:** In `AgentProfileRepository`, use Spring Data JPA's `@Lock(LockModeType.PESSIMISTIC_WRITE)` when querying eligible profiles to prevent race conditions during high-volume concurrent ticket routing.
> 3. **Routing Service:** Create a `ConversationRoutingService`. When a conversation is created, query for all online agents where `activeConversationCount < 3`. Sort them first by `activeConversationCount` (ascending) and then by `lastAssignedAt` (ascending). Assign the ticket to the top agent, update their counts/timestamps, and save. If no agents are available, leave the conversation `UNASSIGNED`.
> 4. **Event-Driven Queue Processing:** Define a Spring Application Event called `AgentCapacityFreedEvent`. When an agent closes a ticket or escalates a ticket (ensure `escalatedConversationCount` cannot exceed 1), publish this event using `ApplicationEventPublisher`.
> 5. **Event Listener:** Create a `@TransactionalEventListener` that reacts to `AgentCapacityFreedEvent`. It should query the oldest `UNASSIGNED` conversation from the backlog and immediately push it to the routing service to drain the queue.
> 6. **Refactor:** Modify the `ConversationController` to route new tickets through this automated service rather than requiring manual assignment by a Manager."
> 7. **Testing:** Extend the `ConversationIntegrationTest` to simulate multiple concurrent conversation creations and verify that the ACD system correctly assigns tickets to agents based on their current load, and that the event-driven routing properly handles capacity changes when tickets are closed or escalated.
> 8. Optionally, you can also include assertions to verify that the Prometheus metrics are being updated to reflect the number of active conversations per agent and the number of escalated conversations.
> 9. Optionally, you can also include assertions to verify that the AOP logging aspect correctly logs the execution times for the routing service methods, and that the event publishing and handling are functioning as expected.
> 10. Optionally, you can also include assertions to verify that the `AgentCapacityFreedEvent` is being published and handled correctly, and that the routing logic is correctly assigning conversations to agents based on their capacity and last assigned time.

### Phase 7: Spring Security Migration & Code Cleanup
**Prompt:**
> "We are replacing the custom `X-Caller-Id` header implementation with real Spring Security.
> 1. **Dependencies:** Add `spring-boot-starter-security` and `spring-security-test` to `build.gradle`.
> 2. **Domain:** Add a `password` field to the `User` entity. Update the `DatabaseSeeder` to encode a default password (e.g., 'password') using `BCryptPasswordEncoder`.
> 3. **Security Configuration:** Create a `SecurityConfig` with `HTTP Basic` authentication. Create a `CustomUserDetailsService` that loads users by email and maps their database roles to Spring Security `GrantedAuthority` roles (`ROLE_CUSTOMER`, `ROLE_AGENT`, `ROLE_MANAGER`).
> 4. **Cleanup:** Refactor `ConversationController` to remove the `X-Caller-Id` header. Instead, extract the caller's identity via `java.security.Principal` or `@AuthenticationPrincipal`.
> 5. **New Security Tests:** Create a `SecurityIntegrationTest` to verify that unauthenticated endpoints return HTTP 401 Unauthorized, and that cross-role endpoints correctly reject access with HTTP 403 Forbidden.
> 6. **Refactor Existing Tests:** Update `ConversationIntegrationTest` to use Spring Security's `@WithMockUser(username="...", roles="...")` so that the existing integration workflows pass under the new security context."

### Phase 8: Real-Time WebSockets (STOMP)
**Prompt:**
> "Now that Spring Security natively manages identity, implement the real-time chat layer.
> 1. **Dependencies:** Add `spring-boot-starter-websocket` to `build.gradle`.
> 2. **WebSocket Configuration:** Create `WebSocketConfig` implementing `WebSocketMessageBrokerConfigurer`. Register a STOMP endpoint at `/ws` (with SockJS fallback). Enable a simple in-memory message broker routing to `/topic` and application destinations to `/app`. *(Note: Because Spring Security is active, the WebSocket upgrade handshake will automatically inherit the authenticated HTTP session Principal, requiring no custom interceptors).*
> 3. **Messaging Controller:** Create a `MessageController` utilizing `@MessageMapping`. When a user sends a payload to `/app/chat/{conversationId}`, the controller should:
     >    a) Extract the sender's identity from the STOMP `Principal`.
     >    b) Save the message to the database via `MessageService`.
     >    c) Use `SimpMessagingTemplate` to broadcast the saved message out to `/topic/conversations/{conversationId}`.
> 4. **Frontend Client:** Generate a simple `src/main/resources/static/index.html` file using `sockjs` and `stomp.js` that allows a user to authenticate, connect to the `/ws` endpoint, subscribe to a conversation, and send/receive real-time messages."