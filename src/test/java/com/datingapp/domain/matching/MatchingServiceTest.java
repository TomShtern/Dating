package com.datingapp.domain.matching;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.datingapp.domain.*;
import com.datingapp.domain.event.*;
import com.datingapp.domain.repository.*;
import com.datingapp.infrastructure.persistence.inmemory.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

class MatchingServiceTest {

    private UserRepository userRepo;
    private SwipeRepository swipeRepo;
    private MatchRepository matchRepo;
    private EventPublisher eventPublisher;
    private MatchingService matchingService;

    @BeforeEach
    void setUp() {
        userRepo = new InMemoryUserRepository();
        swipeRepo = new InMemorySwipeRepository();
        matchRepo = new InMemoryMatchRepository();
        eventPublisher = mock(EventPublisher.class);

        MatchScorer scorer = new MatchScorer(List.of(
                new DistanceStrategy(Distance.ofKilometers(100))));

        matchingService = new MatchingService(
                scorer, userRepo, swipeRepo, matchRepo, eventPublisher);
    }

    @Test
    void processSwipe_shouldCreateMatchOnMutualLike() {
        UserId user1 = UserId.generate();
        UserId user2 = UserId.generate();

        // User 1 likes User 2
        Optional<Match> match1 = matchingService.processSwipe(user1, user2, SwipeDirection.LIKE);
        assertFalse(match1.isPresent());
        verify(eventPublisher, never()).publish(any());

        // User 2 likes User 1
        Optional<Match> match2 = matchingService.processSwipe(user2, user1, SwipeDirection.LIKE);
        assertTrue(match2.isPresent());
        assertEquals(MatchId.canonical(user1, user2), match2.get().getId());

        // Verify event was published
        verify(eventPublisher, times(1)).publish(any(MatchCreatedEvent.class));

        // Test Idempotency: Duplicate swipe should not create another match or event
        Optional<Match> match3 = matchingService.processSwipe(user2, user1, SwipeDirection.LIKE);
        assertTrue(match3.isPresent());
        verify(eventPublisher, times(1)).publish(any(MatchCreatedEvent.class));
    }

    @Test
    void processSwipe_shouldNotCreateMatchOnDislike() {
        UserId user1 = UserId.generate();
        UserId user2 = UserId.generate();

        matchingService.processSwipe(user1, user2, SwipeDirection.DISLIKE);
        matchingService.processSwipe(user2, user1, SwipeDirection.LIKE);

        assertFalse(matchRepo.findByUser(user1).stream().anyMatch(m -> m.involves(user2)));
    }

    @Test
    void findProspects_shouldFilterAndScore() {
        User requester = createTestUser("Requester", 40.7128, -74.0060); // NYC
        User near = createTestUser("Near", 40.7306, -73.9352); // Brooklyn
        User far = createTestUser("Far", 34.0522, -118.2437); // LA

        userRepo.save(requester);
        userRepo.save(near);
        userRepo.save(far);

        Distance radius = Distance.ofKilometers(100);
        List<Prospect> prospects = matchingService.findProspects(requester, radius, 10, Collections.emptySet());

        assertEquals(1, prospects.size());
        assertEquals(near.getId(), prospects.get(0).userId());
        assertTrue(prospects.get(0).score() > 0.9);

        // Verify exclusion from future prospects once swiped
        matchingService.processSwipe(requester.getId(), near.getId(), SwipeDirection.LIKE);
        Set<UserId> swiped = swipeRepo.findSwipedUserIds(requester.getId());
        List<Prospect> updatedProspects = matchingService.findProspects(requester, radius, 10, swiped);
        assertTrue(updatedProspects.isEmpty(), "Swiped user should be excluded");
    }

    @Test
    void findProspects_shouldSortByScore() {
        User requester = createTestUser("Requester", 40.7, -74.0);
        User middle = createTestUser("Middle", 40.8, -74.1); // ~14km
        User closest = createTestUser("Closest", 40.71, -74.01); // ~1km

        userRepo.save(requester);
        userRepo.save(middle);
        userRepo.save(closest);

        Distance radius = Distance.ofKilometers(50);
        List<Prospect> prospects = matchingService.findProspects(requester, radius, 10, Collections.emptySet());

        assertEquals(2, prospects.size());
        assertEquals("Closest", prospects.get(0).displayName());
        assertEquals("Middle", prospects.get(1).displayName());
        assertTrue(prospects.get(0).score() > prospects.get(1).score());
    }

    private User createTestUser(String name, double lat, double lon) {
        UserId id = UserId.generate();
        Profile p = new Profile(id, name, "Bio",
                LocalDate.now().minusYears(25), Collections.emptySet(),
                null, new Location(lat, lon), List.of("url"));
        return new User(id, p);
    }
}
