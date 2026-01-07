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

/**
 * Integration tests for SwipeRepository with real PostgreSQL.
 * Verifies that swipes are properly persisted and retrieved.
 */
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
