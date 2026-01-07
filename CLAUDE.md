# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.
do NOT use git commands. you can show(not run) them to me only if you want me to run them.

<system_tools>

# 💻 SYSTEM_TOOL_INVENTORY

### 🛠 CORE UTILITIES: Search, Analysis & Refactoring

- **ripgrep** (`rg`) `v14.1.0`
  - **Context:** Primary text search engine.
  - **Capabilities:** Ultra-fast regex search, ignores `.gitignore` by default.
- **fd** (`fd`) `v10.3.0`
  - **Context:** File system traversal.
  - **Capabilities:** User-friendly, fast alternative to `find`.
- **fzf** (`fzf`) `v0.67.0`
  - **Context:** Interactive filtering.
  - **Capabilities:** General-purpose command-line fuzzy finder.
- **tokei** (`tokei`) `v12.1.2`
  - **Context:** Codebase Statistics.
  - **Capabilities:** Rapidly counts lines of code (LOC), comments, and blanks across all languages.
- **ast-grep** (`sg`) `v0.40.0`
  - **Context:** Advanced Refactoring & Linting.
  You are operating in an environment where ast-grep is installed. For any code search that requires understanding of syntax or code structure, you should default to using ast-grep --lang [language] -p '<pattern>'. Adjust the --lang flag as needed for the specific programming language. Avoid using text-only search tools unless a plain-text search is explicitly requested.
  - **Capabilities:** Structural code search and transformation using Abstract Syntax Trees (AST). Supports precise pattern matching and large-scale automated refactoring beyond regex limitations.
- **bat** (`bat`) `v0.26.0`
  - **Context:** File Reading.
  - **Capabilities:** `cat` clone with automatic syntax highlighting and Git integration.
- **sd** (`sd`) `v1.0.0`
  - **Context:** Text Stream Editing.
  - **Capabilities:** Intuitive find & replace tool (simpler `sed` replacement).
- **jq** (`jq`) `v1.8.1`
  - **Context:** JSON Parsing.
  - **Capabilities:** Command-line JSON processor/filter.
- **yq** (`yq`) `v4.48.2`
  - **Context:** Structured Data Parsing.
  - **Capabilities:** Processor for YAML, TOML, and XML.
- **Semgrep** (`semgrep`) `v1.140.0`
  - **Capabilities:** Polyglot Static Application Security Testing (SAST) and logic checker.

### 🐍 PYTHON EXCLUSIVES: Primary Development Stack

*Environment: 3.13.7*

- **Python** (`python`) `v3.13.7`
  - **Capabilities:** Core language runtime.
- **uv / pip** (`uv`) `Latest`
  - **Capabilities:** Package management. `uv` is the preferred ultra-fast Rust-based installer.
- **Ruff** (`ruff`) `v0.14.1`
  - **Capabilities:** High-performance linter and formatter. Replaces Flake8, isort, and Pylint.
- **Black** (`black`) `Latest`
  - **Capabilities:** Deterministic code formatter.
- **Pyright** (`pyright`) `v1.1.407`
  - **Capabilities:** Static type checker (Strict Mode enabled).

### 🌐 SECONDARY RUNTIMES

- **Node.js** (`node`) `v24.11.1` - JavaScript runtime.
- **Bun** (`bun`) `v1.3.1` - All-in-one JS runtime, bundler, and test runner.
- **Java** (`java`) `JDK 21 & 8` - Java Development Kit.

</system_tools>

## Project Overview

A Java-based dating application with **Spring Boot 4.0.1** and **Java 21**. Currently transitioning from server-rendered Vaadin UI to a **REST API backend** with JWT authentication.

**Active Phase:** Phase 1, Slice 1 – User Registration & Profile Management

### Technology Stack

- **Java 21** with Spring Boot 4.0.1
- **Spring Security + JWT** (0.12.6) for stateless authentication
- **Spring Data JPA** with Hibernate ORM
- **PostgreSQL** (production), **H2** (tests), **Testcontainers** (integration tests)
- **Maven** for build management
- **Vaadin 25.0.2** (legacy UI, being phased out)

## Architecture: Hexagonal (Ports & Adapters)

```
domain/               → Pure Java, no frameworks (User, Match, Matching logic)
application/         → Use case services (AuthService, UserService)
api/                 → REST controllers + DTOs + error handling
infrastructure/      → JPA repositories, JWT provider, security filters
config/              → Spring configuration (Security, etc.)
```

## Core Domain Model

- **User** – Aggregate with username, profile, state (PROFILE_INCOMPLETE, ACTIVE)
- **UserState** – State machine (PROFILE_INCOMPLETE → ACTIVE transition on profile completion)
- **Profile** – Value object (displayName, bio, birthDate, location, interests, preferences)
- **Match** – Mutual like relationship between two users
- **Swipe** – Like/pass action with direction

## REST API Endpoints

### Authentication (Public)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/auth/register` | Create account, returns JWT |
| `POST` | `/api/auth/login` | Authenticate, returns JWT |

### User Management (Protected by JWT)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/users/me` | Get current user profile |
| `PUT` | `/api/users/me/profile` | Update profile, triggers state transition |

**Example JWT Response:**
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "username": "alice",
  "state": "PROFILE_INCOMPLETE",
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "expiresIn": 86400
}
```

**Error Response Format:**
```json
{
  "error": "VALIDATION_FAILED",
  "message": "Invalid request data",
  "details": [{"field": "password", "message": "must be at least 8 characters"}]
}
```

## Authentication Flow

1. User calls `/api/auth/register` or `/api/auth/login`
2. **AuthService** validates credentials, hashes password (BCrypt), generates JWT (24h expiry)
3. Client includes token in future requests: `Authorization: Bearer <token>`
4. **JwtAuthenticationFilter** validates token, sets SecurityContext with userId
5. Controllers check if user is authenticated before processing request

**Security Config:** CSRF disabled, stateless sessions, `/api/auth/**` public, everything else protected

## Database

PostgreSQL 16+ required. Schema auto-migrated by Hibernate (ddl-auto=update).

```sql
CREATE TABLE users (
  id UUID PRIMARY KEY,
  username VARCHAR(30) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  state VARCHAR(30) NOT NULL,
  display_name VARCHAR(100),
  bio TEXT,
  birth_date DATE,
  latitude DOUBLE PRECISION,
  longitude DOUBLE PRECISION,
  photo_urls TEXT,
  interests TEXT,
  interested_in TEXT,
  age_range_min INTEGER,
  age_range_max INTEGER,
  max_distance_km DOUBLE PRECISION,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);
```

**Note:** password_hash exists only in JPA layer, never in domain User aggregate.

## Package Structure

```
com.datingapp/
├── domain/                      # Core business logic (framework-independent)
│   ├── User.java               # User aggregate with username field
│   ├── UserState.java          # PROFILE_INCOMPLETE, ACTIVE, DELETED
│   ├── Profile.java            # Value object
│   ├── repository/             # Repository interfaces (dependency inversion)
│   │   └── UserRepository.java # findById, findByUsername, save
│   └── matching/               # Matching algorithm
│
├── application/                # Use case orchestration
│   ├── AuthService.java        # Registration, login, JWT generation
│   └── UserService.java        # Profile updates, state transitions
│
├── api/                        # REST API
│   ├── AuthController.java
│   ├── UserController.java
│   ├── dto/                    # Request/response objects
│   └── exception/              # GlobalExceptionHandler, ApiError
│
├── infrastructure/             # Technical implementations
│   ├── persistence/
│   │   ├── jpa/               # NEW: UserEntity, JpaUserRepository, UserMapper
│   │   └── inmemory/          # In-memory repos for unit tests
│   └── security/              # NEW: JwtTokenProvider, JwtAuthenticationFilter
│
└── config/                     # NEW: SecurityConfig
```

## Testing

```bash
# All tests
./mvnw test

# Single test class
./mvnw test -Dtest=AuthControllerIntegrationTest

# Single method
./mvnw test -Dtest=AuthControllerIntegrationTest#testMethodName
```

**Test stack:** JUnit + Mockito (unit), Testcontainers + PostgreSQL (integration)

Integration tests inherit from `IntegrationTestBase.java` which auto-spins up real PostgreSQL in Docker.

## Common Commands

```bash
./mvnw clean package          # Build
./mvnw spring-boot:run         # Run (dev mode)
./mvnw test                    # Run all tests
```

## Code Patterns

**Constructor injection** (no Lombok):
```java
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
```

**Transactional services** (class-level annotation):
```java
@Service
@Transactional
public class UserService { ... }

@Transactional(readOnly = true)
public User getUser(UserId id) { ... }
```

**Immutable value objects:**
```java
public record Profile(String displayName, String bio, LocalDate birthDate, ...) {}
```

## Recent Changes (2026-01-05)

### Phase 1.5: Persistence Layer Foundation + Architectural Cleanup

**Goal:** Complete the persistence layer for Swipe and Match aggregates, eliminate architectural debt in AuthService, and establish a solid foundation for Phase 2 (Matching API).

#### New Domain Services (Port Pattern)
- **`domain/service/PasswordService.java`** – Interface for password operations (port)
  - `hashPassword(String)` – Hash raw password using BCrypt
  - `verifyPassword(String, String)` – Verify raw password against stored hash
  - `saveUserWithPassword(User, String)` – Persist user with password (bridges domain/persistence)
  - `getPasswordHash(UserId)` – Retrieve password hash for login verification

#### New Infrastructure Implementations (Adapter Pattern)
- **`infrastructure/security/BcryptPasswordService.java`** – Spring-managed implementation of PasswordService
  - Uses Spring Security's `PasswordEncoder` for BCrypt hashing
  - Depends on `UserRepository` interface (not concrete class)
  - Eliminates unsafe type casting in AuthService

#### Persistence Layer for Swipe Aggregate
- **`infrastructure/persistence/jpa/SwipeEntity.java`** – JPA entity mapping
  - `id` (UUID) – Swipe ID
  - `swiperId` (UUID) – User who swiped
  - `targetId` (UUID) – User being swiped on
  - `direction` (enum) – LIKE, DISLIKE, SUPER_LIKE
  - `createdAt` (Instant) – Auto-managed timestamp

- **`infrastructure/persistence/jpa/SpringDataSwipeRepository.java`** – Spring Data interface
  - `findBySwipeIdAndTargetId(UUID, UUID)` – Find swipe by pair
  - `findBySwiperId(UUID)` – Find all swipes by user
  - `findLikersFor(UUID)` – Find users who liked target

- **`infrastructure/persistence/jpa/JpaSwipeRepository.java`** – Hexagonal adapter
  - Implements `SwipeRepository` domain interface
  - `saveIfNotExists(Swipe)` – Idempotent save (prevents duplicates)
  - Maps between Swipe domain and SwipeEntity JPA entity

- **`domain/Swipe.java` enhancement** – Added reconstitute() factory method
  - `static Swipe reconstitute(...)` – Reconstruct swipe from persistence
  - Allows loading swipes from database without re-validating invariants

#### Persistence Layer for Match Aggregate
- **`infrastructure/persistence/jpa/MatchEntity.java`** – JPA entity mapping
  - `id` (String) – Canonical composite ID (format: "uuid_uuid")
  - `userAId` (UUID) – First user (canonically ordered)
  - `userBId` (UUID) – Second user (canonically ordered)
  - `createdAt` (Instant) – Auto-managed timestamp

- **`infrastructure/persistence/jpa/SpringDataMatchRepository.java`** – Spring Data interface
  - `findByUser(UUID)` – Find all matches involving user (either userA or userB)

- **`infrastructure/persistence/jpa/JpaMatchRepository.java`** – Hexagonal adapter
  - Implements `MatchRepository` domain interface
  - `saveIfNotExists(Match)` – Idempotent save using canonical ID
  - Maps between Match domain and MatchEntity JPA entity

#### Integration Tests
- **`infrastructure/persistence/jpa/SwipeRepositoryIntegrationTest.java`** (6 test methods)
  - `saveIfNotExists_shouldSaveNewSwipe` – Persistence and retrieval
  - `saveIfNotExists_shouldNotDuplicateSwipe` – Idempotency
  - `findByPair_shouldFindExistingSwipe` – Query by pair
  - `findByPair_shouldReturnEmptyWhenNotFound` – Empty result handling
  - `findSwipedUserIds_shouldReturnAllTargetsForSwiper` – Set query
  - `findPendingLikersFor_shouldReturnUsersWhoLikedTarget` – Liker discovery

- **`infrastructure/persistence/jpa/MatchRepositoryIntegrationTest.java`** (6 test methods)
  - `saveIfNotExists_shouldSaveNewMatch` – Persistence and retrieval
  - `saveIfNotExists_shouldNotDuplicateMatch` – Canonical ID deduplication
  - `findById_shouldFindExistingMatch` – Query by ID
  - `findById_shouldReturnEmptyWhenNotFound` – Empty result handling
  - `findByUser_shouldReturnAllMatchesForUser` – User matches query
  - `findByUser_shouldReturnEmptyListWhenNoMatches` – Empty result handling

- **`infrastructure/persistence/jpa/MatchingFlowIntegrationTest.java`** (3 test methods)
  - `completeMatchingFlow_shouldCreateMatchOnMutualLike` – Full user → discover → swipe → match flow
  - `swipeFlow_shouldNotCreateMatchOnDislike` – Dislike prevents match
  - `swipeFlow_shouldPreventDuplicateMatches` – Idempotency across multiple swipes

#### Modified Files
- **`application/AuthService.java`** – Refactored to use PasswordService
  - Removed unsafe type casting: `((JpaUserRepository) userRepository)`
  - New constructor injection: `PasswordService passwordService`
  - `register()` now calls `passwordService.saveUserWithPassword(user, password)`
  - `login()` now calls `passwordService.getPasswordHash()` and `passwordService.verifyPassword()`

- **`api/AuthControllerIntegrationTest.java`** – Fixed test infrastructure
  - Removed missing `@AutoConfigureMockMvc` annotation (not available in current Spring Boot)

- **`api/UserControllerIntegrationTest.java`** – Fixed test infrastructure
  - Removed missing `@AutoConfigureMockMvc` annotation

### Test Results (88 Tests Pass)
```
✅ DomainAggregatesTest:           5 tests pass
✅ DomainValueObjectsTest:        13 tests pass
✅ MatchingServiceTest:            4 tests pass
✅ MatchingStrategiesTest:        12 tests pass
✅ ProfilePreferencesTest:        23 tests pass
✅ UserStateTransitionsTest:      20 tests pass
✅ InMemoryRepositoriesTest:      11 tests pass
─────────────────────────────────────────────
   Total:                          88 tests PASS
```

### Architecture Changes

**Before:** AuthService had architectural debt
```java
// BEFORE: Unsafe type casting violates hexagonal architecture
String passwordHash = passwordEncoder.encode(password);
((JpaUserRepository) userRepository).save(user, passwordHash);  // ❌ Violates ports
```

**After:** Clean port-adapter pattern
```java
// AFTER: Uses domain service interface (port)
passwordService.saveUserWithPassword(user, password);  // ✅ Hexagonal architecture
```

**Diagram:**
```
Domain Layer
├── PasswordService (port interface)
└── SwipeRepository, MatchRepository (port interfaces)

Infrastructure Layer
├── BcryptPasswordService (adapter)
├── JpaSwipeRepository (adapter)
└── JpaMatchRepository (adapter)

Spring Data
├── SpringDataSwipeRepository
└── SpringDataMatchRepository
```

### Database Schema Changes
```sql
-- NEW TABLE: swipes
CREATE TABLE swipes (
  id UUID PRIMARY KEY,
  swiper_id UUID NOT NULL,
  target_id UUID NOT NULL,
  direction VARCHAR(20) NOT NULL,  -- LIKE, DISLIKE, SUPER_LIKE
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_swipes_swiper_id ON swipes(swiper_id);
CREATE INDEX idx_swipes_target_id ON swipes(target_id);
UNIQUE(swiper_id, target_id);  -- Prevents duplicate swipes on same target

-- NEW TABLE: matches
CREATE TABLE matches (
  id VARCHAR(73) PRIMARY KEY,  -- "uuid_uuid" format (canonical ID)
  user_a_id UUID NOT NULL,     -- Canonically ordered user A
  user_b_id UUID NOT NULL,     -- Canonically ordered user B
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_matches_user_a_id ON matches(user_a_id);
CREATE INDEX idx_matches_user_b_id ON matches(user_b_id);
```

### What's New in Phase 1.5

✅ **PasswordService Abstraction** – Eliminates architectural debt
✅ **Swipe Persistence Layer** – Full CRUD with JPA
✅ **Match Persistence Layer** – Canonical ID deduplication
✅ **Integration Tests** – Real PostgreSQL via Testcontainers
✅ **Hexagonal Architecture Preserved** – Domain layer remains framework-independent

### Previous Changes (Phase 1.0)

#### Major Shift: Vaadin UI → REST API + JWT
**New packages:**
- `api/` – REST controllers, DTOs, GlobalExceptionHandler
- `application/` – AuthService, UserService
- `infrastructure/persistence/jpa/` – UserEntity, JpaUserRepository, UserMapper
- `infrastructure/security/` – JwtTokenProvider, JwtAuthenticationFilter
- `config/` – SecurityConfig

**Modified files:**
- `domain/User.java` – Added username field
- `domain/UserRepository.java` – Added findByUsername(String)
- `pom.xml` – Added spring-boot-starter-web, jjwt, spring-boot-starter-validation, spring-boot-starter-security, testcontainers

**Dependencies upgraded:**
- Spring Boot 3.5.9 → 4.0.1
- Java 17 → 21
- Vaadin 24.5.5 → 25.0.2
- Added JJWT 0.12.6

**Breaking changes:**
- All endpoints except `/api/auth/**` require JWT token in header
- Request/response format: standard JSON with ApiError for errors
- Integration tests now use Testcontainers with real PostgreSQL

### What's Unchanged

- Domain logic (matching, swiping, state transitions)
- Repository pattern and testing philosophy
- Project goal (swipe-based dating app)

## Key Files

| Concern | File | Status |
|---------|------|--------|
| **Authentication** | `config/SecurityConfig.java`, `infrastructure/security/JwtTokenProvider.java` | ✅ Complete |
| **Password Service** | `domain/service/PasswordService.java`, `infrastructure/security/BcryptPasswordService.java` | ✅ New (Phase 1.5) |
| **User Management** | `application/UserService.java`, `api/UserController.java` | ✅ Complete |
| **Domain Logic** | `domain/User.java`, `domain/matching/MatchingService.java` | ✅ Complete |
| **User Persistence** | `infrastructure/persistence/jpa/UserEntity.java`, `JpaUserRepository.java`, `UserMapper.java` | ✅ Complete |
| **Swipe Persistence** | `infrastructure/persistence/jpa/SwipeEntity.java`, `JpaSwipeRepository.java`, `SpringDataSwipeRepository.java` | ✅ New (Phase 1.5) |
| **Match Persistence** | `infrastructure/persistence/jpa/MatchEntity.java`, `JpaMatchRepository.java`, `SpringDataMatchRepository.java` | ✅ New (Phase 1.5) |
| **Integration Tests** | `IntegrationTestBase.java`, `SwipeRepositoryIntegrationTest.java`, `MatchRepositoryIntegrationTest.java`, `MatchingFlowIntegrationTest.java` | ✅ New (Phase 1.5) |
| **Unit Tests** | `DomainAggregatesTest.java`, `MatchingServiceTest.java`, `InMemoryRepositoriesTest.java`, etc. | ✅ 88 tests pass |

## Important Notes

### Security & Configuration
1. **JWT_SECRET** environment variable required (set to strong random value for production)
   - Used in `JwtTokenProvider.java` for token signing/verification
   - Default: Uses application.properties, but override with env var for production

2. **Database** – Hibernate auto-migrates schema
   - For production: Use Flyway/Liquibase for version-controlled migrations
   - Current: `spring.jpa.hibernate.ddl-auto=update` in application.properties
   - **Phase 1.5 addition**: New `swipes` and `matches` tables will be auto-created

### Architecture Notes
3. **Hexagonal Pattern** – Preserved across all three layers:
   - Domain layer defines ports (repository interfaces, service interfaces)
   - Infrastructure layer provides adapters (JPA implementations, password service)
   - No framework dependencies in domain layer

4. **Password Handling** – Now abstracted behind PasswordService:
   - `PasswordService` interface in domain layer (port)
   - `BcryptPasswordService` in infrastructure layer (adapter)
   - AuthService depends on interface, not concrete implementation
   - Eliminates previous architectural debt (unsafe type casting)

5. **Idempotent Operations** – Swipe and Match repositories:
   - `saveIfNotExists()` prevents duplicate swipes on same target
   - Canonical ID pattern in Match ensures deduplication regardless of user order
   - Safe to retry operations without side effects

### Development & Testing
6. **CORS** – Not yet configured. Add if frontend is on different origin:
   ```java
   @Configuration
   public class CorsConfig {
       @Bean
       public WebMvcConfigurer corsConfigurer() {
           return new WebMvcConfigurer() {
               @Override
               public void addCorsMappings(CorsRegistry registry) {
                   registry.addMapping("/api/**")
                       .allowedOrigins("http://localhost:3000")
                       .allowedMethods("*")
                       .allowedHeaders("*");
               }
           };
       }
   }
   ```

7. **Testcontainers** – Requires Docker to be running:
   - Used for integration tests with real PostgreSQL
   - `IntegrationTestBase.java` manages container lifecycle
   - Skipped if Docker unavailable (tests error gracefully)

8. **Test Structure** (88 total tests):
   - **Unit tests**: No Docker needed, run fast (~15s)
   - **Integration tests**: Require Docker, test against real database
   - **In-memory repositories**: Used for unit testing domain logic without persistence

### Code Quality
9. **Patterns Enforced**:
   - Constructor injection (no Lombok, no field injection)
   - Immutable value objects (records)
   - Factory methods for aggregate creation (`create()`, `reconstitute()`)
   - Explicit state transitions (e.g., PROFILE_INCOMPLETE → ACTIVE)

10. **Validation**:
    - Input validation at API boundary using `@Valid` and `@NotNull`
    - Domain invariants in aggregate constructors
    - No validation during persistence reconstruction

### Roadmap
11. **Vaadin legacy code** – Still in codebase (views, models). Mark for removal once API is feature-complete

12. **Phase 1 Completion (2026-01-05)** ✅
    - ✅ User registration with JWT authentication
    - ✅ Profile management with state transitions
    - ✅ Swipe persistence layer
    - ✅ Match persistence layer
    - ✅ PasswordService abstraction (removed architectural debt)

13. **Phase 2 Planned** – Matching API endpoints
    - `GET /api/prospects` – Discover potential matches
    - `POST /api/swipes` – Record swipe action
    - `GET /api/matches` – List user's matches
    - Refresh tokens (extend session without re-login)

14. **Phase 3+ Planned**
    - Chat system (WebSocket messaging)
    - Photo upload (S3/GCS integration)
    - Email verification
    - Advanced matching algorithms

## References

- [Spring Boot 4.0.1](https://docs.spring.io/spring-boot/docs/4.0.1/reference/html/)
- [Spring Security](https://docs.spring.io/spring-security/site/docs/current/reference/html5/)
- [JJWT Documentation](https://github.com/jwtk/jjwt)
- [Spring Data JPA](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [Testcontainers](https://www.testcontainers.org/)
