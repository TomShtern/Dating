# Persistence Layer Developer Guide

**For:** Developers working with Swipe, Match, and Password persistence
**Updated:** January 5, 2026

---

## Quick Start

### Using PasswordService (Application Layer)

```java
// In AuthService or any service
@Service
public class MyService {
    private final PasswordService passwordService;

    public MyService(PasswordService passwordService) {
        this.passwordService = passwordService;
    }

    public void handlePasswordChange(UserId userId, String newPassword) {
        // Hash the password
        String hash = passwordService.hashPassword(newPassword);

        // Or persist user with password in one step
        User user = userRepository.findById(userId).orElseThrow();
        passwordService.saveUserWithPassword(user, newPassword);

        // Or verify password during login
        String storedHash = passwordService.getPasswordHash(userId);
        if (passwordService.verifyPassword(providedPassword, storedHash)) {
            // Password matches!
        }
    }
}
```

**Key Rule:** Always inject `PasswordService` interface, never `BcryptPasswordService` directly.

---

### Using SwipeRepository (Domain Layer)

```java
// Inject the domain interface, not the JPA implementation
@Service
public class MatchingService {
    private final SwipeRepository swipeRepository;

    public MatchingService(SwipeRepository swipeRepository) {
        this.swipeRepository = swipeRepository;
    }

    public Optional<Match> processSwipe(UserId swiper, UserId target, SwipeDirection direction) {
        // Create a new swipe
        Swipe swipe = Swipe.create(swiper, target, direction);

        // Save it (idempotent - safe to retry)
        // If swipe already exists for this (swiper, target) pair, returns existing
        Swipe saved = swipeRepository.saveIfNotExists(swipe);

        // Find swipes made by a user
        Set<UserId> swipedIds = swipeRepository.findSwipedUserIds(swiper);

        // Find who liked someone
        Set<UserId> likers = swipeRepository.findPendingLikersFor(target);

        // Check for mutual interest
        Optional<Swipe> reverseSwipe = swipeRepository.findByPair(target, swiper);

        if (reverseSwipe.isPresent() && reverseSwipe.get().isLike()) {
            // They like each other! Create match...
        }
    }
}
```

**Key Rules:**
- Inject `SwipeRepository` (interface), not `JpaSwipeRepository`
- Create swipes with `Swipe.create()` (validates invariants)
- Use `saveIfNotExists()` for idempotent saves
- Safe to retry after network failures

---

### Using MatchRepository (Domain Layer)

```java
@Service
public class MatchingService {
    private final MatchRepository matchRepository;

    public MatchingService(MatchRepository matchRepository) {
        this.matchRepository = matchRepository;
    }

    public void createMatch(UserId userId1, UserId userId2) {
        // Create match (automatically computes canonical ID)
        Match match = Match.create(userId1, userId2);

        // Save it (idempotent - safe to retry)
        // If match already exists with canonical ID, returns existing
        Match saved = matchRepository.saveIfNotExists(match);

        // Retrieve match by canonical ID
        Optional<Match> found = matchRepository.findById(match.getId());

        // Get all matches for a user
        List<Match> userMatches = matchRepository.findByUser(userId1);
    }
}
```

**Key Rules:**
- Inject `MatchRepository` (interface), not `JpaMatchRepository`
- Create matches with `Match.create()` (validates invariants)
- Use `saveIfNotExists()` for idempotent saves
- Canonical ID computed automatically in domain
- Safe to retry after network failures

---

## Design Patterns Used

### 1. Idempotent Operations

**What:** Calling the same operation multiple times has the same effect as calling it once.

**Example:**
```java
// These two calls have the same result
Swipe swipe1 = swipeRepository.saveIfNotExists(Swipe.create(alice, bob, LIKE));
Swipe swipe2 = swipeRepository.saveIfNotExists(Swipe.create(alice, bob, LIKE));

// swipe1.getId() == swipe2.getId()  ✅ Same swipe
```

**Why:** Network failures happen. If a request times out, the client might retry. Idempotent operations ensure retries are safe (no duplicates).

**Implementation:**
- Swipe: Checks `(swiperId, targetId)` uniqueness
- Match: Checks canonical ID uniqueness

---

### 2. Reconstitute Pattern

**What:** Separate factory methods for creation vs. persistence reconstruction.

**Example:**
```java
// Creation: validates invariants
Swipe newSwipe = Swipe.create(swiper, target, direction);  // Throws if swiper == target

// Reconstitution: trusts persistence
Swipe fromDb = Swipe.reconstitute(swipeId, swiper, target, direction, createdAt);
```

**Why:**
- Clear intent (new vs. loaded)
- Skip validation on trusted data
- Better performance
- Easier to test

---

### 3. Canonical ID for Symmetric Relationships

**What:** Match between A↔B is the same regardless of creation order.

**Example:**
```java
MatchId id1 = MatchId.canonical(alice, bob);  // "aaa..._bbb..."
MatchId id2 = MatchId.canonical(bob, alice);  // "aaa..._bbb..." (same!)

// Both Match(alice, bob) and Match(bob, alice) map to same canonical ID
```

**Why:** Prevents duplicate matches. Perfect deduplication.

---

### 4. Adapter Pattern (Hexagonal Architecture)

**What:** Domain defines interface (port), infrastructure provides implementation (adapter).

```
Domain Layer (Port)
    ↑
    │ implements
    │
Infrastructure Layer (Adapter)
    ↓
    └─→ Spring Data (Technical Detail)
```

**Example:**
```java
// Domain port (interface)
public interface SwipeRepository {
    Swipe saveIfNotExists(Swipe swipe);
    Optional<Swipe> findByPair(UserId swiperId, UserId targetId);
    Set<UserId> findSwipedUserIds(UserId swiperId);
    Set<UserId> findPendingLikersFor(UserId userId);
}

// Infrastructure adapter
@Repository
public class JpaSwipeRepository implements SwipeRepository {
    private final SpringDataSwipeRepository springDataRepo;
    // ... implementations use Spring Data
}

// Spring Data (technical detail)
public interface SpringDataSwipeRepository extends JpaRepository<SwipeEntity, UUID> {
    // ... Spring Data methods
}
```

**Why:** Domain logic stays independent of Spring, JPA, databases. Easy to test and swap implementations.

---

## Database Operations

### Query Examples

#### Find swipes by user
```java
Set<UserId> targetIds = swipeRepository.findSwipedUserIds(myUserId);
// Returns set of all users I've swiped on
```

#### Find who liked me
```java
Set<UserId> admirers = swipeRepository.findPendingLikersFor(myUserId);
// Returns set of all users who LIKE or SUPER_LIKE'd me
```

#### Find match by ID
```java
Optional<Match> match = matchRepository.findById(matchId);
if (match.isPresent()) {
    UserId otherUser = match.get().otherUser(myUserId);
    // Get the person I matched with
}
```

#### Find my matches
```java
List<Match> myMatches = matchRepository.findByUser(myUserId);
for (Match m : myMatches) {
    UserId other = m.otherUser(myUserId);
    // Do something with match
}
```

---

## Testing the Persistence Layer

### Unit Test (In-Memory Repositories)

```java
// Uses in-memory implementations, no database needed
@Test
void testSwipeLogic() {
    SwipeRepository repo = new InMemorySwipeRepository();
    UserId alice = UserId.generate();
    UserId bob = UserId.generate();

    // Create and save swipe
    Swipe swipe = Swipe.create(alice, bob, LIKE);
    Swipe saved = repo.saveIfNotExists(swipe);

    // Verify it's persisted
    Optional<Swipe> found = repo.findByPair(alice, bob);
    assertTrue(found.isPresent());
    assertEquals(LIKE, found.get().getDirection());
}
```

### Integration Test (Real Database)

```java
// Uses Testcontainers + real PostgreSQL
@SpringBootTest
@Testcontainers
public class SwipeRepositoryIntegrationTest extends IntegrationTestBase {
    @Autowired
    private SwipeRepository swipeRepository;

    @Test
    void testSwipeWithRealDatabase() {
        UserId alice = UserId.generate();
        UserId bob = UserId.generate();

        // Create and save
        Swipe swipe = Swipe.create(alice, bob, LIKE);
        Swipe saved = swipeRepository.saveIfNotExists(swipe);

        // Verify in real database
        Optional<Swipe> found = swipeRepository.findByPair(alice, bob);
        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
    }
}
```

**Run integration tests:**
```bash
# Requires Docker
mvn test -Dtest=*RepositoryIntegrationTest

# Skip if Docker unavailable
mvn test -Dtest=DomainAggregatesTest  # Unit tests only
```

---

## Common Pitfalls

### ❌ DON'T: Inject concrete JPA repositories

```java
// WRONG: Tightly coupled to JPA
@Autowired
private JpaSwipeRepository repo;  // ❌

// RIGHT: Depends on domain interface
@Autowired
private SwipeRepository repo;  // ✅
```

### ❌ DON'T: Create swipes without validation

```java
// WRONG: Bypasses domain rules
Swipe swipe = new Swipe(id, alice, alice, LIKE, now);  // Can't swipe on yourself!

// RIGHT: Use factory method
Swipe swipe = Swipe.create(alice, alice, LIKE);  // ✅ Throws IllegalArgumentException
```

### ❌ DON'T: Modify aggregates after persistence

```java
Swipe swipe = swipeRepository.findByPair(alice, bob).orElseThrow();
swipe.direction = DISLIKE;  // ❌ Swipes are immutable!

// Instead: Create new swipe
Swipe newSwipe = Swipe.create(alice, bob, DISLIKE);
```

### ❌ DON'T: Assume matches are unique by creation order

```java
Match m1 = Match.create(alice, bob);  // Canonical ID: aaa..._bbb...
Match m2 = Match.create(bob, alice);  // Same canonical ID!

// They are the SAME match, not different ones
```

### ✅ DO: Use idempotent operations for retries

```java
// Network fails after save but before response
Swipe swipe = swipeRepository.saveIfNotExists(...);  // ✅ Safe to retry
// Client retries, gets same swipe back, no duplicate
```

---

## Performance Considerations

### Indices (Automatically Created)

```sql
-- Swipe queries optimized by
CREATE INDEX idx_swipes_swiper_id ON swipes(swiper_id);
CREATE INDEX idx_swipes_target_id ON swipes(target_id);

-- Match queries optimized by
CREATE INDEX idx_matches_user_a_id ON matches(user_a_id);
CREATE INDEX idx_matches_user_b_id ON matches(user_b_id);
```

**Impact:**
- `findSwipedUserIds(swiper)` – O(log n) via index
- `findPendingLikersFor(target)` – O(log n) via index
- `findByUser(userId)` – O(log n) via either index

### Batch Operations (Coming in Phase 2)

```java
// Currently: One swipe at a time
for (UserId prospect : prospects) {
    swipeRepository.saveIfNotExists(Swipe.create(me, prospect, LIKE));
}

// Future: Batch insert for better performance
List<Swipe> swipes = prospects.stream()
    .map(p -> Swipe.create(me, p, LIKE))
    .collect(toList());
swipeRepository.saveAllIfNotExist(swipes);  // Coming soon
```

---

## Troubleshooting

### Problem: "No qualifying bean of type 'SwipeRepository'"

**Cause:** Missing `@Repository` annotation on JpaSwipeRepository

**Solution:**
```java
@Repository  // ← Add this
public class JpaSwipeRepository implements SwipeRepository {
    // ...
}
```

### Problem: "Cannot swipe on yourself"

**Cause:** Trying to swipe own profile

**Solution:** Validate at API level before creating swipe
```java
if (myUserId.equals(targetId)) {
    throw new BadRequestException("Cannot swipe on yourself");
}
```

### Problem: Duplicate swipes appearing

**Cause:** Not using `saveIfNotExists()`

**Solution:**
```java
// WRONG: Can create duplicates
swipeRepository.save(swipe);

// RIGHT: Idempotent
swipeRepository.saveIfNotExists(swipe);
```

### Problem: Match with wrong canonical ID

**Cause:** Manually constructing MatchId instead of using factory

**Solution:**
```java
// WRONG: Manual construction loses canonical ordering
new MatchId(myId + "_" + theirId);

// RIGHT: Factory ensures canonical order
MatchId.canonical(userId1, userId2);
```

---

## Migration Guide (If Updating Existing Code)

### Migrating from InMemoryRepositories

**Before:**
```java
// Config was manually wiring in-memory repos
@Bean
public SwipeRepository swipeRepository() {
    return new InMemorySwipeRepository();
}
```

**After:**
```java
// Spring auto-detects @Repository annotated classes
// No config needed - just inject SwipeRepository interface
@Autowired
private SwipeRepository swipeRepository;  // Automatically wires JpaSwipeRepository
```

### Updating AuthService

**Before:**
```java
// ❌ Unsafe type casting
((JpaUserRepository) userRepository).save(user, passwordHash);
```

**After:**
```java
// ✅ Clean abstraction
passwordService.saveUserWithPassword(user, password);
```

---

## References

### Code Examples
- `SwipeRepositoryIntegrationTest.java` – Full CRUD examples
- `MatchRepositoryIntegrationTest.java` – Match operations
- `MatchingFlowIntegrationTest.java` – End-to-end flow

### Architecture Docs
- `IMPLEMENTATION_SUMMARY_2026_01_05.md` – Design patterns explained
- `CLAUDE.md` – Project-wide architecture guide
- `docs/plans/2026-01-05-persistence-layer-foundation.md` – Implementation plan

### Related Classes
- Domain: `Swipe.java`, `Match.java`, `SwipeRepository.java`, `MatchRepository.java`
- Infrastructure: `SwipeEntity.java`, `JpaSwipeRepository.java`, `MatchEntity.java`, `JpaMatchRepository.java`
- Services: `PasswordService.java`, `BcryptPasswordService.java`

---

## Support

For questions about:
- **Architecture decisions** → See `IMPLEMENTATION_SUMMARY_2026_01_05.md`
- **API usage** → See code examples above
- **Database schema** → See `CLAUDE.md` under "Database Schema Changes"
- **Test examples** → See `*IntegrationTest.java` files
