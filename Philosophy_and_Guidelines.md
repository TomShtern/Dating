# Dating App Architecture Guidebook

## Philosophy — The 3 Immutable Rules

**Rule 1: Domain First.** Code models people and behavior, not frameworks. Domain code is pure and framework-agnostic. You should be able to compile and test your entire domain layer without Spring, JPA, or Vaadin on the classpath.

**Rule 2: Separation of Concerns Strictly Enforced.** Each layer has one job. No sneaky logic in controllers, entities, or utils. When you wonder "where does this belong?" the answer should be obvious and singular.

**Rule 3: Evolve, Don't Rewrite.** Start simple; design extension points (interfaces, strategies) so growth is possible without global surgery. The matching algorithm you ship on day one will not be the algorithm you use on day one hundred. Plan for that.

---

## Technology Stack

| Component    | Version   | Notes                                                        |
|--------------|-----------|--------------------------------------------------------------|
| Java         | 25        | LTS release (September 2025). Use virtual threads, records, sealed types. |
| Spring Boot  | 4.0.1     | Built on Spring Framework 7. Jakarta EE namespace.          |
| Vaadin       | 25.0.2    | UI layer — added last, kept thin.                            |
| Maven        | 3.14.1    | Single module to start; split later if needed.              |
| Database     | PostgreSQL| ACID, JSONB, PostGIS for geospatial later.                  |

---

## Scale Expectations

This architecture is designed for a small-scale dating app. Realistic expectations are around 300 active users, with a ceiling of perhaps 10,000 users maximum. Do not optimize for millions of users. Do not add complexity for scale you will likely never reach. Build something that works first; optimize if and when real usage demands it.

---

## High-Level Architecture: Onion / Hexagonal

Layers point inward toward the domain core. Dependencies flow inward only — outer layers know about inner layers, never the reverse.

```
┌─────────────────────────────────────────────────────────────────┐
│                         api (outermost)                         │
│         Controllers, CLI, Vaadin views — framework-dependent    │
├─────────────────────────────────────────────────────────────────┤
│                        infrastructure                           │
│       Persistence, messaging, external clients — IO layer       │
├─────────────────────────────────────────────────────────────────┤
│                         application                             │
│              Use-cases, orchestration                           │
├─────────────────────────────────────────────────────────────────┤
│                      domain (innermost)                         │
│     Entities, value objects, domain services, events, rules     │
│                    Pure Java. No frameworks.                    │
└─────────────────────────────────────────────────────────────────┘
```

The domain layer is the heart. It contains your business logic, your matching rules, your user model. It has zero dependencies on Spring, JPA, or any framework.

---

## Development Phases

### Phase 0: Pure Domain (No Frameworks)

Before adding any framework complexity, validate your domain model with pure Java.

**Goals:**
- Create all domain model classes: `User`, `Profile`, `Match`, `Swipe`, value objects
- Create repository interfaces in domain with in-memory HashMap implementations
- Create `MatchingService` with one simple strategy (distance OR interest overlap, not both)
- Write unit tests for the core flow: create users → swipe → check match creation
- Create a simple `Main.java` with console I/O to exercise flows manually

**What You Ship:** A runnable JAR with no Spring, no JPA, no web server. Just domain logic exercised via console and unit tests.

**Why This Matters:** You catch modeling mistakes when they're cheap to fix. You prove your domain works before framework complexity obscures bugs. You build confidence that your core logic is sound.

**Exit Criteria:**
- Two users can be created with profiles
- User A can swipe on User B (like/dislike)
- If both users like each other, a Match is created
- All of this works via console and passes unit tests
- Zero framework dependencies

**WARNING — The In-Memory Repository Trap:** In-memory HashMap repositories are useful for Phase 0 and unit tests, but they have limitations you must understand. They do not enforce unique constraints the way a database does. They cannot properly test race conditions (like two users swiping simultaneously). The transition from in-memory to JPA is never "just swap the implementation" — you will find edge cases. Use in-memory repositories to validate your domain logic, but do not trust them to validate your persistence behavior. Move to real database tests (via Testcontainers) as soon as you enter Phase 1.

### Phase 1: MVP

**Goals:**
- Add Spring Boot wiring and dependency injection
- Replace in-memory repositories with JPA implementations
- Add PostgreSQL persistence
- Add REST API endpoints for core flows
- Add basic authentication (JWT)
- Keep console/CLI client for testing
- Add integration tests with Testcontainers

**What You Ship:** A working backend with REST API. Users can register, create profiles, swipe, match, and see their matches via HTTP.

### Phase 2: V1 (Stability & Features)

**Goals:**
- Improve matching with weighted, composable strategies
- Add notifications (push, email) via domain events
- Add messaging (conversations between matched users)
- Add Vaadin UI (finally!)
- Add "who liked me" feature (see asymmetric discovery below)

### Phase 3: V2 (If You Get Here)

**Goals:**
- ML scoring hook via `MLScoreAdapter`
- Advanced search and discovery
- Whatever features real users actually request

---

## Domain Modeling Rules

### Entities vs Value Objects

**Entities** have identity and lifecycle. Two entities are equal if they have the same ID, even if their attributes differ. Entities are mutable over time.

```java
public class User {
    private final UserId id;
    private Profile profile;
    private UserState state;
    
    public void updateProfile(Profile newProfile) { ... }
    public void ban() { ... }
}
```

**Value Objects** are defined by their attributes, not identity. Two value objects with the same attributes are equal. Value objects are immutable.

```java
public record UserId(UUID value) {
    public UserId {
        Objects.requireNonNull(value, "UserId cannot be null");
    }
    
    public static UserId generate() {
        return new UserId(UUID.randomUUID());
    }
}
```

Use Java `record` for value objects — they give you immutability, equals/hashCode, and toString for free.

### When to Use Classes vs Records

| Use Case                                    | Type      |
|---------------------------------------------|-----------|
| Identity-based entity with lifecycle        | `class`   |
| Immutable value object                      | `record`  |
| ID wrapper (`UserId`, `MatchId`)            | `record`  |
| Domain event                                | `record`  |
| Configuration or options                    | `record`  |

### Aggregate Boundaries — Discover, Don't Declare

An **aggregate** is a cluster of entities and value objects treated as a single unit for data changes. Each aggregate has a **root entity** — the only entity external code can reference directly.

**Important:** Aggregate boundaries should emerge from consistency requirements as you build, not be predetermined upfront. Ask these questions as you implement:

- Does creating a Swipe need to atomically update the User's swipe count? If yes, maybe Swipe belongs inside User aggregate. If no, Swipe is its own aggregate.
- Does creating a Match need to atomically create a Conversation? If yes, maybe they're one aggregate. If no, they're separate.
- If User state changes to BANNED, what happens to their active Matches? Does that need to be atomic?

**Starting Point (refine as you learn):**

- **User Aggregate**: User + Profile + Preferences + Location. User is the root.
- **Swipe Aggregate**: Swipe is its own entity. References UserIds.
- **Match Aggregate**: Match is its own entity. References UserIds.
- **Conversation Aggregate** (Phase 2): Conversation + Messages. Conversation is the root.

These boundaries may change as you discover real consistency requirements during implementation. That's okay — the point is to think about them, not to get them perfect upfront.

---

## Critical Domain Concepts

### User State — Simple Enum

User state determines what operations a user can perform. This is a simple enum, not a complex state machine with separate classes.

```java
public enum UserState {
    REGISTERED(false, false, true, false),
    PROFILE_INCOMPLETE(false, false, true, false),
    ACTIVE(true, true, true, true),
    PAUSED(false, false, true, false),
    BANNED(false, false, false, false);
    
    private final boolean canSwipe;
    private final boolean canMessage;
    private final boolean canUpdateProfile;
    private final boolean canBeDiscovered;
    
    UserState(boolean canSwipe, boolean canMessage, 
              boolean canUpdateProfile, boolean canBeDiscovered) {
        this.canSwipe = canSwipe;
        this.canMessage = canMessage;
        this.canUpdateProfile = canUpdateProfile;
        this.canBeDiscovered = canBeDiscovered;
    }
    
    public boolean canSwipe() { return canSwipe; }
    public boolean canMessage() { return canMessage; }
    public boolean canUpdateProfile() { return canUpdateProfile; }
    public boolean canBeDiscovered() { return canBeDiscovered; }
}
```

State transitions are handled in the `User` class:

```java
public class User {
    private UserState state;
    
    public void activate() {
        if (state != UserState.PROFILE_INCOMPLETE && state != UserState.PAUSED) {
            throw new IllegalStateException("Cannot activate from " + state);
        }
        this.state = UserState.ACTIVE;
    }
    
    public void pause() {
        if (state != UserState.ACTIVE) {
            throw new IllegalStateException("Cannot pause from " + state);
        }
        this.state = UserState.PAUSED;
    }
    
    public void ban(String reason) {
        this.state = UserState.BANNED;
        // record reason somewhere if needed
    }
}
```

This is simple, easy to persist (just store the enum name), and avoids the overhead of separate state classes.

### Swipe as First-Class Entity

Swipes are not just method calls — they need persistence for history tracking (don't show the same person twice) and for detecting mutual interest.

```java
public class Swipe {
    private final SwipeId id;
    private final UserId swiperId;
    private final UserId targetId;
    private final SwipeDirection direction;  // LIKE, DISLIKE, SUPER_LIKE
    private final Instant createdAt;
    
    public static Swipe create(UserId swiper, UserId target, SwipeDirection direction) {
        if (swiper.equals(target)) {
            throw new IllegalArgumentException("Cannot swipe on yourself");
        }
        return new Swipe(
            SwipeId.generate(),
            swiper,
            target,
            direction,
            Instant.now()
        );
    }
    
    public boolean isLike() {
        return direction == SwipeDirection.LIKE || direction == SwipeDirection.SUPER_LIKE;
    }
}
```

### Location — Simple Coordinates

```java
public record Location(double lat, double lon) {
    
    public Distance distanceTo(Location other) {
        double km = haversine(this.lat, this.lon, other.lat, other.lon);
        return Distance.ofKilometers(km);
    }
    
    private static double haversine(double lat1, double lon1, double lat2, double lon2) {
        // standard haversine formula
        double R = 6371; // Earth radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }
}
```

### Photos — Keep It Simple

Users have 1-2 photos. That's it. Store them as URLs pointing to wherever you host images (local filesystem for Phase 0-1, S3 or similar if you ever need it).

```java
public record Profile(
    UserId userId,
    String displayName,
    String bio,
    LocalDate birthDate,
    Set<Interest> interests,
    Preferences preferences,
    Location location,
    List<String> photoUrls  // 1-2 URLs, that's it
) {
    public Profile {
        if (photoUrls.size() > 2) {
            throw new IllegalArgumentException("Maximum 2 photos allowed");
        }
    }
    
    public boolean isComplete() {
        return displayName != null && !displayName.isBlank()
            && birthDate != null
            && location != null
            && !photoUrls.isEmpty();
    }
    
    public int age() {
        return Period.between(birthDate, LocalDate.now()).getYears();
    }
}
```

### Asymmetric Discovery — "Who Liked Me"

Beyond symmetric candidate discovery (show me people I might like), dating apps have an important asymmetric pattern: "show me people who already liked me." This is a different query entirely.

**Symmetric:** Find users who match my preferences, exclude those I've already swiped on, score and rank them.

**Asymmetric:** Find users who swiped LIKE on me, whom I haven't swiped on yet. These are high-value prospects because a match is guaranteed if I like them back.

The `SwipeRepository` needs to support this:

```java
public interface SwipeRepository {
    // ... other methods ...
    
    // Find users who liked me but I haven't responded to
    Set<UserId> findPendingLikersFor(UserId userId);
}
```

This feature can wait until Phase 2, but design your Swipe persistence knowing you'll need this query eventually.

### Race Condition Handling for Mutual Swipes

When User A swipes right on User B at the same time User B swipes right on User A, you could create duplicate matches. The solution is idempotent match creation.

```java
public class MatchingService {
    
    public Optional<Match> processSwipe(UserId swiper, UserId target, SwipeDirection direction) {
        // 1. Record the swipe (idempotent - returns existing if duplicate)
        Swipe swipe = swipeRepository.saveIfNotExists(
            Swipe.create(swiper, target, direction)
        );
        
        if (!swipe.isLike()) {
            return Optional.empty();
        }
        
        // 2. Check for mutual interest
        Optional<Swipe> reverseSwipe = swipeRepository.findByPair(target, swiper);
        
        if (reverseSwipe.isEmpty() || !reverseSwipe.get().isLike()) {
            return Optional.empty();
        }
        
        // 3. Create match with canonical ordering (prevents duplicates)
        MatchId matchId = MatchId.canonical(swiper, target);
        
        // 4. Idempotent save - returns existing match if already created
        Match match = matchRepository.saveIfNotExists(
            Match.create(matchId, swiper, target)
        );
        
        // 5. Publish event if this is a new match
        if (match.isNewlyCreated()) {
            eventPublisher.publish(new MatchCreatedEvent(match.id(), swiper, target));
        }
        
        return Optional.of(match);
    }
}
```

**Key Technique:** `MatchId.canonical(a, b)` always orders the two user IDs consistently (lexicographically), so the same pair always produces the same MatchId regardless of who swiped first.

---

## Application Layer: Use Cases

Each use case is an orchestrator class that coordinates domain services and repositories. They are thin — they don't contain business logic, they delegate to domain.

```java
public class FindProspectsUseCase {
    private final UserRepository userRepository;
    private final MatchingService matchingService;
    private final SwipeRepository swipeRepository;
    
    public List<Prospect> execute(UserId requesterId, FindOptions opts) {
        User requester = userRepository.findById(requesterId)
            .orElseThrow(() -> new RuntimeException("User not found: " + requesterId));
        
        if (!requester.getState().canSwipe()) {
            throw new RuntimeException("User cannot swipe in state: " + requester.getState());
        }
        
        Set<UserId> alreadySwiped = swipeRepository.findSwipedUserIds(requesterId);
        
        return matchingService.findProspects(requester, opts, alreadySwiped);
    }
}
```

**Naming Convention:**
- `FindProspectsUseCase` — query, returns potential matches to swipe on
- `SwipeUseCase` — command, performs swipe action
- `GetMatchesUseCase` — query, returns confirmed mutual matches
- `SendMessageUseCase` — command, sends message (Phase 2)

---

## Ports and Adapters

Define interfaces (ports) in domain. Implement them (adapters) in infrastructure.

### Repository Interfaces (in Domain)

```java
public interface UserRepository {
    Optional<User> findById(UserId id);
    void save(User user);
    List<User> findDiscoverableInRadius(Location center, Distance radius, int limit);
    boolean existsById(UserId id);
}

public interface SwipeRepository {
    Swipe saveIfNotExists(Swipe swipe);
    Optional<Swipe> findByPair(UserId swiper, UserId target);
    Set<UserId> findSwipedUserIds(UserId swiper);
    Set<UserId> findPendingLikersFor(UserId userId);  // For "who liked me"
}

public interface MatchRepository {
    Match saveIfNotExists(Match match);
    Optional<Match> findById(MatchId id);
    List<Match> findByUser(UserId userId);
}
```

### Event Publisher (in Domain)

```java
public interface EventPublisher {
    void publish(DomainEvent event);
}

public sealed interface DomainEvent permits 
    UserSwipedEvent, MatchCreatedEvent, MessageSentEvent {
    Instant occurredAt();
}
```

---

## Matching Subsystem — Pluggable by Design

The matching algorithm will evolve. Build it as a pluggable strategy system.

### Strategy Interface

```java
public interface MatchStrategy {
    /**
     * Compute a score for how well two users match.
     * @return score between 0.0 (poor match) and 1.0 (excellent match)
     */
    double score(User candidate, User requester);
    
    /**
     * Human-readable name for logging and debugging.
     */
    String name();
}
```

### Concrete Strategy Example

```java
public class DistanceStrategy implements MatchStrategy {
    private final Distance maxDistance;
    
    public DistanceStrategy(Distance maxDistance) {
        this.maxDistance = maxDistance;
    }
    
    @Override
    public double score(User candidate, User requester) {
        Distance distance = requester.getProfile().location()
            .distanceTo(candidate.getProfile().location());
        
        if (distance.isGreaterThan(maxDistance)) {
            return 0.0;
        }
        
        return 1.0 - (distance.kilometers() / maxDistance.kilometers());
    }
    
    @Override
    public String name() { return "distance"; }
}
```

### Composite Scorer

```java
public class MatchScorer {
    private final List<MatchStrategy> strategies;
    
    public MatchScorer(List<MatchStrategy> strategies) {
        this.strategies = List.copyOf(strategies);
    }
    
    public double score(User candidate, User requester) {
        if (strategies.isEmpty()) {
            return 0.5; // neutral score if no strategies
        }
        
        double sum = 0;
        for (MatchStrategy strategy : strategies) {
            sum += strategy.score(candidate, requester);
        }
        return sum / strategies.size();
    }
}
```

Start with one strategy (probably distance). Add more when you have a reason to.

---

## OOP Design Patterns Used

This architecture uses classic OOP patterns where they provide genuine value.

### Strategy Pattern
**Where:** `MatchStrategy` interface and implementations
**Why:** Matching algorithms will change. Strategies are interchangeable and independently testable.

### Composite Pattern  
**Where:** `MatchScorer` composes multiple `MatchStrategy` instances
**Why:** Complex scoring is built from simpler pieces.

### Factory Method
**Where:** `Match.create(...)`, `Swipe.create(...)`, `UserId.generate()`
**Why:** Encapsulates creation logic and enforces invariants.

### Repository Pattern
**Where:** `UserRepository`, `MatchRepository`, `SwipeRepository` interfaces
**Why:** Abstracts persistence. Domain doesn't know if data is in PostgreSQL or HashMap.

### Observer/Event Pattern
**Where:** `EventPublisher` and `DomainEvent` types
**Why:** Decouples actions from reactions. `MatchingService` publishes `MatchCreatedEvent`; notification system subscribes.

---

## Testing Strategy

### Unit Tests (Fast, Pure)
- Test domain objects: `User`, `Profile`, `Match`, `Swipe`
- Test strategies: `DistanceStrategy`, etc.
- Test domain services: `MatchScorer`, `MatchingService` (with mocked repos)
- No Spring, no database, no I/O

### Integration Tests (Real Dependencies)
- Test repository implementations with real PostgreSQL (Testcontainers)
- Test use cases end-to-end with real infrastructure
- This is where you'll catch the issues that in-memory repos hide

---

## Anti-Patterns — Avoid These

**Business logic in controllers or JPA entities.** Controllers translate HTTP to use cases. JPA entities are persistence details — dumb data carriers in infrastructure layer.

**God services that do everything.** If `UserService` handles registration, profiles, swiping, matching, and messaging, split it by use case.

**Repository interfaces in infrastructure.** They belong in domain. Infrastructure provides implementations.

**Optimizing for scale you don't have.** You have ~300 users, maybe 10K max. Don't add caching, sharding, or async processing until you measure a real problem.

**Over-engineering with patterns.** Use Strategy pattern for matching because it will change. Don't use State pattern for user status when an enum works fine. Every pattern has a cost; pay it only when you get value.

---

## Quick Decision Guide

| Question | Answer |
|----------|--------|
| Where does business logic go? | Domain services or domain objects. Never controllers. |
| Where do I put a new entity? | `domain/` package directly for now. Sub-packages when you have 15+ classes. |
| Where does the repository interface go? | `domain/repository/`. |
| Where does the repository implementation go? | `infrastructure/persistence/`. |
| When do I add Kafka? | Probably never. In-process events are fine for your scale. |
| When do I add Redis? | When you measure a caching need. Not "just in case." |
| When do I add the UI? | After core domain works via console and tests. Phase 2. |
| How many users should I design for? | ~300 realistic, 10K ceiling. Don't optimize for millions. |

---

## What Success Looks Like

After Phase 0:
- You can create users, swipe, and see matches in the console
- All domain logic is tested
- No framework dependencies in domain

After Phase 1 (MVP):
- Working REST API for core flows
- Data persists in PostgreSQL
- Basic auth works

After Phase 2 (V1):
- Users can actually use the app (Vaadin UI)
- Matching works well enough that users find it useful
- "Who liked me" feature works

---

*This guidebook is opinionated. Follow these rules, deviate intentionally when you have good reasons, and build something that works.*
