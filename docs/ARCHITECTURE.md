# 🏗️ معماری سیستم Smart Campus Platform

**توضیح کامل معماری، الگوها، و امنیت**

---

## 📋 فهرست

1. [نمای کلی](#1-نمای-کلی)
2. [معماری Microservices](#2-معماری-microservices)
3. [الگوهای طراحی](#3-الگوهای-طراحی)
4. [جریان داده](#4-جریان-داده)
5. [امنیت](#5-امنیت)
6. [مقیاس‌پذیری](#6-مقیاسپذیری)

---

## 1. نمای کلی

### 1.1 Architecture Diagram

```
                    ┌─────────────────┐
                    │  React Frontend │
                    │  (TypeScript)   │
                    │   Port: 4173    │
                    └────────┬────────┘
                             │ HTTPS/REST
                             │
              ┌──────────────▼──────────────┐
              │     API Gateway (8080)      │
              │  ┌─────────────────────┐   │
              │  │ Rate Limiting (Redis)│   │
              │  │ JWT Validation       │   │
              │  │ CORS Handling        │   │
              │  │ Retry Mechanism      │   │
              │  │ Health Aggregation   │   │
              │  └─────────────────────┘   │
              └─┬───┬───┬───┬───┬───┬───┬─┘
                │   │   │   │   │   │   │
        ┌───────┴┐ ┌┴──┐│   │   │   │   │
        │        │ │   ││   │   │   │   │
     ┌──▼──┐  ┌─▼─▼┐ ┌▼▼──┐ ┌▼──┐ ┌▼──┐ ┌▼───┐
     │Auth │  │Book│ │Mark│ │Exam│ │Order│ │Notif│
     │:8081│  │:8082│ │:8083│ │:8085│ │:8084│ │:8086│
     └──┬──┘  └──┬─┘ └──┬─┘ └──┬─┘ └──┬─┘ └──┬─┘
        │        │      │      │      │      │
        │        │      └──────┴──────┴──────┘
        │        │             │
        │        │      ┌──────▼──────┐
        │        │      │  RabbitMQ   │
        │        │      │ Event Bus   │
        │        │      │  :5672      │
        │        │      └─────────────┘
        │        │
     ┌──▼──┐  ┌─▼──┐  ┌───────┐
     │AuthDB│  │Book│  │ Redis │
     │:5433 │  │DB  │  │ :6379 │
     └──────┘  │:5434│  └───────┘
               └────┘
          ... (6 PostgreSQL DBs)
```

### 1.2 Tech Stack

**Backend:**
- Java 17, Spring Boot 3.3.1
- Spring Cloud Gateway
- Spring Security + JWT
- Spring Data JPA
- RabbitMQ (AMQP)
- PostgreSQL 15
- Redis 7
- Docker

**Frontend:**
- React 18
- TypeScript
- Vite
- React Router DOM

---

## 2. معماری Microservices

### 2.1 سرویس‌ها

| سرویس | پورت | دیتابیس | مسئولیت |
|-------|------|---------|---------|
| **API Gateway** | 8080 | - | ورودی واحد، مسیریابی، امنیت |
| **Auth Service** | 8081 | authdb | احراز هویت، مدیریت کاربران |
| **Booking Service** | 8082 | bookingdb | رزرو منابع (اتاق، لابراتوار) |
| **Marketplace Service** | 8083 | marketdb | فروش تیکت، مدیریت موجودی |
| **Order Service** | 8084 | orderdb | مدیریت سفارشات، پرداخت |
| **Exam Service** | 8085 | examdb | آزمون‌ها، نمرات |
| **Notification Service** | 8086 | notificationdb | اعلان‌ها، پیام‌ها |
| **IoT Service** | 8087 | - | داده‌های سنسورها (mock) |

### 2.2 Database per Service

هر سرویس دیتابیس مستقل دارد:

```sql
-- Auth Database
CREATE DATABASE authdb;
  Tables: user_account

-- Booking Database  
CREATE DATABASE bookingdb;
  Tables: resource, reservation

-- Marketplace Database
CREATE DATABASE marketdb;
  Tables: ticket, inventory

-- Order Database
CREATE DATABASE orderdb;
  Tables: order, order_item

-- Exam Database
CREATE DATABASE examdb;
  Tables: course, exam, exam_attempt

-- Notification Database
CREATE DATABASE notificationdb;
  Tables: notification
```

**مزایا:**
- ✅ ایزوله‌سازی کامل
- ✅ مقیاس‌پذیری مستقل
- ✅ عدم وابستگی
- ✅ انتخاب تکنولوژی آزاد

---

## 3. الگوهای طراحی

### 3.1 API Gateway Pattern

**محل پیاده‌سازی:** `api-gateway/`

```java
// application.yml
spring:
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: http://auth-service:8081
          predicates:
            - Path=/auth/**
```

**قابلیت‌ها:**
1. **Single Entry Point** - همه درخواست‌ها از یک نقطه
2. **Rate Limiting** - 10 req/sec per IP
3. **JWT Validation** - بررسی token قبل از forward
4. **Retry Mechanism** - 3 تلاش با backoff
5. **CORS Handling** - مدیریت متمرکز CORS
6. **Health Aggregation** - جمع‌آوری وضعیت سرویس‌ها

**جریان درخواست:**
```
Client → API Gateway → JWT Check → Rate Limit → Route → Service
                          ↓ fail          ↓ limit      ↓ error
                        401            429          Retry 3x
```

---

### 3.2 Saga Pattern (Choreography)

**محل پیاده‌سازی:** `order-service/CheckoutService.java`

**سناریو:** خرید تیکت با مراحل multiple

```java
@Transactional
public Order checkout(List<OrderItemDto> items, String username) {
    Order order = new Order();
    order.setStatus(PENDING);
    
    try {
        // Step 1: Reserve Inventory
        for (OrderItem item : items) {
            restTemplate.postForEntity(
                marketplaceUrl + "/inventory/reserve",
                item, Void.class
            );
        }
        
        // Step 2: Process Payment (mock)
        processPayment(order.getTotalAmount());
        
        // Step 3: Confirm Order
        order.setStatus(CONFIRMED);
        orderRepository.save(order);
        
        // Step 4: Publish Event
        eventPublisher.publish("order.confirmed", Map.of(
            "orderId", order.getId(),
            "userId", username,
            "status", "confirmed"
        ));
        
        return order;
        
    } catch (Exception e) {
        // Compensating Transaction
        for (OrderItem item : items) {
            restTemplate.postForEntity(
                marketplaceUrl + "/inventory/release",
                item, Void.class
            );
        }
        
        order.setStatus(FAILED);
        orderRepository.save(order);
        
        eventPublisher.publish("order.failed", Map.of(
            "orderId", order.getId(),
            "userId", username,
            "reason", e.getMessage()
        ));
        
        throw new CheckoutException("Checkout failed: " + e.getMessage());
    }
}
```

**جریان:**
```
Order → Reserve Inventory → Payment → Confirm → Publish Event
   ↓ fail     ↓ fail          ↓ fail
Rollback ← Release ← Compensate
```

---

### 3.3 Circuit Breaker Pattern

**محل پیاده‌سازی:** `exam-service/NotificationClient.java`

```java
@Component
public class NotificationClient {
    
    @CircuitBreaker(name = "notification", fallbackMethod = "fallback")
    public void sendExamStarted(String userId, Long examId) {
        restTemplate.postForEntity(
            notificationUrl + "/notifications",
            Map.of("user", userId, "message", "Exam started"),
            Void.class
        );
    }
    
    private void fallback(String userId, Long examId, Throwable t) {
        log.warn("Notification service unavailable. User: {}, Exam: {}", 
            userId, examId);
        // Store locally or queue for later
    }
}
```

**تنظیمات:**
```yaml
resilience4j:
  circuitbreaker:
    instances:
      notification:
        sliding-window-size: 5
        minimum-number-of-calls: 2
        wait-duration-in-open-state: 5s
```

**States:**
```
CLOSED (همه درخواست‌ها می‌رود)
   ↓ failure rate > threshold
OPEN (همه درخواست‌ها reject می‌شود، fallback اجرا می‌شود)
   ↓ wait-duration
HALF-OPEN (چند درخواست test می‌شود)
   ↓ success → CLOSED
   ↓ failure → OPEN
```

---

### 3.4 Event-Driven Architecture

**محل پیاده‌سازی:** RabbitMQ Events

**Queues:**
- `order.events` - سفارشات
- `reservation.events` - رزروها

**Publisher (Order Service):**
```java
@Component
public class OrderEventPublisher {
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    public void publish(String eventType, Map<String, Object> payload) {
        rabbitTemplate.convertAndSend(
            "order.events",
            eventType,
            payload
        );
    }
}
```

**Consumer (Notification Service):**
```java
@Component
public class NotificationController {
    
    @RabbitListener(queues = "order.events")
    public void handleOrderEvents(
        @Header("type") String eventType,
        @Payload Map<String, Object> payload
    ) {
        if ("order.confirmed".equals(eventType)) {
            String userId = (String) payload.get("userId");
            notificationService.create(
                userId,
                "Order #" + payload.get("orderId") + " confirmed"
            );
            
            // Send to faculty too
            notificationService.create(
                "faculty",
                "New order from " + userId
            );
        }
    }
}
```

**مزایا:**
- ✅ Loose Coupling - سرویس‌ها مستقل هستند
- ✅ Scalability - consumers متعدد
- ✅ Reliability - پیام‌ها persistent هستند
- ✅ Asynchronous - non-blocking

---

## 4. جریان داده

### 4.1 Authentication Flow

```
1. User → Login (POST /auth/login)
2. Auth Service → Validate credentials
3. Auth Service → Generate JWT (exp: 10h)
4. Auth Service → Return {token, role, tenantId}

5. User → Request with Header: Authorization: Bearer TOKEN
6. API Gateway → Extract & Validate JWT
7. API Gateway → Forward with headers:
   - X-User: username
   - X-Role: role  
   - X-Tenant-Id: tenantId
   - Authorization: Bearer TOKEN
8. Service → Extract from headers → Set TenantContext
9. Service → Process request (filtered by tenantId)
10. Service → Return response
```

### 4.2 Booking Flow

```
Student → Reserve Resource

1. POST /booking/reservations
   Body: {resourceId, startTime, endTime}
   
2. Booking Service → Check availability
   Query with PESSIMISTIC_WRITE lock
   
3. If available:
   - Create Reservation (status: CONFIRMED)
   - Publish event: reservation.created
   
4. Notification Service → Listen event
   - Create notification for user
   - Create notification for faculty
   
5. Return: {reservationId, status: CONFIRMED}
```

**Concurrent Booking Prevention:**
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT r FROM Reservation r WHERE ...")
List<Reservation> findConflicting(...);
```

### 4.3 Order Flow (Saga)

```
Student → Checkout

1. POST /orders/checkout
   Body: [{ticketId, quantity}, ...]
   
2. Order Service → Start Saga:
   
   Step 1: Reserve Inventory
   POST /marketplace/inventory/reserve
   
   Step 2: Process Payment (mock)
   
   Step 3: Confirm Order
   order.status = CONFIRMED
   
   Step 4: Publish Event
   eventPublisher.publish("order.confirmed")
   
3. Notification Service → Listen
   Create notifications
   
4. Return: {orderId, status, items}

IF any step fails:
  Compensating: Release inventory
  order.status = FAILED
  Publish: "order.failed"
```

---

## 5. امنیت

### 5.1 Authentication & Authorization

**JWT Structure:**
```json
{
  "sub": "amin",
  "role": "FACULTY",
  "tenantId": "default",
  "iat": 1703759200,
  "exp": 1703795200
}
```

**RBAC Matrix:**

| Endpoint | FACULTY | TEACHER | STUDENT |
|----------|---------|---------|---------|
| POST /users | ✅ | ❌ | ❌ |
| GET /resources | ✅ | ✅ | ✅ |
| POST /resources | ✅ | ❌ | ❌ |
| POST /reservations | ✅ | ✅ | ✅ |
| DELETE /reservations/{id} | ✅ | ❌ | own only |
| POST /exams | ✅ | ✅ | ❌ |
| GET /exams | ✅ | ✅ | ✅ |
| POST /orders | ❌ | ✅ | ✅ |

**Implementation:**
```java
@PreAuthorize("hasRole('FACULTY')")
public ResponseEntity<?> createResource(...) { }

@PreAuthorize("hasAnyRole('FACULTY','TEACHER','STUDENT')")
public ResponseEntity<?> reserve(...) { }
```

### 5.2 Multi-Tenancy

**TenantContext (ThreadLocal):**
```java
public class TenantContext {
    private static ThreadLocal<String> currentTenant = new ThreadLocal<>();
    
    public static void setTenantId(String tenantId) {
        currentTenant.set(tenantId);
    }
    
    public static String getTenantId() {
        return currentTenant.get();
    }
}
```

**JWT Filter:**
```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(...) {
        String token = extractToken(request);
        Claims claims = jwtService.extractClaims(token);
        
        // Set context
        TenantContext.setTenantId(claims.get("tenantId"));
        
        // Set authentication
        Authentication auth = new UsernamePasswordAuthenticationToken(
            claims.getSubject(),
            null,
            List.of(new SimpleGrantedAuthority("ROLE_" + claims.get("role")))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
        
        filterChain.doFilter(request, response);
    }
}
```

**Query Filtering:**
```java
@Service
public class ReservationService {
    
    public List<Reservation> findAll() {
        String tenantId = TenantContext.getTenantId();
        return repository.findByTenantId(tenantId);
    }
}
```

**Data Isolation Guarantee:**
- هر query به `tenantId` فیلتر می‌شود
- کاربران فقط داده‌های تنانت خود را می‌بینند
- Cross-tenant access غیرممکن است

### 5.3 Rate Limiting

**Implementation:** Redis Token Bucket

```yaml
spring:
  cloud:
    gateway:
      default-filters:
        - name: RequestRateLimiter
          args:
            redis-rate-limiter.replenishRate: 10    # tokens/sec
            redis-rate-limiter.burstCapacity: 20    # max tokens
            key-resolver: "#{@ipAddressKeyResolver}"
```

```java
@Bean
public KeyResolver ipAddressKeyResolver() {
    return exchange -> {
        String ip = exchange.getRequest()
            .getRemoteAddress()
            .getAddress()
            .getHostAddress();
        return Mono.just(ip);
    };
}
```

**Algorithm:**
```
Bucket capacity: 20 tokens
Refill rate: 10 tokens/second

Request arrives:
  If bucket has tokens → Allow, consume 1 token
  Else → Reject with 429 Too Many Requests
  
Bucket refills continuously at 10/sec
```

### 5.4 Password Security

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(); // Work factor: 10
}

// Hashing
String hashed = passwordEncoder.encode("13831383");
// Result: $2a$10$N9qo8uLOickgx2ZMRZoMye...

// Verification
boolean matches = passwordEncoder.matches("13831383", hashed);
```

---

## 6. مقیاس‌پذیری

### 6.1 Horizontal Scaling

هر سرویس می‌تواند مستقل scale شود:

```yaml
# docker-compose.yml
booking-service:
  image: booking-service:latest
  deploy:
    replicas: 3  # 3 instances
```

**Load Balancing:** API Gateway به صورت خودکار round-robin

### 6.2 Database Scaling

**Read Replicas:**
```yaml
spring:
  datasource:
    primary:
      url: jdbc:postgresql://primary:5432/bookingdb
    replica:
      url: jdbc:postgresql://replica:5432/bookingdb
```

**Connection Pooling:**
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
```

### 6.3 Caching Strategy

**Redis Cache:**
- Rate limiting keys: `RL:{ip}:{timestamp}`
- Session cache (future): `SESSION:{userId}`

**In-Memory Cache:**
- JWT validation (no DB call needed)

### 6.4 Message Queue

**RabbitMQ Scalability:**
- Multiple consumers per queue
- Persistent messages
- Dead letter queue (DLQ)

---

## 📊 Metrics & Monitoring

### Health Checks

```bash
# Individual service
GET http://localhost:8081/actuator/health

# Aggregated (API Gateway)
GET http://localhost:8080/actuator/health
```

**Response:**
```json
{
  "status": "UP",
  "components": {
    "auth-service": {"status": "UP"},
    "booking-service": {"status": "UP"},
    "db": {"status": "UP"},
    "redis": {"status": "UP"}
  }
}
```

---

## 🎯 Design Principles

✅ **Single Responsibility** - هر سرویس یک کار
✅ **Loose Coupling** - ارتباط از طریق events
✅ **High Cohesion** - قابلیت‌های مرتبط در یک سرویس
✅ **Fault Isolation** - خرابی یک سرویس به بقیه سرایت نمی‌کند
✅ **Independent Deployment** - سرویس‌ها مستقل deploy می‌شوند
✅ **Eventual Consistency** - consistency نهایی با events

---

**نسخه**: 1.0  
**تاریخ**: 28 دسامبر 2025

