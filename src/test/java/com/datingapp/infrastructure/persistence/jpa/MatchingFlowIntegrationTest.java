package com.datingapp.infrastructure.persistence.jpa;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
 * Tests: User creation → Discovery → Swipe → Match creation with real
 * PostgreSQL.
 *
 * ★ Insight ─────────────────────────────────────
 * This test exercises the full matching pipeline:
 * 1. Users are persisted with profiles (UserRepository)
 * 2. Matching algorithm discovers prospects (MatchingService)
 * 3. Swipes are recorded (SwipeRepository)
 * 4. Mutual likes create matches (MatchRepository)
 * All operations use real PostgreSQL via Testcontainers.
 * ─────────────────────────────────────────────────
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
                Set.of());

        // Then: Bob should appear in Alice's prospects
        assertEquals(1, aliceProspects.size());
        assertEquals(bob.getId(), aliceProspects.get(0).userId());
        assertEquals(1, aliceProspects.get(0).sharedInterests().size());

        // When: Alice likes Bob
        Optional<Match> matchAfterAliceLikes = matchingService.processSwipe(
                alice.getId(),
                bob.getId(),
                SwipeDirection.LIKE);

        // Then: No match yet (waiting for mutual interest)
        assertTrue(matchAfterAliceLikes.isEmpty());

        // When: Bob likes Alice back
        Optional<Match> matchAfterBobLikes = matchingService.processSwipe(
                bob.getId(),
                alice.getId(),
                SwipeDirection.LIKE);

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
        Optional<Match> secondMatch = matchingService.processSwipe(bob.getId(), alice.getId(),
                SwipeDirection.SUPER_LIKE);

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
                List.of("photo1.jpg"));

        User user = new User(userId, username, profile);
        userRepository.save(user);
        return user;
    }

    private MatchingService createMatchingService() {
        MatchScorer scorer = new MatchScorer(List.of(
                new DistanceStrategy(Distance.ofKilometers(100))));

        return new MatchingService(
                scorer,
                userRepository,
                swipeRepository,
                matchRepository,
                event -> {
                } // No-op event publisher for tests
        );
    }
}
