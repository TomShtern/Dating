# Persistence Layer Foundation Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Complete the persistence layer for Swipe and Match aggregates, and fix the AuthService architectural debt by introducing a PasswordService abstraction.

**Architecture:** Hexagonal Architecture (Ports & Adapters). Domain defines repository interfaces (ports), infrastructure provides JPA implementations (adapters). Password handling is extracted into a domain service interface to eliminate unsafe type casting.

**Tech Stack:** Java 21, Spring Boot 4.0.1, Spring Data JPA, Hibernate, PostgreSQL, JUnit 5, Mockito, Testcontainers

---

## Context

**Current State:**
- ✅ UserRepository fully implemented (JPA + in-memory)
- ❌ SwipeRepository only has in-memory implementation
- ❌ MatchRepository only has in-memory implementation
- ❌ AuthService uses unsafe type casting: `((JpaUserRepository) userRepository).save(user, passwordHash)`

**Problem:** Password hash storage breaks the repository abstraction. The domain `User` aggregate shouldn't know about passwords (security concern), but the application layer needs to store them during registration/login.

**Solution:** Introduce `PasswordService` interface in domain, concrete implementation in infrastructure. This maintains clean architecture boundaries.

---

## Task 1: Create PasswordService Abstraction

**Files:**
- Create: `src/main/java/com/datingapp/domain/service/PasswordService.java`
- Create: `src/main/java/com/datingapp/infrastructure/security/BcryptPasswordService.java`
- Modify: `src/main/java/com/datingapp/application/AuthService.java`

### Step 1: Write the PasswordService interface

Create `src/main/java/com/datingapp/domain/service/PasswordService.java`:

```java
package com.datingapp.domain.service;

import com.datingapp.domain.User;

/**
 * Domain service for password operations.
 * Decouples password hashing from repository pattern.
 */
public interface PasswordService {
    /**
     * Hash a raw password.
     */
    String hashPassword(String rawPassword);

    /**
     * Verify a raw password against a stored hash.
     */
    boolean verifyPassword(String rawPassword, String storedHash);

    /**
     * Save user with password hash.
     * This method exists because password is not part of the domain User aggregate.
     */
    void saveUserWithPassword(User user, String rawPassword);

    /**
     * Get password hash for a user (for login verification).
     */
    String getPasswordHash(com.datingapp.domain.UserId userId);
}
```

### Step 2: Implement BcryptPasswordService

Create `src/main/java/com/datingapp/infrastructure/security/BcryptPasswordService.java`:

```java
package com.datingapp.infrastructure.security;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.datingapp.domain.User;
import com.datingapp.domain.UserId;
import com.datingapp.domain.repository.UserRepository;
import com.datingapp.domain.service.PasswordService;
import com.datingapp.infrastructure.persistence.jpa.JpaUserRepository;

@Service
public class BcryptPasswordService implements PasswordService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JpaUserRepository jpaUserRepository;

    public BcryptPasswordService(PasswordEncoder passwordEncoder, UserRepository userRepository, JpaUserRepository jpaUserRepository) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.jpaUserRepository = jpaUserRepository;
    }

    @Override
    public String hashPassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    @Override
    public boolean verifyPassword(String rawPassword, String storedHash) {
        return passwordEncoder.matches(rawPassword, storedHash);
    }

    @Override
    public void saveUserWithPassword(User user, String rawPassword) {
        String passwordHash = hashPassword(rawPassword);
        jpaUserRepository.save(user, passwordHash);
    }

    @Override
    public String getPasswordHash(UserId userId) {
        return jpaUserRepository.getPasswordHash(userId);
    }
}
```

### Step 3: Refactor AuthService to use PasswordService

Modify `src/main/java/com/datingapp/application/AuthService.java`:

**Replace lines 1-72 with:**

```java
package com.datingapp.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.datingapp.api.dto.LoginResponse;
import com.datingapp.domain.User;
import com.datingapp.domain.UserId;
import com.datingapp.domain.repository.UserRepository;
import com.datingapp.domain.service.PasswordService;
import com.datingapp.infrastructure.security.JwtTokenProvider;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository, PasswordService passwordService,
            JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordService = passwordService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public LoginResponse register(String username, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }

        UserId userId = UserId.generate();
        User user = new User(userId, username, null);

        passwordService.saveUserWithPassword(user, password);

        String token = jwtTokenProvider.generateToken(userId, username);
        return new LoginResponse(
                userId.value(),
                username,
                user.getState(),
                token,
                jwtTokenProvider.getExpirationSeconds()
        );
    }

    public LoginResponse login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String storedHash = passwordService.getPasswordHash(user.getId());

        if (!passwordService.verifyPassword(password, storedHash)) {
            throw new IllegalArgumentException("Invalid password");
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername());
        return new LoginResponse(
                user.getId().value(),
                user.getUsername(),
                user.getState(),
                token,
                jwtTokenProvider.getExpirationSeconds()
        );
    }
}
```

### Step 4: Run integration tests to verify

Run:
```bash
mvn test -Dtest=AuthControllerIntegrationTest
```

Expected: All tests PASS

### Step 5: Commit

```bash
git add src/main/java/com/datingapp/domain/service/PasswordService.java
git add src/main/java/com/datingapp/infrastructure/security/BcryptPasswordService.java
git add src/main/java/com/datingapp/application/AuthService.java
git commit -m "refactor: introduce PasswordService to eliminate unsafe casting

- Create PasswordService interface in domain/service
- Implement BcryptPasswordService in infrastructure
- Refactor AuthService to use PasswordService instead of type casting
- Maintains hexagonal architecture boundaries"
```

---

## Task 2: Create Swipe Persistence Layer

**Files:**
- Create: `src/main/java/com/datingapp/infrastructure/persistence/jpa/SwipeEntity.java`
- Create: `src/main/java/com/datingapp/infrastructure/persistence/jpa/SpringDataSwipeRepository.java`
- Create: `src/main/java/com/datingapp/infrastructure/persistence/jpa/JpaSwipeRepository.java`
- Create: `src/test/java/com/datingapp/infrastructure/persistence/jpa/SwipeRepositoryIntegrationTest.java`

### Step 1: Write failing integration test

Create `src/test/java/com/datingapp/infrastructure/persistence/jpa/SwipeRepositoryIntegrationTest.java`:

```java
package com.datingapp.infrastructure.persistence.jpa;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.datingapp.IntegrationTestBase;
import com.datingapp.domain.Swipe;
import com.datingapp.domain.SwipeDirection;
import com.datingapp.domain.UserId;
import com.datingapp.domain.repository.SwipeRepository;

class SwipeRepositoryIntegrationTest extends IntegrationTestBase {

    @Autowired
    private SwipeRepository swipeRepository;

    private UserId user1;
    private UserId user2;

    @BeforeEach
    void setUp() {
        user1 = UserId.generate();
        user2 = UserId.generate();
    }

    @Test
    void saveIfNotExists_shouldSaveNewSwipe() {
        Swipe swipe = Swipe.create(user1, user2, SwipeDirection.LIKE);

        Swipe saved = swipeRepository.saveIfNotExists(swipe);

        assertNotNull(saved);
        assertEquals(swipe.getId(), saved.getId());
        assertEquals(user1, saved.getSwiperId());
        assertEquals(user2, saved.getTargetId());
        assertEquals(SwipeDirection.LIKE, saved.getDirection());
    }

    @Test
    void saveIfNotExists_shouldNotDuplicateSwipe() {
        Swipe swipe1 = Swipe.create(user1, user2, SwipeDirection.LIKE);
        swipeRepository.saveIfNotExists(swipe1);

        Swipe swipe2 = Swipe.create(user1, user2, SwipeDirection.SUPER_LIKE);
        Swipe result = swipeRepository.saveIfNotExists(swipe2);

        // Should return original swipe, not save the new one
        assertEquals(SwipeDirection.LIKE, result.getDirection());
    }

    @Test
    void findByPair_shouldFindExistingSwipe() {
        Swipe swipe = Swipe.create(user1, user2, SwipeDirection.LIKE);
        swipeRepository.saveIfNotExists(swipe);

        Optional<Swipe> found = swipeRepository.findByPair(user1, user2);

        assertTrue(found.isPresent());
        assertEquals(user1, found.get().getSwiperId());
        assertEquals(user2, found.get().getTargetId());
    }

    @Test
    void findByPair_shouldReturnEmptyWhenNotFound() {
        Optional<Swipe> found = swipeRepository.findByPair(user1, user2);
        assertTrue(found.isEmpty());
    }

    @Test
    void findSwipedUserIds_shouldReturnAllTargetsForSwiper() {
        UserId user3 = UserId.generate();
        swipeRepository.saveIfNotExists(Swipe.create(user1, user2, SwipeDirection.LIKE));
        swipeRepository.saveIfNotExists(Swipe.create(user1, user3, SwipeDirection.DISLIKE));

        Set<UserId> swipedIds = swipeRepository.findSwipedUserIds(user1);

        assertEquals(2, swipedIds.size());
        assertTrue(swipedIds.contains(user2));
        assertTrue(swipedIds.contains(user3));
    }

    @Test
    void findPendingLikersFor_shouldReturnUsersWhoLikedTarget() {
        UserId user3 = UserId.generate();
        swipeRepository.saveIfNotExists(Swipe.create(user1, user2, SwipeDirection.LIKE));
        swipeRepository.saveIfNotExists(Swipe.create(user3, user2, SwipeDirection.SUPER_LIKE));
        swipeRepository.saveIfNotExists(Swipe.create(user1, user3, SwipeDirection.DISLIKE));

        Set<UserId> likers = swipeRepository.findPendingLikersFor(user2);

        assertEquals(2, likers.size());
        assertTrue(likers.contains(user1));
        assertTrue(likers.contains(user3));
    }
}
```

### Step 2: Run test to verify it fails

Run:
```bash
mvn test -Dtest=SwipeRepositoryIntegrationTest
```

Expected: FAIL with "No qualifying bean of type 'SwipeRepository'"

### Step 3: Create SwipeEntity

Create `src/main/java/com/datingapp/infrastructure/persistence/jpa/SwipeEntity.java`:

```java
package com.datingapp.infrastructure.persistence.jpa;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.datingapp.domain.SwipeDirection;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "swipes")
public class SwipeEntity {

    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(nullable = false, columnDefinition = "UUID")
    private UUID swiperId;

    @Column(nullable = false, columnDefinition = "UUID")
    private UUID targetId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SwipeDirection direction;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    // Default constructor for JPA
    public SwipeEntity() {}

    public SwipeEntity(UUID id, UUID swiperId, UUID targetId, SwipeDirection direction, Instant createdAt) {
        this.id = id;
        this.swiperId = swiperId;
        this.targetId = targetId;
        this.direction = direction;
        this.createdAt = createdAt;
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getSwiperId() { return swiperId; }
    public void setSwiperId(UUID swiperId) { this.swiperId = swiperId; }

    public UUID getTargetId() { return targetId; }
    public void setTargetId(UUID targetId) { this.targetId = targetId; }

    public SwipeDirection getDirection() { return direction; }
    public void setDirection(SwipeDirection direction) { this.direction = direction; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
```

### Step 4: Create SpringDataSwipeRepository

Create `src/main/java/com/datingapp/infrastructure/persistence/jpa/SpringDataSwipeRepository.java`:

```java
package com.datingapp.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataSwipeRepository extends JpaRepository<SwipeEntity, UUID> {

    Optional<SwipeEntity> findBySwipeIdAndTargetId(UUID swiperId, UUID targetId);

    List<SwipeEntity> findBySwiperId(UUID swiperId);

    @Query("SELECT s FROM SwipeEntity s WHERE s.targetId = :targetId AND (s.direction = 'LIKE' OR s.direction = 'SUPER_LIKE')")
    List<SwipeEntity> findLikersFor(@Param("targetId") UUID targetId);
}
```

### Step 5: Create JpaSwipeRepository

Create `src/main/java/com/datingapp/infrastructure/persistence/jpa/JpaSwipeRepository.java`:

```java
package com.datingapp.infrastructure.persistence.jpa;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.datingapp.domain.Swipe;
import com.datingapp.domain.SwipeDirection;
import com.datingapp.domain.SwipeId;
import com.datingapp.domain.UserId;
import com.datingapp.domain.repository.SwipeRepository;

@Repository
public class JpaSwipeRepository implements SwipeRepository {

    private final SpringDataSwipeRepository springDataRepo;

    public JpaSwipeRepository(SpringDataSwipeRepository springDataRepo) {
        this.springDataRepo = springDataRepo;
    }

    @Override
    public Swipe saveIfNotExists(Swipe swipe) {
        Optional<SwipeEntity> existing = springDataRepo.findBySwipeIdAndTargetId(
                swipe.getSwiperId().value(),
                swipe.getTargetId().value()
        );

        if (existing.isPresent()) {
            return toDomain(existing.get());
        }

        SwipeEntity entity = toEntity(swipe);
        springDataRepo.save(entity);
        return swipe;
    }

    @Override
    public Optional<Swipe> findByPair(UserId swiperId, UserId targetId) {
        return springDataRepo.findBySwipeIdAndTargetId(swiperId.value(), targetId.value())
                .map(this::toDomain);
    }

    @Override
    public Set<UserId> findSwipedUserIds(UserId swiperId) {
        return springDataRepo.findBySwiperId(swiperId.value()).stream()
                .map(entity -> new UserId(entity.getTargetId()))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<UserId> findPendingLikersFor(UserId userId) {
        return springDataRepo.findLikersFor(userId.value()).stream()
                .map(entity -> new UserId(entity.getSwiperId()))
                .collect(Collectors.toSet());
    }

    private SwipeEntity toEntity(Swipe swipe) {
        return new SwipeEntity(
                swipe.getId().value(),
                swipe.getSwiperId().value(),
                swipe.getTargetId().value(),
                swipe.getDirection(),
                swipe.getCreatedAt()
        );
    }

    private Swipe toDomain(SwipeEntity entity) {
        return Swipe.reconstitute(
                new SwipeId(entity.getId()),
                new UserId(entity.getSwiperId()),
                new UserId(entity.getTargetId()),
                entity.getDirection(),
                entity.getCreatedAt()
        );
    }
}
```

### Step 6: Add reconstitute method to Swipe domain

Modify `src/main/java/com/datingapp/domain/Swipe.java`:

Add this method after the `create()` factory method (after line 45):

```java
    /**
     * Reconstitute a swipe from persistence.
     * Used when loading swipes from the database.
     */
    public static Swipe reconstitute(SwipeId id, UserId swiperId, UserId targetId,
            SwipeDirection direction, Instant createdAt) {
        return new Swipe(id, swiperId, targetId, direction, createdAt);
    }
```

### Step 7: Run tests to verify they pass

Run:
```bash
mvn test -Dtest=SwipeRepositoryIntegrationTest
```

Expected: All tests PASS

### Step 8: Commit

```bash
git add src/main/java/com/datingapp/infrastructure/persistence/jpa/SwipeEntity.java
git add src/main/java/com/datingapp/infrastructure/persistence/jpa/SpringDataSwipeRepository.java
git add src/main/java/com/datingapp/infrastructure/persistence/jpa/JpaSwipeRepository.java
git add src/main/java/com/datingapp/domain/Swipe.java
git add src/test/java/com/datingapp/infrastructure/persistence/jpa/SwipeRepositoryIntegrationTest.java
git commit -m "feat: implement Swipe JPA persistence layer

- Create SwipeEntity with JPA annotations
- Create SpringDataSwipeRepository with custom queries
- Implement JpaSwipeRepository adapter
- Add Swipe.reconstitute() for persistence reconstruction
- Add comprehensive integration tests with Testcontainers"
```

---

## Task 3: Create Match Persistence Layer

**Files:**
- Create: `src/main/java/com/datingapp/infrastructure/persistence/jpa/MatchEntity.java`
- Create: `src/main/java/com/datingapp/infrastructure/persistence/jpa/SpringDataMatchRepository.java`
- Create: `src/main/java/com/datingapp/infrastructure/persistence/jpa/JpaMatchRepository.java`
- Create: `src/test/java/com/datingapp/infrastructure/persistence/jpa/MatchRepositoryIntegrationTest.java`

### Step 1: Write failing integration test

Create `src/test/java/com/datingapp/infrastructure/persistence/jpa/MatchRepositoryIntegrationTest.java`:

```java
package com.datingapp.infrastructure.persistence.jpa;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.datingapp.IntegrationTestBase;
import com.datingapp.domain.Match;
import com.datingapp.domain.MatchId;
import com.datingapp.domain.UserId;
import com.datingapp.domain.repository.MatchRepository;

class MatchRepositoryIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MatchRepository matchRepository;

    private UserId user1;
    private UserId user2;
    private UserId user3;

    @BeforeEach
    void setUp() {
        user1 = UserId.generate();
        user2 = UserId.generate();
        user3 = UserId.generate();
    }

    @Test
    void saveIfNotExists_shouldSaveNewMatch() {
        Match match = Match.create(user1, user2);

        Match saved = matchRepository.saveIfNotExists(match);

        assertNotNull(saved);
        assertEquals(match.getId(), saved.getId());
        assertTrue(saved.involves(user1));
        assertTrue(saved.involves(user2));
    }

    @Test
    void saveIfNotExists_shouldNotDuplicateMatch() {
        Match match1 = Match.create(user1, user2);
        matchRepository.saveIfNotExists(match1);

        Match match2 = Match.create(user2, user1); // Reverse order
        Match result = matchRepository.saveIfNotExists(match2);

        // Should return existing match with canonical ID
        assertEquals(match1.getId(), result.getId());
        assertFalse(result.isNewlyCreated());
    }

    @Test
    void findById_shouldFindExistingMatch() {
        Match match = Match.create(user1, user2);
        matchRepository.saveIfNotExists(match);

        Optional<Match> found = matchRepository.findById(match.getId());

        assertTrue(found.isPresent());
        assertEquals(match.getId(), found.get().getId());
    }

    @Test
    void findById_shouldReturnEmptyWhenNotFound() {
        MatchId nonExistentId = MatchId.canonical(user1, user2);
        Optional<Match> found = matchRepository.findById(nonExistentId);
        assertTrue(found.isEmpty());
    }

    @Test
    void findByUser_shouldReturnAllMatchesForUser() {
        matchRepository.saveIfNotExists(Match.create(user1, user2));
        matchRepository.saveIfNotExists(Match.create(user1, user3));
        matchRepository.saveIfNotExists(Match.create(user2, user3)); // user1 not involved

        List<Match> matches = matchRepository.findByUser(user1);

        assertEquals(2, matches.size());
        assertTrue(matches.stream().allMatch(m -> m.involves(user1)));
    }

    @Test
    void findByUser_shouldReturnEmptyListWhenNoMatches() {
        List<Match> matches = matchRepository.findByUser(user1);
        assertTrue(matches.isEmpty());
    }
}
```

### Step 2: Run test to verify it fails

Run:
```bash
mvn test -Dtest=MatchRepositoryIntegrationTest
```

Expected: FAIL with "No qualifying bean of type 'MatchRepository'"

### Step 3: Create MatchEntity

Create `src/main/java/com/datingapp/infrastructure/persistence/jpa/MatchEntity.java`:

```java
package com.datingapp.infrastructure.persistence.jpa;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "matches")
public class MatchEntity {

    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(nullable = false, columnDefinition = "UUID")
    private UUID userAId;

    @Column(nullable = false, columnDefinition = "UUID")
    private UUID userBId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    // Default constructor for JPA
    public MatchEntity() {}

    public MatchEntity(UUID id, UUID userAId, UUID userBId, Instant createdAt) {
        this.id = id;
        this.userAId = userAId;
        this.userBId = userBId;
        this.createdAt = createdAt;
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserAId() { return userAId; }
    public void setUserAId(UUID userAId) { this.userAId = userAId; }

    public UUID getUserBId() { return userBId; }
    public void setUserBId(UUID userBId) { this.userBId = userBId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
```

### Step 4: Create SpringDataMatchRepository

Create `src/main/java/com/datingapp/infrastructure/persistence/jpa/SpringDataMatchRepository.java`:

```java
package com.datingapp.infrastructure.persistence.jpa;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataMatchRepository extends JpaRepository<MatchEntity, UUID> {

    @Query("SELECT m FROM MatchEntity m WHERE m.userAId = :userId OR m.userBId = :userId")
    List<MatchEntity> findByUser(@Param("userId") UUID userId);
}
```

### Step 5: Create JpaMatchRepository

Create `src/main/java/com/datingapp/infrastructure/persistence/jpa/JpaMatchRepository.java`:

```java
package com.datingapp.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.datingapp.domain.Match;
import com.datingapp.domain.MatchId;
import com.datingapp.domain.UserId;
import com.datingapp.domain.repository.MatchRepository;

@Repository
public class JpaMatchRepository implements MatchRepository {

    private final SpringDataMatchRepository springDataRepo;

    public JpaMatchRepository(SpringDataMatchRepository springDataRepo) {
        this.springDataRepo = springDataRepo;
    }

    @Override
    public Match saveIfNotExists(Match match) {
        Optional<MatchEntity> existing = springDataRepo.findById(match.getId().value());

        if (existing.isPresent()) {
            return toDomain(existing.get());
        }

        MatchEntity entity = toEntity(match);
        springDataRepo.save(entity);
        return match;
    }

    @Override
    public Optional<Match> findById(MatchId id) {
        return springDataRepo.findById(id.value())
                .map(this::toDomain);
    }

    @Override
    public List<Match> findByUser(UserId userId) {
        return springDataRepo.findByUser(userId.value()).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private MatchEntity toEntity(Match match) {
        return new MatchEntity(
                match.getId().value(),
                match.getUserA().value(),
                match.getUserB().value(),
                match.getCreatedAt()
        );
    }

    private Match toDomain(MatchEntity entity) {
        return Match.reconstitute(
                new MatchId(entity.getId()),
                new UserId(entity.getUserAId()),
                new UserId(entity.getUserBId()),
                entity.getCreatedAt()
        );
    }
}
```

### Step 6: Run tests to verify they pass

Run:
```bash
mvn test -Dtest=MatchRepositoryIntegrationTest
```

Expected: All tests PASS

### Step 7: Commit

```bash
git add src/main/java/com/datingapp/infrastructure/persistence/jpa/MatchEntity.java
git add src/main/java/com/datingapp/infrastructure/persistence/jpa/SpringDataMatchRepository.java
git add src/main/java/com/datingapp/infrastructure/persistence/jpa/JpaMatchRepository.java
git add src/test/java/com/datingapp/infrastructure/persistence/jpa/MatchRepositoryIntegrationTest.java
git commit -m "feat: implement Match JPA persistence layer

- Create MatchEntity with canonical ID storage
- Create SpringDataMatchRepository with user lookup query
- Implement JpaMatchRepository adapter
- Add comprehensive integration tests with Testcontainers
- Uses existing Match.reconstitute() from domain"
```

---

## Task 4: End-to-End Integration Test

**Files:**
- Create: `src/test/java/com/datingapp/infrastructure/persistence/jpa/MatchingFlowIntegrationTest.java`

### Step 1: Write end-to-end matching flow test

Create `src/test/java/com/datingapp/infrastructure/persistence/jpa/MatchingFlowIntegrationTest.java`:

```java
package com.datingapp.infrastructure.persistence.jpa;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.datingapp.IntegrationTestBase;
import com.datingapp.domain.Distance;
import com.datingapp.domain.Interest;
import com.datingapp.domain.Location;
import com.datingapp.domain.Match;
import com.datingapp.domain.Preferences;
import com.datingapp.domain.Profile;
import com.datingapp.domain.Prospect;
import com.datingapp.domain.Swipe;
import com.datingapp.domain.SwipeDirection;
import com.datingapp.domain.User;
import com.datingapp.domain.UserId;
import com.datingapp.domain.matching.DistanceStrategy;
import com.datingapp.domain.matching.MatchScorer;
import com.datingapp.domain.matching.MatchingService;
import com.datingapp.domain.repository.MatchRepository;
import com.datingapp.domain.repository.SwipeRepository;
import com.datingapp.domain.repository.UserRepository;

/**
 * End-to-end integration test for the complete matching flow.
 * Tests: User creation → Discovery → Swipe → Match creation with real PostgreSQL.
 */
class MatchingFlowIntegrationTest extends IntegrationTestBase {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SwipeRepository swipeRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Test
    void completeMatchingFlow_shouldCreateMatchOnMutualLike() {
        // Given: Two users with complete profiles in NYC
        User alice = createAndSaveUser("alice", 40.7128, -74.0060, Set.of(Interest.HIKING));
        User bob = createAndSaveUser("bob", 40.7306, -73.9352, Set.of(Interest.HIKING, Interest.MUSIC));

        // When: Alice discovers Bob
        MatchingService matchingService = createMatchingService();
        List<Prospect> aliceProspects = matchingService.findProspects(
                alice,
                Distance.ofKilometers(50),
                10,
                Set.of()
        );

        // Then: Bob should appear in Alice's prospects
        assertEquals(1, aliceProspects.size());
        assertEquals(bob.getId(), aliceProspects.get(0).userId());
        assertEquals(1, aliceProspects.get(0).sharedInterests().size());

        // When: Alice likes Bob
        Optional<Match> matchAfterAliceLikes = matchingService.processSwipe(
                alice.getId(),
                bob.getId(),
                SwipeDirection.LIKE
        );

        // Then: No match yet (waiting for mutual interest)
        assertTrue(matchAfterAliceLikes.isEmpty());

        // When: Bob likes Alice back
        Optional<Match> matchAfterBobLikes = matchingService.processSwipe(
                bob.getId(),
                alice.getId(),
                SwipeDirection.LIKE
        );

        // Then: Match is created
        assertTrue(matchAfterBobLikes.isPresent());
        Match match = matchAfterBobLikes.get();
        assertTrue(match.involves(alice.getId()));
        assertTrue(match.involves(bob.getId()));

        // Verify persistence: Match should be retrievable
        Optional<Match> persistedMatch = matchRepository.findById(match.getId());
        assertTrue(persistedMatch.isPresent());

        // Verify: Alice can see the match in her match list
        List<Match> aliceMatches = matchRepository.findByUser(alice.getId());
        assertEquals(1, aliceMatches.size());
        assertEquals(match.getId(), aliceMatches.get(0).getId());

        // Verify: Bob can see the match in his match list
        List<Match> bobMatches = matchRepository.findByUser(bob.getId());
        assertEquals(1, bobMatches.size());
        assertEquals(match.getId(), bobMatches.get(0).getId());
    }

    @Test
    void swipeFlow_shouldNotCreateMatchOnDislike() {
        User alice = createAndSaveUser("alice", 40.7128, -74.0060, Set.of(Interest.GAMING));
        User bob = createAndSaveUser("bob", 40.7306, -73.9352, Set.of(Interest.GAMING));

        MatchingService matchingService = createMatchingService();

        // Alice likes Bob
        matchingService.processSwipe(alice.getId(), bob.getId(), SwipeDirection.LIKE);

        // Bob dislikes Alice
        Optional<Match> result = matchingService.processSwipe(bob.getId(), alice.getId(), SwipeDirection.DISLIKE);

        // No match created
        assertTrue(result.isEmpty());
        assertTrue(matchRepository.findByUser(alice.getId()).isEmpty());
    }

    @Test
    void swipeFlow_shouldPreventDuplicateMatches() {
        User alice = createAndSaveUser("alice", 40.7128, -74.0060, Set.of(Interest.TRAVEL));
        User bob = createAndSaveUser("bob", 40.7306, -73.9352, Set.of(Interest.TRAVEL));

        MatchingService matchingService = createMatchingService();

        // Both users like each other
        matchingService.processSwipe(alice.getId(), bob.getId(), SwipeDirection.LIKE);
        Optional<Match> firstMatch = matchingService.processSwipe(bob.getId(), alice.getId(), SwipeDirection.LIKE);

        assertTrue(firstMatch.isPresent());

        // Try to swipe again (shouldn't create duplicate)
        Optional<Match> secondMatch = matchingService.processSwipe(bob.getId(), alice.getId(), SwipeDirection.SUPER_LIKE);

        assertTrue(secondMatch.isPresent());
        assertEquals(firstMatch.get().getId(), secondMatch.get().getId());

        // Verify only one match exists
        List<Match> aliceMatches = matchRepository.findByUser(alice.getId());
        assertEquals(1, aliceMatches.size());
    }

    // Helper methods

    private User createAndSaveUser(String username, double lat, double lon, Set<Interest> interests) {
        UserId userId = UserId.generate();
        Location location = new Location(lat, lon);
        Preferences preferences = new Preferences(Set.of("ALL"), null, Distance.ofKilometers(100));

        Profile profile = new Profile(
                userId,
                username,
                "Bio for " + username,
                LocalDate.of(1990, 1, 1),
                interests,
                preferences,
                location,
                List.of("photo1.jpg")
        );

        User user = new User(userId, username, profile);
        userRepository.save(user);
        return user;
    }

    private MatchingService createMatchingService() {
        MatchScorer scorer = new MatchScorer(List.of(
                new DistanceStrategy(Distance.ofKilometers(100))
        ));

        return new MatchingService(
                scorer,
                userRepository,
                swipeRepository,
                matchRepository,
                event -> {} // No-op event publisher for tests
        );
    }
}
```

### Step 2: Run the end-to-end test

Run:
```bash
mvn test -Dtest=MatchingFlowIntegrationTest
```

Expected: All tests PASS

### Step 3: Commit

```bash
git add src/test/java/com/datingapp/infrastructure/persistence/jpa/MatchingFlowIntegrationTest.java
git commit -m "test: add end-to-end matching flow integration test

- Tests complete flow: User → Discovery → Swipe → Match
- Verifies mutual like creates match
- Verifies dislike prevents match
- Verifies idempotency (no duplicate matches)
- Uses real PostgreSQL via Testcontainers"
```

---

## Task 5: Run Full Test Suite

### Step 1: Run all tests

Run:
```bash
mvn clean test
```

Expected: All tests PASS (domain tests + integration tests)

### Step 2: Verify test coverage

Run:
```bash
mvn test jacoco:report
```

Check: `target/site/jacoco/index.html` for coverage report

Expected: High coverage on domain and infrastructure layers

### Step 3: Final commit

```bash
git add -A
git commit -m "chore: complete persistence layer foundation

Summary of changes:
- Introduced PasswordService abstraction (eliminates unsafe casting)
- Implemented JpaSwipeRepository with full CRUD operations
- Implemented JpaMatchRepository with canonical ID handling
- Added comprehensive integration tests for all repositories
- Added end-to-end matching flow test

All tests passing with real PostgreSQL via Testcontainers.
Architecture boundaries preserved (hexagonal pattern intact)."
```

---

## Success Criteria

✅ **PasswordService abstraction eliminates type casting**
✅ **JpaSwipeRepository fully functional with integration tests**
✅ **JpaMatchRepository fully functional with integration tests**
✅ **End-to-end matching flow works with real database**
✅ **All tests pass (unit + integration)**
✅ **Hexagonal architecture boundaries maintained**
✅ **Code follows existing patterns (constructor injection, records, etc.)**

---

## Next Steps

After this plan is complete, you'll be ready to implement **Priority 3: Expose the Matching API** which will include:
- `MatchService` (application layer orchestration)
- `MatchController` (REST API endpoints)
- `GET /api/prospects`, `POST /api/swipes`, `GET /api/matches`

This plan establishes the foundation for all future features.
