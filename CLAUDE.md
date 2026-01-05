# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A Java-based dating application using **Spring Boot 3.5.9** with **Vaadin 24.5.5** for the UI. The app implements a swipe-based matching system where users can like/pass on profiles, and matched users can chat.

## Technology Stack

- **Java 17** with Spring Boot 3.5.9
- **Vaadin 24.5.5** (server-side Java UI framework, no separate frontend code)
- **Spring Security** with VaadinWebSecurity integration
- **Spring Data JPA** with Hibernate
- **PostgreSQL** (production), **H2** (tests)
- **Maven** for build management

## Common Commands

```bash
# Build the project
./mvnw clean package

# Run the application (dev mode)
./mvnw spring-boot:run

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=MatchingServiceTest

# Run a single test method
./mvnw test -Dtest=MatchingServiceTest#likeUser_shouldReturnMatched_whenMutualLike

# Build for production (optimizes Vaadin frontend bundle)
./mvnw clean package -Pproduction
```

## Architecture

### Layer Structure

```
com.datingapp
├── config/          # Security & Spring configuration
├── model/           # JPA entities (User, Match, Message, UserInteraction)
├── repository/      # Spring Data JPA repositories
├── service/         # Business logic layer
└── views/           # Vaadin UI views (all routes)
```

### Core Domain Model

- **User** - Profile with username, displayName, age, gender, bio. Uses UUID primary key.
- **UserInteraction** - Records LIKE/PASS actions between users. Has unique constraint on (from_user, to_user).
- **Match** - Created when two users mutually like each other. Links user1 and user2.
- **Message** - Chat messages within a Match context.

### Matching Flow

1. `MatchingService.getPotentialMatches()` excludes already-interacted and matched users
2. `likeUser()` checks for mutual like - if found, creates a Match and returns `MATCHED`
3. `passUser()` records a PASS interaction to hide from future suggestions

### Authentication

- Uses Spring Security with Vaadin's `VaadinWebSecurity` base class
- BCrypt password encoding
- `CustomUserDetailsService` loads users from database for authentication
- Public routes: `/register`, `/VAADIN/**`, `/frontend/**`

### Vaadin Views

| Route | View Class | Description |
|-------|------------|-------------|
| `/` or `/matches` | `MatchingView` | Main swipe interface for liking/passing |
| `/login` | `LoginView` | Authentication |
| `/register` | `RegisterView` | User registration |
| `/chat` | `ChatView` | Message list with matches |
| `/profile` | `ProfileView` | Edit user profile |

All authenticated views use `MainLayout` as the parent layout (provides navigation drawer).

## Code Patterns

### Entity Pattern

Entities use explicit builders (no Lombok) for IDE compatibility:
```java
User user = User.builder()
    .username("john")
    .displayName("John")
    .age(25)
    .build();
```

### Repository Pattern

Custom JPQL queries in repositories for complex lookups:
```java
@Query("SELECT m FROM Match m WHERE m.user1 = :user OR m.user2 = :user")
List<Match> findMatchesForUser(@Param("user") User user);
```

### Service Layer

- All services are `@Transactional` at class level
- Read-only operations marked with `@Transactional(readOnly = true)`
- Services use constructor injection

### Vaadin Views

- Views marked with `@Route` and `@PageTitle`
- Authenticated views require `@PermitAll` or role-specific annotation
- Services injected via constructor (marked `transient` for serialization safety)

## Testing

- Unit tests use Mockito with `@ExtendWith(MockitoExtension.class)`
- Integration tests use `@SpringBootTest` with H2 in-memory database
- Test profile activated via `application-test.properties`

## Database

Production requires PostgreSQL running on `localhost:5432/datingapp`.

Schema auto-updated via `spring.jpa.hibernate.ddl-auto=update`.
