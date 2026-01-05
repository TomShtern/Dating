# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

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
- **Java** (`java`) `JDK 25 & 8` - Java Development Kit.

</system_tools>

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
