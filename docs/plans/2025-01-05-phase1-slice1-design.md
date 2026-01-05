# Phase 1 - Vertical Slice 1: User Registration & Profile

**Date:** 2025-01-05
**Status:** Approved
**Scope:** First vertical slice of Phase 1 MVP

---

## Overview

This design covers the first vertical slice of Phase 1: user registration and profile management with JWT authentication, JPA persistence, and Testcontainers integration testing.

**What we're building:**
- Account registration (username + password)
- JWT-based authentication
- Profile completion (triggers PROFILE_INCOMPLETE → ACTIVE transition)
- PostgreSQL persistence via JPA
- Integration tests with Testcontainers

---

## API Design

### Endpoints

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/api/auth/register` | None | Create account |
| `POST` | `/api/auth/login` | None | Authenticate, receive JWT |
| `GET` | `/api/users/me` | JWT | Get current user with profile |
| `PUT` | `/api/users/me/profile` | JWT | Update/complete profile |

### Request/Response Examples

**Register:**
```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "alice",
  "password": "secret123"
}
```
```http
201 Created

{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "username": "alice",
  "state": "PROFILE_INCOMPLETE",
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "expiresIn": 86400
}
```

**Login:**
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "alice",
  "password": "secret123"
}
```
```http
200 OK

{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "username": "alice",
  "state": "PROFILE_INCOMPLETE",
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "expiresIn": 86400
}
```

**Update Profile:**
```http
PUT /api/users/me/profile
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
Content-Type: application/json

{
  "displayName": "Alice",
  "bio": "Hi there!",
  "birthDate": "1995-03-15",
  "location": {
    "latitude": 40.7128,
    "longitude": -74.006
  },
  "photoUrls": ["https://example.com/photo1.jpg"],
  "interests": ["HIKING", "MUSIC"],
  "preferences": {
    "interestedIn": ["female"],
    "ageRange": { "min": 22, "max": 35 },
    "maxDistanceKm": 50
  }
}
```
```http
200 OK

{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "username": "alice",
  "state": "ACTIVE",
  "profile": {
    "displayName": "Alice",
    "bio": "Hi there!",
    "birthDate": "1995-03-15",
    "age": 29,
    "location": { "latitude": 40.7128, "longitude": -74.006 },
    "photoUrls": ["https://example.com/photo1.jpg"],
    "interests": ["HIKING", "MUSIC"],
    "preferences": {
      "interestedIn": ["female"],
      "ageRange": { "min": 22, "max": 35 },
      "maxDistanceKm": 50
    }
  }
}
```

### Error Response Format

All errors follow this structure:

```json
{
  "error": "VALIDATION_FAILED",
  "message": "Invalid request data",
  "details": [
    { "field": "password", "message": "must be at least 8 characters" }
  ]
}
```

| Scenario | HTTP Status | Error Code |
|----------|-------------|------------|
| Validation failure | 400 | `VALIDATION_FAILED` |
| Bad credentials | 401 | `UNAUTHORIZED` |
| Missing/invalid token | 401 | `UNAUTHORIZED` |
| Duplicate username | 409 | `CONFLICT` |
| User not found | 404 | `NOT_FOUND` |
| Server error | 500 | `INTERNAL_ERROR` |

### Input Validation Rules

**Username:**
- Minimum 3 characters
- Maximum 30 characters
- Alphanumeric and underscores only

**Password:**
- Minimum 8 characters

---

## Architecture

### Package Structure

```
com.datingapp
├── domain/                          # EXISTS (mostly unchanged)
│   ├── User.java                    # Add: username field
│   ├── UserRepository.java          # Add: findByUsername(String)
│   └── ...
│
├── application/                     # NEW - use case orchestration
│   ├── AuthService.java
│   └── UserService.java
│
├── infrastructure/
│   ├── persistence/
│   │   ├── inmemory/               # EXISTS (keep for unit tests)
│   │   │   └── InMemoryUserRepository.java  # Add: findByUsername
│   │   └── jpa/                    # NEW
│   │       ├── JpaUserRepository.java
│   │       ├── UserEntity.java
│   │       └── UserMapper.java
│   │
│   └── security/                   # NEW
│       ├── JwtTokenProvider.java
│       └── JwtAuthenticationFilter.java
│
├── api/                            # NEW
│   ├── AuthController.java
│   ├── UserController.java
│   ├── dto/
│   │   ├── RegisterRequest.java
│   │   ├── LoginRequest.java
│   │   ├── LoginResponse.java
│   │   ├── ProfileUpdateRequest.java
│   │   └── UserResponse.java
│   └── exception/
│       ├── GlobalExceptionHandler.java
│       └── ApiError.java
│
└── config/                         # NEW
    └── SecurityConfig.java
```

### Domain Changes

Add `username` to domain `User`:

```java
public class User {
    private final UserId id;
    private final String username;  // NEW
    private Profile profile;
    private UserState state;

    public User(UserId id, String username, Profile profile) {
        this.id = Objects.requireNonNull(id);
        this.username = Objects.requireNonNull(username);
        this.profile = profile;
        this.state = profile.isComplete() ? UserState.ACTIVE : UserState.PROFILE_INCOMPLETE;
    }
    // ...
}
```

Add `findByUsername` to `UserRepository`:

```java
public interface UserRepository {
    Optional<User> findById(UserId id);
    Optional<User> findByUsername(String username);  // NEW
    void save(User user);
    boolean existsById(UserId id);
    // ...
}
```

### JPA Entity Design

Flat table structure (no embedded objects):

```sql
CREATE TABLE users (
    id              UUID PRIMARY KEY,
    username        VARCHAR(30) UNIQUE NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    state           VARCHAR(30) NOT NULL,
    display_name    VARCHAR(100),
    bio             TEXT,
    birth_date      DATE,
    latitude        DOUBLE PRECISION,
    longitude       DOUBLE PRECISION,
    photo_urls      TEXT,           -- comma-separated or JSON array
    interests       TEXT,           -- comma-separated enum names
    interested_in   TEXT,           -- comma-separated
    age_range_min   INTEGER,
    age_range_max   INTEGER,
    max_distance_km DOUBLE PRECISION,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NOT NULL
);
```

**Note:** `password_hash` exists only in `UserEntity`, not in domain `User`. The mapper does not map passwords to/from domain.

### Data Flow

**Registration:**
```
Client POST /api/auth/register
    ↓
AuthController.register(RegisterRequest)
    ↓
AuthService.register(username, password)
    - Check username uniqueness
    - Hash password with BCrypt
    - Create domain User (no password)
    - Save via UserRepository
    - Generate JWT
    ↓
JpaUserRepository.save(user, passwordHash)
    - UserMapper.toEntity(user, passwordHash)
    - JPA persist
    ↓
Return 201 + token + user info
```

**Profile Update:**
```
Client PUT /api/users/me/profile
Authorization: Bearer eyJ...
    ↓
JwtAuthenticationFilter
    - Validate token
    - Set SecurityContext with userId
    ↓
UserController.updateProfile(ProfileUpdateRequest)
    ↓
UserService.updateProfile(userId, profileData)
    - Load User from repository
    - Build Profile value object from DTO
    - Call user.updateProfile(newProfile)
    - Domain handles state transition
    - Save updated User
    ↓
Return 200 + updated user
```

---

## Security

### JWT Configuration

```yaml
# application.yml
jwt:
  secret: ${JWT_SECRET}  # From environment variable
  expiration: 86400      # 24 hours in seconds
```

### Security Filter Chain

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        return http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

**Public endpoints:** `/api/auth/register`, `/api/auth/login`
**Protected endpoints:** Everything else

---

## Testing Strategy

### Test Pyramid

| Layer | Description | Tool | Count |
|-------|-------------|------|-------|
| Unit | Domain logic | JUnit | ~107 existing |
| Unit | UserMapper, JwtTokenProvider | JUnit | ~10 new |
| Integration | JpaUserRepository | Testcontainers | ~8 new |
| Integration | Auth flow (controller → DB) | Testcontainers + MockMvc | ~10 new |
| Integration | Security & error cases | Testcontainers + MockMvc | ~8 new |

**Total new tests:** ~36

### Testcontainers Base Class

```java
@SpringBootTest
@Testcontainers
abstract class IntegrationTestBase {

    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```

### Key Test Cases

**Registration:**
- Success: new user created, token returned
- Failure: duplicate username → 409
- Failure: invalid username format → 400
- Failure: password too short → 400

**Login:**
- Success: correct credentials → token returned
- Failure: wrong password → 401
- Failure: unknown username → 401

**Profile Update:**
- Success: partial profile → state stays INCOMPLETE
- Success: complete profile → state transitions to ACTIVE
- Failure: no auth token → 401
- Failure: expired token → 401
- Failure: invalid data → 400

**Security:**
- Unauthenticated access to /api/users/me → 401
- Malformed token → 401

---

## Files to Create

### New Files (~15)

```
src/main/java/com/datingapp/
├── application/
│   ├── AuthService.java
│   └── UserService.java
├── infrastructure/
│   ├── persistence/jpa/
│   │   ├── JpaUserRepository.java
│   │   ├── UserEntity.java
│   │   └── UserMapper.java
│   └── security/
│       ├── JwtTokenProvider.java
│       └── JwtAuthenticationFilter.java
├── api/
│   ├── AuthController.java
│   ├── UserController.java
│   ├── dto/
│   │   ├── RegisterRequest.java
│   │   ├── LoginRequest.java
│   │   ├── LoginResponse.java
│   │   ├── ProfileUpdateRequest.java
│   │   └── UserResponse.java
│   └── exception/
│       ├── GlobalExceptionHandler.java
│       └── ApiError.java
└── config/
    └── SecurityConfig.java

src/test/java/com/datingapp/
├── infrastructure/
│   ├── persistence/jpa/
│   │   └── JpaUserRepositoryIntegrationTest.java
│   └── security/
│       └── JwtTokenProviderTest.java
├── api/
│   ├── AuthControllerIntegrationTest.java
│   └── UserControllerIntegrationTest.java
└── IntegrationTestBase.java
```

### Modified Files (~4)

- `domain/User.java` - add username field
- `domain/UserRepository.java` - add findByUsername
- `infrastructure/persistence/inmemory/InMemoryUserRepository.java` - implement findByUsername
- Some existing domain tests - update User construction

---

## Decisions Log

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Registration flow | Two-step | Aligns with existing domain state machine |
| Authentication | Simple JWT, 24h | MVP simplicity; upgrade later if needed |
| API style | Resource-based REST | Standard, extensible |
| Table design | Flat | Simple for ~300 users scale |
| Username location | Domain User | Part of identity, not just auth |
| Password location | JPA only | Auth is infrastructure concern |
| Photo handling | Accept URLs | File upload deferred to Phase 2 |

---

## Out of Scope (Phase 2+)

- Refresh tokens
- Photo file upload
- Email verification
- Password reset
- Rate limiting
- Admin endpoints
