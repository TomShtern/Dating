# Phase 2: Matching API Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build three REST endpoints (`GET /api/prospects`, `POST /api/swipes`, `GET /api/matches`) to complete the matching flow.

**Architecture:** Controllers handle HTTP → Services contain business logic → Existing repositories persist data. No new database tables needed.

**Tech Stack:** Spring Boot 4.0.1, Spring Data JPA, existing SwipeRepository and MatchRepository, JUnit + Testcontainers

---

### Task 1: ProspectDTO and ProspectsService

**Files:**
- Create: `src/main/java/com/datingapp/api/dto/ProspectDto.java`
- Create: `src/main/java/com/datingapp/application/ProspectsService.java`
- Create: `src/test/java/com/datingapp/application/ProspectsServiceTest.java`

**Step 1: Write the failing test**

```java
// File: src/test/java/com/datingapp/application/ProspectsServiceTest.java
package com.datingapp.application;

import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import com.datingapp.domain.Distance;
import com.datingapp.domain.Interest;
import com.datingapp.domain.Location;
import com.datingapp.domain.Preferences;
import com.datingapp.domain.Profile;
import com.datingapp.domain.User;
import com.datingapp.domain.UserId;
import com.datingapp.domain.matching.MatchScorer;
import com.datingapp.domain.repository.SwipeRepository;
import com.datingapp.domain.repository.UserRepository;
import com.datingapp.infrastructure.persistence.inmemory.InMemorySwipeRepository;
import com.datingapp.infrastructure.persistence.inmemory.InMemoryUserRepository;
import com.datingapp.api.dto.ProspectDto;

class ProspectsServiceTest {

    @Test
    void findProspects_shouldReturnUsersWithinDistance() {
        // Given
        UserRepository userRepository = new InMemoryUserRepository();
        SwipeRepository swipeRepository = new InMemorySwipeRepository();
        ProspectsService service = new ProspectsService(userRepository, swipeRepository);

        UserId alice = UserId.generate();
        User aliceUser = createUser(alice, "alice", 40.7128, -74.0060, Set.of(Interest.HIKING));
        userRepository.save(aliceUser);

        UserId bob = UserId.generate();
        User bobUser = createUser(bob, "bob", 40.7306, -73.9352, Set.of(Interest.HIKING));
        userRepository.save(bobUser);

        // When
        List<ProspectDto> prospects = service.findProspects(
                alice,
                Distance.ofKilometers(100),
                10,
                Set.of()
        );

        // Then
        assertEquals(1, prospects.size());
        assertEquals(bob, prospects.get(0).userId());
        assertEquals("bob", prospects.get(0).displayName());
        assertEquals(1, prospects.get(0).sharedInterestCount());
    }

    @Test
    void findProspects_shouldExcludeAlreadySwipedUsers() {
        // Given
        UserRepository userRepository = new InMemoryUserRepository();
        SwipeRepository swipeRepository = new InMemorySwipeRepository();
        ProspectsService service = new ProspectsService(userRepository, swipeRepository);

        UserId alice = UserId.generate();
        User aliceUser = createUser(alice, "alice", 40.7128, -74.0060, Set.of(Interest.HIKING));
        userRepository.save(aliceUser);

        UserId bob = UserId.generate();
        User bobUser = createUser(bob, "bob", 40.7306, -73.9352, Set.of(Interest.HIKING));
        userRepository.save(bobUser);

        // Alice already swiped on Bob
        swipeRepository.saveIfNotExists(
                com.datingapp.domain.Swipe.create(alice, bob, com.datingapp.domain.SwipeDirection.LIKE)
        );

        // When
        List<ProspectDto> prospects = service.findProspects(
                alice,
                Distance.ofKilometers(100),
                10,
                Set.of()
        );

        // Then
        assertEquals(0, prospects.size()); // Bob excluded because already swiped
    }

    private User createUser(UserId id, String username, double lat, double lon, Set<Interest> interests) {
        Location location = new Location(lat, lon);
        Preferences preferences = new Preferences(Set.of("ALL"), null, Distance.ofKilometers(100));
        Profile profile = new Profile(
                id,
                username,
                "Bio for " + username,
                LocalDate.of(1990, 1, 1),
                interests,
                preferences,
                location,
                List.of()
        );
        return new User(id, username, profile);
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=ProspectsServiceTest -v`

Expected: FAIL with "class not found: ProspectsService"

**Step 3: Write the DTO and Service**

```java
// File: src/main/java/com/datingapp/api/dto/ProspectDto.java
package com.datingapp.api.dto;

import com.datingapp.domain.UserId;

public record ProspectDto(
    UserId userId,
    String displayName,
    int age,
    double distanceKm,
    int sharedInterestCount
) {}
```

```java
// File: src/main/java/com/datingapp/application/ProspectsService.java
package com.datingapp.application;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.datingapp.api.dto.ProspectDto;
import com.datingapp.domain.Distance;
import com.datingapp.domain.Interest;
import com.datingapp.domain.Location;
import com.datingapp.domain.User;
import com.datingapp.domain.UserId;
import com.datingapp.domain.repository.SwipeRepository;
import com.datingapp.domain.repository.UserRepository;

@Service
public class ProspectsService {
    private final UserRepository userRepository;
    private final SwipeRepository swipeRepository;

    public ProspectsService(UserRepository userRepository, SwipeRepository swipeRepository) {
        this.userRepository = userRepository;
        this.swipeRepository = swipeRepository;
    }

    public List<ProspectDto> findProspects(UserId userId, Distance maxDistance, int limit, Set<Interest> excludedInterests) {
        User currentUser = userRepository.findById(userId).orElseThrow();
        Location myLocation = currentUser.getProfile().location();
        Set<Interest> myInterests = currentUser.getProfile().interests();

        Set<UserId> alreadySwipedIds = swipeRepository.findSwipedUserIds(userId);

        return userRepository.getAll().stream()
                .filter(u -> !u.getId().equals(userId)) // Exclude self
                .filter(u -> !alreadySwipedIds.contains(u.getId())) // Exclude already swiped
                .filter(u -> u.getProfile().location().distanceTo(myLocation).isLessThanOrEqual(maxDistance))
                .sorted((a, b) -> Double.compare(
                    a.getProfile().location().distanceTo(myLocation).kilometers(),
                    b.getProfile().location().distanceTo(myLocation).kilometers()
                ))
                .limit(limit)
                .map(u -> new ProspectDto(
                    u.getId(),
                    u.getProfile().displayName(),
                    calculateAge(u.getProfile().birthDate()),
                    u.getProfile().location().distanceTo(myLocation).kilometers(),
                    countSharedInterests(myInterests, u.getProfile().interests())
                ))
                .collect(Collectors.toList());
    }

    private int calculateAge(LocalDate birthDate) {
        return LocalDate.now().getYear() - birthDate.getYear();
    }

    private int countSharedInterests(Set<Interest> myInterests, Set<Interest> theirInterests) {
        return (int) myInterests.stream()
                .filter(theirInterests::contains)
                .count();
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=ProspectsServiceTest -v`

Expected: PASS (both test methods)

**Step 5: Commit**

```bash
git add src/main/java/com/datingapp/api/dto/ProspectDto.java
git add src/main/java/com/datingapp/application/ProspectsService.java
git add src/test/java/com/datingapp/application/ProspectsServiceTest.java
git commit -m "feat: add ProspectsService to discover prospects excluding already-swiped users"
```

---

### Task 2: ProspectsController with Integration Test

**Files:**
- Create: `src/main/java/com/datingapp/api/ProspectsController.java`
- Create: `src/main/java/com/datingapp/api/dto/ProspectsResponseDto.java`
- Create: `src/test/java/com/datingapp/api/ProspectsControllerIntegrationTest.java`

**Step 1: Write the failing integration test**

```java
// File: src/test/java/com/datingapp/api/ProspectsControllerIntegrationTest.java
package com.datingapp.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.datingapp.IntegrationTestBase;
import com.datingapp.domain.Distance;
import com.datingapp.domain.Interest;
import com.datingapp.domain.Location;
import com.datingapp.domain.Preferences;
import com.datingapp.domain.Profile;
import com.datingapp.domain.User;
import com.datingapp.domain.UserId;
import com.datingapp.infrastructure.security.JwtTokenProvider;
import com.datingapp.domain.repository.UserRepository;

@SpringBootTest
class ProspectsControllerIntegrationTest extends IntegrationTestBase {
    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private MockMvc mockMvc;

    @Override
    public void setUp() {
        super.setUp();
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void getProspects_shouldReturnNearbyUsersExcludingAlreadySwiped() throws Exception {
        // Given: Alice and Bob in NYC
        UserId alice = UserId.generate();
        User aliceUser = createUser(alice, "alice", 40.7128, -74.0060, Set.of(Interest.HIKING));
        userRepository.save(aliceUser);

        UserId bob = UserId.generate();
        User bobUser = createUser(bob, "bob", 40.7306, -73.9352, Set.of(Interest.HIKING));
        userRepository.save(bobUser);

        String token = jwtTokenProvider.generateToken(alice);

        // When
        mockMvc.perform(get("/api/prospects")
                .header("Authorization", "Bearer " + token)
                .param("limit", "10")
                .param("maxDistanceKm", "100"))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prospects[0].userId").exists())
                .andExpect(jsonPath("$.prospects[0].displayName").value("bob"))
                .andExpect(jsonPath("$.prospects[0].sharedInterestCount").value(1));
    }

    @Test
    void getProspects_shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/prospects")
                .param("limit", "10")
                .param("maxDistanceKm", "100"))
                .andExpect(status().isUnauthorized());
    }

    private User createUser(UserId id, String username, double lat, double lon, Set<Interest> interests) {
        Location location = new Location(lat, lon);
        Preferences preferences = new Preferences(Set.of("ALL"), null, Distance.ofKilometers(100));
        Profile profile = new Profile(
                id,
                username,
                "Bio for " + username,
                LocalDate.of(1990, 1, 1),
                interests,
                preferences,
                location,
                java.util.List.of()
        );
        return new User(id, username, profile);
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=ProspectsControllerIntegrationTest -v`

Expected: FAIL with 404 (endpoint not found)

**Step 3: Write ProspectsController and DTO**

```java
// File: src/main/java/com/datingapp/api/dto/ProspectsResponseDto.java
package com.datingapp.api.dto;

import java.util.List;

public record ProspectsResponseDto(
    List<ProspectDto> prospects,
    int total
) {}
```

```java
// File: src/main/java/com/datingapp/api/ProspectsController.java
package com.datingapp.api;

import java.util.List;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.datingapp.api.dto.ProspectDto;
import com.datingapp.api.dto.ProspectsResponseDto;
import com.datingapp.application.ProspectsService;
import com.datingapp.domain.Distance;
import com.datingapp.domain.Interest;
import com.datingapp.infrastructure.security.JwtTokenProvider;
import com.datingapp.domain.UserId;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/prospects")
public class ProspectsController {
    private final ProspectsService prospectsService;
    private final JwtTokenProvider jwtTokenProvider;

    public ProspectsController(ProspectsService prospectsService, JwtTokenProvider jwtTokenProvider) {
        this.prospectsService = prospectsService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @GetMapping
    public ResponseEntity<ProspectsResponseDto> getProspects(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "100") double maxDistanceKm,
            HttpServletRequest request) {

        String token = extractToken(request);
        UserId userId = jwtTokenProvider.extractUserId(token);

        List<ProspectDto> prospects = prospectsService.findProspects(
                userId,
                Distance.ofKilometers(maxDistanceKm),
                limit,
                Set.of()
        );

        return ResponseEntity.ok(new ProspectsResponseDto(prospects, prospects.size()));
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        throw new IllegalArgumentException("Missing or invalid Authorization header");
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=ProspectsControllerIntegrationTest -v`

Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/com/datingapp/api/ProspectsController.java
git add src/main/java/com/datingapp/api/dto/ProspectsResponseDto.java
git add src/test/java/com/datingapp/api/ProspectsControllerIntegrationTest.java
git commit -m "feat: add GET /api/prospects endpoint for discovering matches"
```

---

### Task 3: SwipeDTO and SwipesController

**Files:**
- Create: `src/main/java/com/datingapp/api/dto/SwipeRequestDto.java`
- Create: `src/main/java/com/datingapp/api/dto/MatchDataDto.java`
- Create: `src/main/java/com/datingapp/api/dto/SwipeResponseDto.java`
- Create: `src/main/java/com/datingapp/api/SwipesController.java`
- Create: `src/test/java/com/datingapp/api/SwipesControllerIntegrationTest.java`

**Step 1: Write the failing integration test**

```java
// File: src/test/java/com/datingapp/api/SwipesControllerIntegrationTest.java
package com.datingapp.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.datingapp.IntegrationTestBase;
import com.datingapp.domain.Distance;
import com.datingapp.domain.Interest;
import com.datingapp.domain.Location;
import com.datingapp.domain.Preferences;
import com.datingapp.domain.Profile;
import com.datingapp.domain.SwipeDirection;
import com.datingapp.domain.User;
import com.datingapp.domain.UserId;
import com.datingapp.infrastructure.security.JwtTokenProvider;
import com.datingapp.domain.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
class SwipesControllerIntegrationTest extends IntegrationTestBase {
    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private MockMvc mockMvc;

    @Override
    public void setUp() {
        super.setUp();
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void postSwipe_shouldRecordLikeWithoutMatch() throws Exception {
        // Given
        UserId alice = UserId.generate();
        User aliceUser = createUser(alice, "alice", 40.7128, -74.0060, Set.of(Interest.HIKING));
        userRepository.save(aliceUser);

        UserId bob = UserId.generate();
        User bobUser = createUser(bob, "bob", 40.7306, -73.9352, Set.of(Interest.HIKING));
        userRepository.save(bobUser);

        String token = jwtTokenProvider.generateToken(alice);
        String requestBody = new ObjectMapper().writeValueAsString(
            new com.datingapp.api.dto.SwipeRequestDto(bob, SwipeDirection.LIKE)
        );

        // When
        mockMvc.perform(post("/api/swipes")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content(requestBody))
                // Then
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.match").doesNotExist()); // No match yet
    }

    @Test
    void postSwipe_shouldCreateMatchOnMutualLike() throws Exception {
        // Given
        UserId alice = UserId.generate();
        User aliceUser = createUser(alice, "alice", 40.7128, -74.0060, Set.of(Interest.HIKING));
        userRepository.save(aliceUser);

        UserId bob = UserId.generate();
        User bobUser = createUser(bob, "bob", 40.7306, -73.9352, Set.of(Interest.HIKING));
        userRepository.save(bobUser);

        // Alice likes Bob
        String aliceToken = jwtTokenProvider.generateToken(alice);
        String requestBody = new ObjectMapper().writeValueAsString(
            new com.datingapp.api.dto.SwipeRequestDto(bob, SwipeDirection.LIKE)
        );
        mockMvc.perform(post("/api/swipes")
                .header("Authorization", "Bearer " + aliceToken)
                .contentType("application/json")
                .content(requestBody))
                .andExpect(status().isCreated());

        // When: Bob likes Alice back
        String bobToken = jwtTokenProvider.generateToken(bob);
        String bobRequestBody = new ObjectMapper().writeValueAsString(
            new com.datingapp.api.dto.SwipeRequestDto(alice, SwipeDirection.LIKE)
        );

        // Then: Match should be created
        mockMvc.perform(post("/api/swipes")
                .header("Authorization", "Bearer " + bobToken)
                .contentType("application/json")
                .content(bobRequestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.match").exists())
                .andExpect(jsonPath("$.match.matchedWithUserId").exists());
    }

    @Test
    void postSwipe_shouldReturn400WhenSwipingSelf() throws Exception {
        // Given
        UserId alice = UserId.generate();
        User aliceUser = createUser(alice, "alice", 40.7128, -74.0060, Set.of(Interest.HIKING));
        userRepository.save(aliceUser);

        String token = jwtTokenProvider.generateToken(alice);
        String requestBody = new ObjectMapper().writeValueAsString(
            new com.datingapp.api.dto.SwipeRequestDto(alice, SwipeDirection.LIKE)
        );

        // When
        mockMvc.perform(post("/api/swipes")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content(requestBody))
                // Then
                .andExpect(status().isBadRequest());
    }

    private User createUser(UserId id, String username, double lat, double lon, Set<Interest> interests) {
        Location location = new Location(lat, lon);
        Preferences preferences = new Preferences(Set.of("ALL"), null, Distance.ofKilometers(100));
        Profile profile = new Profile(
                id,
                username,
                "Bio for " + username,
                LocalDate.of(1990, 1, 1),
                interests,
                preferences,
                location,
                java.util.List.of()
        );
        return new User(id, username, profile);
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=SwipesControllerIntegrationTest -v`

Expected: FAIL with 404 (endpoint not found)

**Step 3: Write SwipesController and DTOs**

```java
// File: src/main/java/com/datingapp/api/dto/SwipeRequestDto.java
package com.datingapp.api.dto;

import com.datingapp.domain.SwipeDirection;
import com.datingapp.domain.UserId;

public record SwipeRequestDto(
    UserId targetUserId,
    SwipeDirection direction
) {}
```

```java
// File: src/main/java/com/datingapp/api/dto/MatchDataDto.java
package com.datingapp.api.dto;

import com.datingapp.domain.UserId;

public record MatchDataDto(
    String matchId,
    UserId matchedWithUserId,
    String matchedWithDisplayName
) {}
```

```java
// File: src/main/java/com/datingapp/api/dto/SwipeResponseDto.java
package com.datingapp.api.dto;

public record SwipeResponseDto(
    String swipeId,
    MatchDataDto match
) {}
```

```java
// File: src/main/java/com/datingapp/api/SwipesController.java
package com.datingapp.api;

import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.datingapp.api.dto.MatchDataDto;
import com.datingapp.api.dto.SwipeRequestDto;
import com.datingapp.api.dto.SwipeResponseDto;
import com.datingapp.domain.Match;
import com.datingapp.domain.User;
import com.datingapp.domain.UserId;
import com.datingapp.domain.matching.MatchingService;
import com.datingapp.infrastructure.security.JwtTokenProvider;
import com.datingapp.domain.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/swipes")
public class SwipesController {
    private final MatchingService matchingService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    public SwipesController(MatchingService matchingService, JwtTokenProvider jwtTokenProvider, UserRepository userRepository) {
        this.matchingService = matchingService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<SwipeResponseDto> postSwipe(
            @RequestBody SwipeRequestDto request,
            HttpServletRequest httpRequest) {

        String token = extractToken(httpRequest);
        UserId swiperId = jwtTokenProvider.extractUserId(token);

        // Validate: can't swipe on yourself
        if (swiperId.equals(request.targetUserId())) {
            return ResponseEntity.badRequest().build();
        }

        // Process swipe (uses MatchingService which calls repositories)
        Optional<Match> match = matchingService.processSwipe(swiperId, request.targetUserId(), request.direction());

        // Build response
        MatchDataDto matchData = null;
        if (match.isPresent()) {
            User otherUser = userRepository.findById(request.targetUserId()).orElseThrow();
            matchData = new MatchDataDto(
                match.get().getId().value(),
                request.targetUserId(),
                otherUser.getProfile().displayName()
            );
        }

        SwipeResponseDto response = new SwipeResponseDto(
            null, // swipeId not critical for now
            matchData
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        throw new IllegalArgumentException("Missing or invalid Authorization header");
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=SwipesControllerIntegrationTest -v`

Expected: PASS (all 3 test methods)

**Step 5: Commit**

```bash
git add src/main/java/com/datingapp/api/dto/SwipeRequestDto.java
git add src/main/java/com/datingapp/api/dto/MatchDataDto.java
git add src/main/java/com/datingapp/api/dto/SwipeResponseDto.java
git add src/main/java/com/datingapp/api/SwipesController.java
git add src/test/java/com/datingapp/api/SwipesControllerIntegrationTest.java
git commit -m "feat: add POST /api/swipes endpoint for recording swipes and creating matches"
```

---

### Task 4: MatchesController

**Files:**
- Create: `src/main/java/com/datingapp/api/dto/MatchDto.java`
- Create: `src/main/java/com/datingapp/api/MatchesController.java`
- Create: `src/test/java/com/datingapp/api/MatchesControllerIntegrationTest.java`

**Step 1: Write the failing integration test**

```java
// File: src/test/java/com/datingapp/api/MatchesControllerIntegrationTest.java
package com.datingapp.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.datingapp.IntegrationTestBase;
import com.datingapp.domain.Distance;
import com.datingapp.domain.Interest;
import com.datingapp.domain.Location;
import com.datingapp.domain.Preferences;
import com.datingapp.domain.Profile;
import com.datingapp.domain.SwipeDirection;
import com.datingapp.domain.User;
import com.datingapp.domain.UserId;
import com.datingapp.domain.Swipe;
import com.datingapp.domain.Match;
import com.datingapp.infrastructure.security.JwtTokenProvider;
import com.datingapp.domain.repository.UserRepository;
import com.datingapp.domain.repository.SwipeRepository;
import com.datingapp.domain.repository.MatchRepository;

@SpringBootTest
class MatchesControllerIntegrationTest extends IntegrationTestBase {
    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SwipeRepository swipeRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private MockMvc mockMvc;

    @Override
    public void setUp() {
        super.setUp();
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void getMatches_shouldReturnUserMatches() throws Exception {
        // Given: Alice and Bob matched
        UserId alice = UserId.generate();
        User aliceUser = createUser(alice, "alice", 40.7128, -74.0060, Set.of(Interest.HIKING));
        userRepository.save(aliceUser);

        UserId bob = UserId.generate();
        User bobUser = createUser(bob, "bob", 40.7306, -73.9352, Set.of(Interest.HIKING));
        userRepository.save(bobUser);

        // Create mutual likes → creates match
        Swipe aliceLikes = Swipe.create(alice, bob, SwipeDirection.LIKE);
        swipeRepository.saveIfNotExists(aliceLikes);

        Swipe bobLikes = Swipe.create(bob, alice, SwipeDirection.LIKE);
        swipeRepository.saveIfNotExists(bobLikes);

        Match match = Match.create(alice, bob);
        matchRepository.saveIfNotExists(match);

        String token = jwtTokenProvider.generateToken(alice);

        // When
        mockMvc.perform(get("/api/matches")
                .header("Authorization", "Bearer " + token))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matches[0].matchedWithUserId").exists())
                .andExpect(jsonPath("$.matches[0].displayName").value("bob"));
    }

    @Test
    void getMatches_shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/matches"))
                .andExpect(status().isUnauthorized());
    }

    private User createUser(UserId id, String username, double lat, double lon, Set<Interest> interests) {
        Location location = new Location(lat, lon);
        Preferences preferences = new Preferences(Set.of("ALL"), null, Distance.ofKilometers(100));
        Profile profile = new Profile(
                id,
                username,
                "Bio for " + username,
                LocalDate.of(1990, 1, 1),
                interests,
                preferences,
                location,
                java.util.List.of()
        );
        return new User(id, username, profile);
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=MatchesControllerIntegrationTest -v`

Expected: FAIL with 404 (endpoint not found)

**Step 3: Write MatchesController and DTO**

```java
// File: src/main/java/com/datingapp/api/dto/MatchDto.java
package com.datingapp.api.dto;

import com.datingapp.domain.UserId;

public record MatchDto(
    String matchId,
    UserId matchedWithUserId,
    String displayName
) {}
```

```java
// File: src/main/java/com/datingapp/api/MatchesController.java
package com.datingapp.api;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.datingapp.api.dto.MatchDto;
import com.datingapp.domain.Match;
import com.datingapp.domain.User;
import com.datingapp.domain.UserId;
import com.datingapp.infrastructure.security.JwtTokenProvider;
import com.datingapp.domain.repository.MatchRepository;
import com.datingapp.domain.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/matches")
public class MatchesController {
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public MatchesController(MatchRepository matchRepository, UserRepository userRepository, JwtTokenProvider jwtTokenProvider) {
        this.matchRepository = matchRepository;
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @GetMapping
    public ResponseEntity<?> getMatches(HttpServletRequest request) {
        String token = extractToken(request);
        UserId userId = jwtTokenProvider.extractUserId(token);

        List<Match> matches = matchRepository.findByUser(userId);

        List<MatchDto> matchDtos = matches.stream()
                .map(match -> {
                    UserId otherUserId = match.otherUser(userId);
                    User otherUser = userRepository.findById(otherUserId).orElseThrow();
                    return new MatchDto(
                        match.getId().value(),
                        otherUserId,
                        otherUser.getProfile().displayName()
                    );
                })
                .toList();

        return ResponseEntity.ok(new java.util.HashMap<String, Object>() {{
            put("matches", matchDtos);
        }});
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        throw new IllegalArgumentException("Missing or invalid Authorization header");
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=MatchesControllerIntegrationTest -v`

Expected: PASS (both test methods)

**Step 5: Commit**

```bash
git add src/main/java/com/datingapp/api/dto/MatchDto.java
git add src/main/java/com/datingapp/api/MatchesController.java
git add src/test/java/com/datingapp/api/MatchesControllerIntegrationTest.java
git commit -m "feat: add GET /api/matches endpoint to list user matches"
```

---

### Task 5: Full Integration Test - End-to-End Matching Flow

**Files:**
- Create: `src/test/java/com/datingapp/api/MatchingFlowE2EIntegrationTest.java`

**Step 1: Write the comprehensive end-to-end test**

```java
// File: src/test/java/com/datingapp/api/MatchingFlowE2EIntegrationTest.java
package com.datingapp.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.datingapp.IntegrationTestBase;
import com.datingapp.domain.Distance;
import com.datingapp.domain.Interest;
import com.datingapp.domain.Location;
import com.datingapp.domain.Preferences;
import com.datingapp.domain.Profile;
import com.datingapp.domain.SwipeDirection;
import com.datingapp.domain.User;
import com.datingapp.domain.UserId;
import com.datingapp.infrastructure.security.JwtTokenProvider;
import com.datingapp.domain.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * End-to-end test: User discovers matches, swipes, and creates mutual match.
 * Tests the complete flow: GET /api/prospects → POST /api/swipes → GET /api/matches
 */
@SpringBootTest
class MatchingFlowE2EIntegrationTest extends IntegrationTestBase {
    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private MockMvc mockMvc;

    @Override
    public void setUp() {
        super.setUp();
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void completeMatchingFlow_discoveryToMatch() throws Exception {
        // Step 1: Create two users
        UserId alice = UserId.generate();
        User aliceUser = createUser(alice, "alice", 40.7128, -74.0060, Set.of(Interest.HIKING, Interest.MUSIC));
        userRepository.save(aliceUser);

        UserId bob = UserId.generate();
        User bobUser = createUser(bob, "bob", 40.7306, -73.9352, Set.of(Interest.HIKING));
        userRepository.save(bobUser);

        String aliceToken = jwtTokenProvider.generateToken(alice);
        String bobToken = jwtTokenProvider.generateToken(bob);

        // Step 2: Alice discovers Bob via /api/prospects
        MvcResult prospectResult = mockMvc.perform(get("/api/prospects")
                .header("Authorization", "Bearer " + aliceToken)
                .param("limit", "10")
                .param("maxDistanceKm", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prospects[0].userId").exists())
                .andReturn();

        // Verify Bob is in prospects
        String prospectJson = prospectResult.getResponse().getContentAsString();
        assertTrue(prospectJson.contains(bob.value().toString()), "Bob should appear in Alice's prospects");

        // Step 3: Alice swipes LIKE on Bob via /api/swipes
        com.datingapp.api.dto.SwipeRequestDto aliceSwipe = new com.datingapp.api.dto.SwipeRequestDto(bob, SwipeDirection.LIKE);
        String aliceSwipeJson = new ObjectMapper().writeValueAsString(aliceSwipe);

        mockMvc.perform(post("/api/swipes")
                .header("Authorization", "Bearer " + aliceToken)
                .contentType("application/json")
                .content(aliceSwipeJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.match").doesNotExist()); // No match yet

        // Step 4: Bob discovers Alice via /api/prospects
        MvcResult bobProspectResult = mockMvc.perform(get("/api/prospects")
                .header("Authorization", "Bearer " + bobToken)
                .param("limit", "10")
                .param("maxDistanceKm", "100"))
                .andExpect(status().isOk())
                .andReturn();

        String bobProspectJson = bobProspectResult.getResponse().getContentAsString();
        assertTrue(bobProspectJson.contains(alice.value().toString()), "Alice should appear in Bob's prospects");

        // Step 5: Bob swipes LIKE on Alice → Creates mutual match
        com.datingapp.api.dto.SwipeRequestDto bobSwipe = new com.datingapp.api.dto.SwipeRequestDto(alice, SwipeDirection.LIKE);
        String bobSwipeJson = new ObjectMapper().writeValueAsString(bobSwipe);

        mockMvc.perform(post("/api/swipes")
                .header("Authorization", "Bearer " + bobToken)
                .contentType("application/json")
                .content(bobSwipeJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.match").exists()); // Match created!

        // Step 6: Alice checks her matches via /api/matches
        mockMvc.perform(get("/api/matches")
                .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matches[0].displayName").value("bob"));

        // Step 7: Bob checks his matches via /api/matches
        mockMvc.perform(get("/api/matches")
                .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matches[0].displayName").value("alice"));
    }

    private User createUser(UserId id, String username, double lat, double lon, Set<Interest> interests) {
        Location location = new Location(lat, lon);
        Preferences preferences = new Preferences(Set.of("ALL"), null, Distance.ofKilometers(100));
        Profile profile = new Profile(
                id,
                username,
                "Bio for " + username,
                LocalDate.of(1990, 1, 1),
                interests,
                preferences,
                location,
                java.util.List.of()
        );
        return new User(id, username, profile);
    }
}
```

**Step 2: Run test to verify it passes**

Run: `./mvnw test -Dtest=MatchingFlowE2EIntegrationTest -v`

Expected: PASS (complete flow works end-to-end)

**Step 3: Run all tests to ensure nothing broke**

Run: `./mvnw test`

Expected: All tests pass (88 old + 13 new = 101 tests)

**Step 4: Commit**

```bash
git add src/test/java/com/datingapp/api/MatchingFlowE2EIntegrationTest.java
git commit -m "test: add end-to-end integration test for complete matching flow"
```

---

### Task 6: Bug Fixes and Cleanup

**Files:**
- Modify: `src/main/java/com/datingapp/domain/Location.java` (if distanceTo() missing)

**Step 1: Run all tests and fix any compilation errors**

Run: `./mvnw clean compile`

Expected: All classes compile without errors

If `Location.distanceTo()` is missing, add it:

```java
// In src/main/java/com/datingapp/domain/Location.java
public Distance distanceTo(Location other) {
    double latDiff = Math.toRadians(other.latitude - this.latitude);
    double lonDiff = Math.toRadians(other.longitude - this.longitude);

    double a = Math.sin(latDiff / 2) * Math.sin(latDiff / 2) +
            Math.cos(Math.toRadians(this.latitude)) * Math.cos(Math.toRadians(other.latitude)) *
            Math.sin(lonDiff / 2) * Math.sin(lonDiff / 2);

    double c = 2 * Math.asin(Math.sqrt(a));
    double km = 6371 * c;

    return Distance.ofKilometers(km);
}
```

**Step 2: Run full test suite**

Run: `./mvnw test -v`

Expected: 101 tests pass

**Step 3: Commit**

```bash
git add src/main/java/com/datingapp/domain/Location.java
git commit -m "feat: add distanceTo() method to Location value object"
```

---

## Summary

**6 tasks will be completed:**
1. ✓ ProspectsService + ProspectDTO
2. ✓ ProspectsController + GET /api/prospects
3. ✓ SwipesController + POST /api/swipes
4. ✓ MatchesController + GET /api/matches
5. ✓ End-to-end integration test
6. ✓ Bug fixes and full test verification

**Result:** Phase 2 complete with `/api/prospects`, `/api/swipes`, `/api/matches` endpoints.