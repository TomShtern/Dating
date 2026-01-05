# Package Tree and UML Blueprint

This document provides the concrete structural blueprint for the dating app. Use it alongside the Philosophy and Guidelines document to understand both *what* the structure is and *why* it's organized this way.

---

## Package Tree

The structure is intentionally flat for an MVP. Don't create deep hierarchies until you have enough classes to justify them. Refactor into sub-packages when a package has 15+ classes, not before.

```
com.datingapp
├── domain
│   ├── User.java                    // Aggregate root
│   ├── UserId.java                  // Value object: UUID wrapper
│   ├── UserState.java               // Enum: REGISTERED, ACTIVE, PAUSED, BANNED
│   ├── Profile.java                 // Value object within User aggregate
│   ├── Preferences.java             // Value object: matching preferences
│   ├── Location.java                // Value object: lat/lon coordinates
│   ├── Swipe.java                   // Aggregate root (own aggregate)
│   ├── SwipeId.java                 // Value object: UUID wrapper
│   ├── SwipeDirection.java          // Enum: LIKE, DISLIKE, SUPER_LIKE
│   ├── Match.java                   // Aggregate root
│   ├── MatchId.java                 // Value object: canonical ID from two UserIds
│   ├── Prospect.java                // Value object: user shown before swiping
│   ├── Interest.java                // Enum: HIKING, MUSIC, TRAVEL, etc.
│   ├── Distance.java                // Value object: type-safe distance
│   ├── AgeRange.java                // Value object
│   ├── repository
│   │   ├── UserRepository.java      // Port (interface)
│   │   ├── SwipeRepository.java     // Port (interface)
│   │   └── MatchRepository.java     // Port (interface)
│   ├── matching
│   │   ├── MatchStrategy.java       // Strategy interface
│   │   ├── MatchScorer.java         // Composes strategies
│   │   ├── DistanceStrategy.java    // Concrete strategy
│   │   └── InterestOverlapStrategy.java
│   └── event
│       ├── DomainEvent.java         // Sealed interface
│       ├── EventPublisher.java      // Port (interface)
│       ├── UserSwipedEvent.java
│       └── MatchCreatedEvent.java
├── application
│   └── usecase
│       ├── RegisterUserUseCase.java
│       ├── CompleteProfileUseCase.java
│       ├── FindProspectsUseCase.java
│       ├── SwipeUseCase.java
│       └── GetMatchesUseCase.java
├── infrastructure
│   ├── persistence
│   │   ├── entity
│   │   │   ├── UserEntity.java      // JPA entity
│   │   │   ├── SwipeEntity.java
│   │   │   └── MatchEntity.java
│   │   ├── jpa
│   │   │   ├── JpaUserRepository.java
│   │   │   ├── JpaSwipeRepository.java
│   │   │   ├── JpaMatchRepository.java
│   │   │   └── SpringDataUserRepository.java
│   │   ├── mapper
│   │   │   ├── UserMapper.java
│   │   │   ├── SwipeMapper.java
│   │   │   └── MatchMapper.java
│   │   └── inmemory
│   │       ├── InMemoryUserRepository.java
│   │       ├── InMemorySwipeRepository.java
│   │       └── InMemoryMatchRepository.java
│   └── messaging
│       └── SpringEventPublisher.java
├── api
│   ├── rest
│   │   ├── UserController.java
│   │   ├── MatchingController.java
│   │   └── dto
│   │       ├── CreateUserRequest.java
│   │       ├── UpdateProfileRequest.java
│   │       └── SwipeRequest.java
│   └── cli
│       └── ConsoleClient.java       // Dev/testing CLI
└── config
    ├── AppConfig.java
    └── MatchingConfig.java          // Wires strategies
```

**Notes on structure:**

- Domain classes are flat in `domain/` package. No `domain/model/user/` nesting — that's premature.
- Sub-packages (`repository/`, `matching/`, `event/`) only where there's a clear grouping of related classes.
- No DTO classes in application layer unless you genuinely need to transform domain objects for the API boundary. Often the domain objects (like `Prospect`) are already in the right shape.
- REST DTOs are in `api/rest/dto/` because they're API concerns (request validation, JSON shape), not domain concerns.

---

## Naming Clarification

To avoid confusion with the word "match":

| Term         | Meaning                                                          |
| ------------ | ---------------------------------------------------------------- |
| **Prospect** | A user shown to you before you swipe. Potential match candidate. |
| **Swipe**    | Your action (LIKE, DISLIKE, SUPER_LIKE) on a prospect.           |
| **Match**    | Confirmed mutual interest — both users liked each other.         |

When code says "Match," it always means mutual interest confirmed, never a potential candidate.

---

## UML Class Diagram (Compact Text Form)

Legend: `+` public, `-` private

### Domain Model — User Aggregate

```
┌─────────────────────────────────────────────────────────────────┐
│                        <<aggregate root>>                        │
│                           class User                             │
├─────────────────────────────────────────────────────────────────┤
│ - id: UserId                                                     │
│ - profile: Profile                                               │
│ - state: UserState                                               │
│ - createdAt: Instant                                             │
│ - updatedAt: Instant                                             │
├─────────────────────────────────────────────────────────────────┤
│ + User(UserId, Profile)                                          │
│ + getId(): UserId                                                │
│ + getProfile(): Profile                                          │
│ + getState(): UserState                                          │
│ + updateProfile(Profile): void                                   │
│ + activate(): void                                               │
│ + pause(): void                                                  │
│ + ban(String reason): void                                       │
│ + canSwipe(): boolean              // delegates to state.canSwipe() │
│ + canMessage(): boolean                                          │
│ + canBeDiscovered(): boolean                                     │
└─────────────────────────────────────────────────────────────────┘
          │ has
          ▼
┌─────────────────────────────────────────────────────────────────┐
│                        <<value object>>                          │
│                        record Profile                            │
├─────────────────────────────────────────────────────────────────┤
│ - userId: UserId                                                 │
│ - displayName: String                                            │
│ - bio: String                                                    │
│ - birthDate: LocalDate                                           │
│ - interests: Set<Interest>                                       │
│ - preferences: Preferences                                       │
│ - location: Location                                             │
│ - photoUrls: List<String>          // max 2 photos               │
├─────────────────────────────────────────────────────────────────┤
│ + isComplete(): boolean                                          │
│ + age(): int                                                     │
└─────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────┐  ┌──────────────────────────────────────┐
│       <<value object>>               │  │       <<value object>>               │
│       record Preferences             │  │       record Location                │
├──────────────────────────────────────┤  ├──────────────────────────────────────┤
│ - interestedIn: Set<Gender>          │  │ - lat: double                        │
│ - ageRange: AgeRange                 │  │ - lon: double                        │
│ - maxDistance: Distance              │  ├──────────────────────────────────────┤
├──────────────────────────────────────┤  │ + distanceTo(Location): Distance     │
│ + matches(Profile): boolean          │  └──────────────────────────────────────┘
└──────────────────────────────────────┘  

┌─────────────────────────────────────────────────────────────────┐
│                           <<enum>>                               │
│                          UserState                               │
├─────────────────────────────────────────────────────────────────┤
│ REGISTERED(false, false, true, false)                            │
│ PROFILE_INCOMPLETE(false, false, true, false)                    │
│ ACTIVE(true, true, true, true)                                   │
│ PAUSED(false, false, true, false)                                │
│ BANNED(false, false, false, false)                               │
├─────────────────────────────────────────────────────────────────┤
│ + canSwipe(): boolean                                            │
│ + canMessage(): boolean                                          │
│ + canUpdateProfile(): boolean                                    │
│ + canBeDiscovered(): boolean                                     │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                      <<value object>>                            │
│                       record UserId                              │
├─────────────────────────────────────────────────────────────────┤
│ - value: UUID                                                    │
├─────────────────────────────────────────────────────────────────┤
│ + UserId(UUID)                         // compact constructor    │
│ + generate(): UserId                   // factory method         │
│ + value(): UUID                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Domain Model — Swipe Aggregate

```
┌─────────────────────────────────────────────────────────────────┐
│                        <<aggregate root>>                        │
│                          class Swipe                             │
├─────────────────────────────────────────────────────────────────┤
│ - id: SwipeId                                                    │
│ - swiperId: UserId                                               │
│ - targetId: UserId                                               │
│ - direction: SwipeDirection                                      │
│ - createdAt: Instant                                             │
├─────────────────────────────────────────────────────────────────┤
│ + create(swiper, target, direction): Swipe  // factory method    │
│ + getId(): SwipeId                                               │
│ + getSwiperId(): UserId                                          │
│ + getTargetId(): UserId                                          │
│ + getDirection(): SwipeDirection                                 │
│ + isLike(): boolean                  // LIKE or SUPER_LIKE       │
└─────────────────────────────────────────────────────────────────┘

┌───────────────────────────────────┐
│           <<enum>>                │
│        SwipeDirection             │
├───────────────────────────────────┤
│ LIKE                              │
│ DISLIKE                           │
│ SUPER_LIKE                        │
└───────────────────────────────────┘
```

### Domain Model — Match Aggregate

```
┌─────────────────────────────────────────────────────────────────┐
│                        <<aggregate root>>                        │
│                          class Match                             │
├─────────────────────────────────────────────────────────────────┤
│ - id: MatchId                                                    │
│ - userA: UserId                      // Canonical order: userA < userB │
│ - userB: UserId                                                  │
│ - createdAt: Instant                                             │
│ - newlyCreated: boolean              // For idempotency tracking │
├─────────────────────────────────────────────────────────────────┤
│ + create(userA, userB): Match         // factory, enforces order │
│ + getId(): MatchId                                               │
│ + involves(UserId): boolean          // true if user is in match │
│ + otherUser(UserId): UserId          // get the other party      │
│ + isNewlyCreated(): boolean                                      │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                        <<value object>>                          │
│                         record MatchId                           │
├─────────────────────────────────────────────────────────────────┤
│ - value: String                      // Deterministic from users │
├─────────────────────────────────────────────────────────────────┤
│ + canonical(UserId a, UserId b): MatchId                         │
│ + value(): String                                                │
└─────────────────────────────────────────────────────────────────┘

Implementation: canonical() sorts the two UUIDs lexicographically
and concatenates them. Same pair always produces same MatchId.
```

### Domain Model — Prospect (What Users See Before Swiping)

```
┌─────────────────────────────────────────────────────────────────┐
│                        <<value object>>                          │
│                        record Prospect                           │
├─────────────────────────────────────────────────────────────────┤
│ - userId: UserId                                                 │
│ - displayName: String                                            │
│ - age: int                                                       │
│ - bio: String                                                    │
│ - photoUrls: List<String>                                        │
│ - distance: Distance                 // "3 km away"              │
│ - sharedInterests: Set<Interest>                                 │
│ - score: double                      // For internal ranking     │
└─────────────────────────────────────────────────────────────────┘

Prospect is computed by MatchingService from User data.
It's what gets shown to users when browsing — a read-only view
with just the info needed for the swipe decision.
```

### Domain Services

```
┌─────────────────────────────────────────────────────────────────┐
│                      <<domain service>>                          │
│                      class MatchingService                       │
├─────────────────────────────────────────────────────────────────┤
│ - scorer: MatchScorer                                            │
│ - userRepository: UserRepository                                 │
│ - swipeRepository: SwipeRepository                               │
│ - matchRepository: MatchRepository                               │
│ - eventPublisher: EventPublisher                                 │
├─────────────────────────────────────────────────────────────────┤
│ + findProspects(User requester, FindOptions opts,                │
│                 Set<UserId> excludedIds): List<Prospect>         │
│ + processSwipe(UserId swiper, UserId target,                     │
│                SwipeDirection dir): Optional<Match>              │
└─────────────────────────────────────────────────────────────────┘
          │ uses
          ▼
┌─────────────────────────────────────────────────────────────────┐
│                      <<domain service>>                          │
│                       class MatchScorer                          │
├─────────────────────────────────────────────────────────────────┤
│ - strategies: List<MatchStrategy>                                │
├─────────────────────────────────────────────────────────────────┤
│ + score(User candidate, User requester): double                  │
└─────────────────────────────────────────────────────────────────┘
          │ composes
          ▼
┌─────────────────────────────────────────────────────────────────┐
│                      <<strategy interface>>                      │
│                     interface MatchStrategy                      │
├─────────────────────────────────────────────────────────────────┤
│ + score(User candidate, User requester): double                  │
│ + name(): String                                                 │
└─────────────────────────────────────────────────────────────────┘
          △
          │ implemented by
          │
    ┌─────┴───────────────┐
    │                     │
┌───┴────────┐    ┌───────┴──────────┐
│ Distance   │    │ InterestOverlap  │
│ Strategy   │    │ Strategy         │
└────────────┘    └──────────────────┘

Start with one strategy. Add more when needed.
```

### Repository Interfaces (in Domain)

```
┌─────────────────────────────────────────────────────────────────┐
│                      <<port/interface>>                          │
│                     UserRepository                               │
├─────────────────────────────────────────────────────────────────┤
│ + findById(UserId): Optional<User>                               │
│ + save(User): void                                               │
│ + findDiscoverableInRadius(Location center,                      │
│                            Distance radius,                      │
│                            int limit): List<User>                │
│ + existsById(UserId): boolean                                    │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                      <<port/interface>>                          │
│                     SwipeRepository                              │
├─────────────────────────────────────────────────────────────────┤
│ + saveIfNotExists(Swipe): Swipe                                  │
│ + findByPair(UserId swiper, UserId target): Optional<Swipe>      │
│ + findSwipedUserIds(UserId swiper): Set<UserId>                  │
│ + findPendingLikersFor(UserId userId): Set<UserId>               │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                      <<port/interface>>                          │
│                     MatchRepository                              │
├─────────────────────────────────────────────────────────────────┤
│ + saveIfNotExists(Match): Match                                  │
│ + findById(MatchId): Optional<Match>                             │
│ + findByUser(UserId): List<Match>                                │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                      <<port/interface>>                          │
│                     EventPublisher                               │
├─────────────────────────────────────────────────────────────────┤
│ + publish(DomainEvent): void                                     │
└─────────────────────────────────────────────────────────────────┘
```

### Application Layer — Use Cases

```
┌─────────────────────────────────────────────────────────────────┐
│                        <<use case>>                              │
│                   class FindProspectsUseCase                     │
├─────────────────────────────────────────────────────────────────┤
│ - userRepository: UserRepository                                 │
│ - swipeRepository: SwipeRepository                               │
│ - matchingService: MatchingService                               │
├─────────────────────────────────────────────────────────────────┤
│ + execute(UserId requester, FindOptions opts): List<Prospect>    │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                        <<use case>>                              │
│                      class SwipeUseCase                          │
├─────────────────────────────────────────────────────────────────┤
│ - userRepository: UserRepository                                 │
│ - matchingService: MatchingService                               │
├─────────────────────────────────────────────────────────────────┤
│ + execute(UserId swiper, UserId target,                          │
│           SwipeDirection dir): SwipeResult                       │
└─────────────────────────────────────────────────────────────────┘

SwipeResult contains:
- matched: boolean
- match: Optional<Match>
```

### Infrastructure — Repository Implementations

```
┌─────────────────────────────────────────────────────────────────┐
│                        <<adapter>>                               │
│                     JpaUserRepository                            │
│                  implements UserRepository                       │
├─────────────────────────────────────────────────────────────────┤
│ - springDataRepo: SpringDataUserRepository                       │
│ - mapper: UserMapper                                             │
├─────────────────────────────────────────────────────────────────┤
│ + findById(UserId): Optional<User>                               │
│ + save(User): void                                               │
│ + findDiscoverableInRadius(...): List<User>                      │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                        <<adapter>>                               │
│                   InMemoryUserRepository                         │
│                  implements UserRepository                       │
├─────────────────────────────────────────────────────────────────┤
│ - storage: Map<UserId, User>                                     │
├─────────────────────────────────────────────────────────────────┤
│ + findById(UserId): Optional<User>                               │
│ + save(User): void                                               │
│ // WARNING: Does not enforce DB constraints.                     │
│ // Use for Phase 0 domain validation only.                       │
│ // Move to Testcontainers + real DB for integration tests.       │
└─────────────────────────────────────────────────────────────────┘
```

### API Layer — Controllers

```
┌─────────────────────────────────────────────────────────────────┐
│                        <<adapter>>                               │
│                    MatchingController                            │
├─────────────────────────────────────────────────────────────────┤
│ - findProspectsUseCase: FindProspectsUseCase                     │
│ - swipeUseCase: SwipeUseCase                                     │
├─────────────────────────────────────────────────────────────────┤
│ @GetMapping("/users/{userId}/prospects")                         │
│ + getProspects(userId, options): ResponseEntity<List<Prospect>>  │
│                                                                  │
│ @PostMapping("/users/{userId}/swipes")                           │
│ + swipe(userId, SwipeRequest): ResponseEntity<SwipeResult>       │
└─────────────────────────────────────────────────────────────────┘

Controllers are thin: validate input, call use case, return response.
NO business logic in controllers.
```

---

## Domain Events

```
┌─────────────────────────────────────────────────────────────────┐
│                    <<sealed interface>>                          │
│                       DomainEvent                                │
├─────────────────────────────────────────────────────────────────┤
│ + occurredAt(): Instant                                          │
└─────────────────────────────────────────────────────────────────┘
         permits
            │
    ┌───────┴────────┬─────────────────┐
    │                │                 │
┌───┴────────┐ ┌─────┴──────────┐ ┌────┴───────────┐
│UserSwiped  │ │MatchCreated    │ │MessageSent     │
│Event       │ │Event           │ │Event (Phase 2) │
├────────────┤ ├────────────────┤ ├────────────────┤
│swiperId    │ │matchId         │ │conversationId  │
│targetId    │ │userA           │ │senderId        │
│direction   │ │userB           │ │messageId       │
└────────────┘ └────────────────┘ └────────────────┘
```

---

## Key Relationships Summary

```
FindProspectsUseCase
    │
    ├──uses──> MatchingService
    │              │
    │              ├──uses──> MatchScorer
    │              │              │
    │              │              └──composes──> MatchStrategy (1 or more)
    │              │
    │              ├──uses──> UserRepository (port)
    │              │              │
    │              │              └──implemented by──> JpaUserRepository
    │              │                                   InMemoryUserRepository
    │              │
    │              └──uses──> EventPublisher (port)
    │                             │
    │                             └──implemented by──> SpringEventPublisher
    │
    └──uses──> SwipeRepository (port)

User (aggregate root)
    │
    ├──has──> Profile (value object)
    │             │
    │             ├──has──> Preferences (value object)
    │             │
    │             └──has──> Location (value object)
    │
    └──has──> UserState (enum)
```

---

## Key Method Signatures

```java
// Domain Service
List<Prospect> MatchingService.findProspects(
    User requester, 
    FindOptions opts, 
    Set<UserId> excludedIds
)

Optional<Match> MatchingService.processSwipe(
    UserId swiper, 
    UserId target, 
    SwipeDirection direction
)

// Match Scorer
double MatchScorer.score(User candidate, User requester)

// Strategy
double MatchStrategy.score(User candidate, User requester)

// Repositories
Optional<User> UserRepository.findById(UserId id)
void UserRepository.save(User user)
Swipe SwipeRepository.saveIfNotExists(Swipe swipe)
Optional<Swipe> SwipeRepository.findByPair(UserId swiper, UserId target)
Set<UserId> SwipeRepository.findPendingLikersFor(UserId userId)
Match MatchRepository.saveIfNotExists(Match match)

// Use Cases
List<Prospect> FindProspectsUseCase.execute(UserId requesterId, FindOptions opts)
SwipeResult SwipeUseCase.execute(UserId swiper, UserId target, SwipeDirection direction)

// Factories
UserId UserId.generate()
MatchId MatchId.canonical(UserId a, UserId b)
Swipe Swipe.create(UserId swiper, UserId target, SwipeDirection dir)
Match Match.create(UserId a, UserId b)
```

---

## Core Flow (One-Liner)

User opens app → `FindProspectsUseCase` fetches prospects via `MatchingService.findProspects` (excludes already-swiped) → User sees one prospect at a time → User swipes → `SwipeUseCase` calls `MatchingService.processSwipe` → `MatchingService` records swipe via `SwipeRepository`, checks for mutual like → if mutual, creates `Match` with canonical ID via `MatchRepository.saveIfNotExists`, publishes `MatchCreatedEvent` → Both users see "It's a Match!"

---

## Phase 0 Implementation Order

1. **Value Objects First**
   
   - `UserId`, `SwipeId`, `MatchId`
   - `Distance`, `AgeRange`, `Location`
   - `Interest` enum

2. **UserState Enum**
   
   - Simple enum with permission methods

3. **User Aggregate**
   
   - `Profile` record
   - `Preferences` record  
   - `User` class with state management

4. **Swipe Aggregate**
   
   - `SwipeDirection` enum
   - `Swipe` class with factory method

5. **Match Aggregate**
   
   - `MatchId.canonical()` implementation
   - `Match` class

6. **Repository Interfaces**
   
   - `UserRepository`, `SwipeRepository`, `MatchRepository`
   - In-memory implementations (with warning comments about limitations)

7. **Matching Logic**
   
   - `MatchStrategy` interface
   - One simple strategy (e.g., `DistanceStrategy`)
   - `MatchScorer` with single strategy
   - `MatchingService`

8. **Console Client**
   
   - Simple `Main.java` with menu
   - Create users, swipe, view matches

9. **Unit Tests**
   
   - Test all value objects
   - Test user state transitions
   - Test matching service with in-memory repos

**Exit Phase 0 when:** Two users can swipe on each other via console and see a match created — all without Spring, JPA, or any framework.

---

*This blueprint is your structural reference. Keep it flat, keep it simple, refactor when complexity demands it.*
