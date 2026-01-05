package com.datingapp.domain;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class DomainAggregatesTest {

    @Test
    void user_shouldHandleStateTransitions() {
        UserId id = UserId.generate();
        Profile incompleteProfile = new Profile(id, null, null, null, null, null, null, null);
        User user = new User(id, "testuser", incompleteProfile);

        assertEquals(UserState.PROFILE_INCOMPLETE, user.getState());
        assertFalse(user.canSwipe());

        Profile completeProfile = new Profile(id, "Alice", "Bio",
                LocalDate.now().minusYears(25), Collections.emptySet(),
                null, new Location(0, 0), List.of("url1"));

        user.updateProfile(completeProfile);
        assertEquals(UserState.ACTIVE, user.getState());
        assertTrue(user.canSwipe());

        user.pause();
        assertEquals(UserState.PAUSED, user.getState());
        assertFalse(user.canSwipe());

        user.activate();
        assertEquals(UserState.ACTIVE, user.getState());

        user.ban("Violation");
        assertEquals(UserState.BANNED, user.getState());
        assertFalse(user.canBeDiscovered());

        // Ensure updates are blocked when banned
        assertThrows(IllegalStateException.class, () -> user.updateProfile(completeProfile));
    }

    @Test
    void profile_shouldEnforcePhotoLimit() {
        UserId id = UserId.generate();
        assertThrows(IllegalArgumentException.class,
                () -> new Profile(id, "Name", "Bio", LocalDate.now().minusYears(20),
                        Collections.emptySet(), null, new Location(0, 0),
                        List.of("url1", "url2", "url3")));
    }

    @Test
    void swipe_shouldPreventSelfSwipe() {
        UserId userA = UserId.generate();
        assertThrows(IllegalArgumentException.class, () -> Swipe.create(userA, userA, SwipeDirection.LIKE));

        UserId userB = UserId.generate();
        Swipe swipe = Swipe.create(userA, userB, SwipeDirection.LIKE);
        assertEquals(userA, swipe.getSwiperId());
        assertEquals(userB, swipe.getTargetId());
        assertTrue(swipe.isLike());
    }

    @Test
    void match_shouldOrderingIdempotently() {
        UserId userA = UserId.of(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"));
        UserId userB = UserId.of(java.util.UUID.fromString("00000000-0000-0000-0000-000000000002"));

        Match match1 = Match.create(userA, userB);
        Match match2 = Match.create(userB, userA);

        assertEquals(match1.getId(), match2.getId());
        assertEquals(userA, match1.getUserA());
        assertEquals(userB, match1.getUserB());
        assertTrue(match1.involves(userA));
        assertEquals(userB, match1.otherUser(userA));
        assertThrows(IllegalArgumentException.class, () -> match1.otherUser(UserId.generate()));
    }

    @Test
    void match_shouldReconstituteCorrectly() {
        MatchId id = new MatchId("custom_id");
        UserId a = UserId.generate();
        UserId b = UserId.generate();
        java.time.Instant now = java.time.Instant.now();

        Match match = Match.reconstitute(id, a, b, now);

        assertEquals(id, match.getId());
        assertFalse(match.isNewlyCreated());
        assertEquals(now, match.getCreatedAt());
    }
}
