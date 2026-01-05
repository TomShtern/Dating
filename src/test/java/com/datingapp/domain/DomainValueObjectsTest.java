package com.datingapp.domain;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class DomainValueObjectsTest {

    @Test
    void userId_shouldWorkCorrectly() {
        UUID uuid = UUID.randomUUID();
        UserId id = UserId.of(uuid);
        assertEquals(uuid, id.value());
        assertEquals(uuid.toString(), id.toString());
        assertNotNull(UserId.generate());
        assertThrows(NullPointerException.class, () -> new UserId(null));
    }

    @Test
    void userId_shouldBeEqualWhenSameUUID() {
        UUID uuid = UUID.randomUUID();
        UserId id1 = UserId.of(uuid);
        UserId id2 = UserId.of(uuid);

        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void userId_shouldNotBeEqualWhenDifferentUUID() {
        UserId id1 = UserId.generate();
        UserId id2 = UserId.generate();

        assertNotEquals(id1, id2);
    }

    @Test
    void swipeId_shouldGenerateUniqueIds() {
        SwipeId id1 = SwipeId.generate();
        SwipeId id2 = SwipeId.generate();

        assertNotNull(id1.value());
        assertNotNull(id2.value());
        assertNotEquals(id1, id2, "Generated SwipeIds should be unique");
    }

    @Test
    void swipeId_shouldRejectNull() {
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> new SwipeId(null));

        assertEquals("SwipeId cannot be null", ex.getMessage());
    }

    @Test
    void swipeId_shouldBeEqualWhenSameUUID() {
        UUID uuid = UUID.randomUUID();
        SwipeId id1 = new SwipeId(uuid);
        SwipeId id2 = new SwipeId(uuid);

        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void matchId_shouldBeCanonical() {
        UserId idA = UserId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        UserId idB = UserId.of(UUID.fromString("00000000-0000-0000-0000-000000000002"));

        MatchId match1 = MatchId.canonical(idA, idB);
        MatchId match2 = MatchId.canonical(idB, idA);

        assertEquals(match1, match2);
        assertEquals("00000000-0000-0000-0000-000000000001_00000000-0000-0000-0000-000000000002", match1.value());
    }

    @Test
    void location_shouldCalculateDistance() {
        // NYC: 40.7128° N, 74.0060° W
        // LA: 34.0522° N, 118.2437° W
        Location nyc = new Location(40.7128, -74.0060);
        Location la = new Location(34.0522, -118.2437);

        Distance distance = nyc.distanceTo(la);
        // Approximately 3940 km
        assertEquals(3940, Math.round(distance.kilometers()), 50);

        assertThrows(IllegalArgumentException.class, () -> new Location(91, 0));
        assertThrows(IllegalArgumentException.class, () -> new Location(0, 181));
    }

    @Test
    void distance_shouldCompareCorrectly() {
        Distance d1 = Distance.ofKilometers(10);
        Distance d2 = Distance.ofKilometers(20);

        assertTrue(d1.isLessThanOrEqual(d2));
        assertTrue(d2.isGreaterThan(d1));
        assertNotEquals(d1, d2);

        assertThrows(IllegalArgumentException.class, () -> new Distance(-1));
    }

    @Test
    void ageRange_shouldValidateAndContain() {
        AgeRange range = AgeRange.of(18, 30);
        assertTrue(range.contains(25));
        assertTrue(range.contains(18));
        assertTrue(range.contains(30));
        assertFalse(range.contains(17));
        assertFalse(range.contains(31));

        assertThrows(IllegalArgumentException.class, () -> AgeRange.of(17, 30));
        assertThrows(IllegalArgumentException.class, () -> AgeRange.of(30, 20));
    }

    @Test
    void userState_shouldHaveCorrectPermissions() {
        assertTrue(UserState.ACTIVE.canSwipe());
        assertTrue(UserState.ACTIVE.canMessage());
        assertFalse(UserState.PAUSED.canSwipe());
        assertFalse(UserState.BANNED.canBeDiscovered());
    }

    @Test
    void swipeDirection_shouldIdentifyLikes() {
        assertTrue(SwipeDirection.LIKE.isLike());
        assertTrue(SwipeDirection.SUPER_LIKE.isLike());
        assertFalse(SwipeDirection.DISLIKE.isLike());
    }

    @Test
    void interest_shouldHaveExpectedValues() {
        assertEquals("HIKING", Interest.HIKING.name());
        assertTrue(Interest.values().length >= 10);
    }
}
