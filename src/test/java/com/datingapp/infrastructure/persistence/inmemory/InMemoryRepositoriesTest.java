package com.datingapp.infrastructure.persistence.inmemory;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.datingapp.domain.*;

class InMemoryRepositoriesTest {

    @Nested
    class UserRepositoryTest {

        private InMemoryUserRepository repo;

        @BeforeEach
        void setUp() {
            repo = new InMemoryUserRepository();
        }

        @Test
        void findById_shouldReturnEmpty_whenNotFound() {
            Optional<User> result = repo.findById(UserId.generate());

            assertTrue(result.isEmpty());
        }

        @Test
        void findById_shouldReturnUser_afterSave() {
            User user = createActiveUserAt(40.7, -74.0, "Alice");
            repo.save(user);

            Optional<User> result = repo.findById(user.getId());

            assertTrue(result.isPresent());
            assertEquals("Alice", result.get().getProfile().displayName());
        }

        @Test
        void save_shouldOverwriteExistingUser() {
            UserId id = UserId.generate();
            User original = new User(id, "original_user", createProfileAt(id, 40.7, -74.0, "Original"));
            repo.save(original);

            User updated = new User(id, "original_user", createProfileAt(id, 40.7, -74.0, "Updated"));
            repo.save(updated);

            Optional<User> result = repo.findById(id);
            assertEquals("Updated", result.get().getProfile().displayName());
        }

        @Test
        void existsById_shouldReturnFalse_whenNotFound() {
            assertFalse(repo.existsById(UserId.generate()));
        }

        @Test
        void existsById_shouldReturnTrue_afterSave() {
            User user = createActiveUserAt(40.7, -74.0, "Alice");
            repo.save(user);

            assertTrue(repo.existsById(user.getId()));
        }

        @Test
        void findDiscoverableInRadius_shouldReturnEmpty_whenNoUsers() {
            List<User> result = repo.findDiscoverableInRadius(
                    new Location(40.7, -74.0),
                    Distance.ofKilometers(100),
                    10);

            assertTrue(result.isEmpty());
        }

        @Test
        void findDiscoverableInRadius_shouldExcludeUsersOutsideRadius() {
            User nearNYC = createActiveUserAt(40.73, -73.99, "Near"); // ~3km
            User inLA = createActiveUserAt(34.05, -118.24, "Far");   // ~3940km

            repo.save(nearNYC);
            repo.save(inLA);

            List<User> result = repo.findDiscoverableInRadius(
                    new Location(40.7128, -74.0060),
                    Distance.ofKilometers(50),
                    10);

            assertEquals(1, result.size());
            assertEquals("Near", result.get(0).getProfile().displayName());
        }

        @Test
        void findDiscoverableInRadius_shouldExcludeNonDiscoverableUsers() {
            User active = createActiveUserAt(40.73, -73.99, "Active");
            User paused = createActiveUserAt(40.74, -73.98, "Paused");
            paused.pause();

            repo.save(active);
            repo.save(paused);

            List<User> result = repo.findDiscoverableInRadius(
                    new Location(40.7128, -74.0060),
                    Distance.ofKilometers(50),
                    10);

            assertEquals(1, result.size());
            assertEquals("Active", result.get(0).getProfile().displayName());
        }

        @Test
        void findDiscoverableInRadius_shouldExcludeUsersWithNullLocation() {
            User withLocation = createActiveUserAt(40.73, -73.99, "WithLocation");
            User withoutLocation = createUserWithNullLocation("WithoutLocation");

            repo.save(withLocation);
            repo.save(withoutLocation);

            List<User> result = repo.findDiscoverableInRadius(
                    new Location(40.7128, -74.0060),
                    Distance.ofKilometers(50),
                    10);

            assertEquals(1, result.size());
            assertEquals("WithLocation", result.get(0).getProfile().displayName());
        }

        @Test
        void findDiscoverableInRadius_shouldRespectLimit() {
            for (int i = 0; i < 10; i++) {
                repo.save(createActiveUserAt(40.7 + i * 0.001, -74.0, "User" + i));
            }

            List<User> result = repo.findDiscoverableInRadius(
                    new Location(40.7, -74.0),
                    Distance.ofKilometers(100),
                    5);

            assertEquals(5, result.size());
        }

        @Test
        void findDiscoverableInRadius_shouldIncludeUserAtExactBoundary() {
            // Create a user at exactly the boundary (tricky edge case)
            User atBoundary = createActiveUserAt(40.8, -74.0, "AtBoundary");
            repo.save(atBoundary);

            Location center = new Location(40.7, -74.0);
            Distance toUser = center.distanceTo(atBoundary.getProfile().location());

            List<User> result = repo.findDiscoverableInRadius(
                    center,
                    toUser, // Exactly the distance to the user
                    10);

            assertEquals(1, result.size(), "User at exact boundary should be included");
        }

        private User createActiveUserAt(double lat, double lon, String name) {
            UserId id = UserId.generate();
            Profile profile = createProfileAt(id, lat, lon, name);
            return new User(id, name.toLowerCase().replaceAll("\\s+", "_"), profile);
        }

        private User createUserWithNullLocation(String name) {
            UserId id = UserId.generate();
            Profile profile = new Profile(id, name, "Bio",
                    LocalDate.now().minusYears(25), Collections.emptySet(),
                    null, null, List.of("photo.jpg"));
            return new User(id, name.toLowerCase().replaceAll("\\s+", "_"), profile);
        }

        private Profile createProfileAt(UserId id, double lat, double lon, String name) {
            return new Profile(id, name, "Bio",
                    LocalDate.now().minusYears(25), Collections.emptySet(),
                    null, new Location(lat, lon), List.of("photo.jpg"));
        }
    }

    @Nested
    class SwipeRepositoryTest {

        private InMemorySwipeRepository repo;

        @BeforeEach
        void setUp() {
            repo = new InMemorySwipeRepository();
        }

        @Test
        void saveIfNotExists_shouldSaveNewSwipe() {
            UserId swiper = UserId.generate();
            UserId target = UserId.generate();
            Swipe swipe = Swipe.create(swiper, target, SwipeDirection.LIKE);

            Swipe saved = repo.saveIfNotExists(swipe);

            assertSame(swipe, saved, "Should return the same swipe instance");
        }

        @Test
        void saveIfNotExists_shouldReturnExisting_whenDuplicate() {
            UserId swiper = UserId.generate();
            UserId target = UserId.generate();

            Swipe first = Swipe.create(swiper, target, SwipeDirection.LIKE);
            Swipe second = Swipe.create(swiper, target, SwipeDirection.DISLIKE);

            Swipe saved1 = repo.saveIfNotExists(first);
            Swipe saved2 = repo.saveIfNotExists(second);

            assertSame(first, saved1);
            assertSame(first, saved2, "Second save should return first swipe");
            assertEquals(SwipeDirection.LIKE, saved2.getDirection(),
                    "Direction should be from first swipe");
        }

        @Test
        void findByPair_shouldReturnEmpty_whenNotFound() {
            Optional<Swipe> result = repo.findByPair(UserId.generate(), UserId.generate());

            assertTrue(result.isEmpty());
        }

        @Test
        void findByPair_shouldFindExactPair() {
            UserId swiper = UserId.generate();
            UserId target = UserId.generate();
            Swipe swipe = Swipe.create(swiper, target, SwipeDirection.LIKE);
            repo.saveIfNotExists(swipe);

            Optional<Swipe> result = repo.findByPair(swiper, target);

            assertTrue(result.isPresent());
            assertEquals(swiper, result.get().getSwiperId());
        }

        @Test
        void findByPair_shouldNotFindReversePair() {
            UserId userA = UserId.generate();
            UserId userB = UserId.generate();
            repo.saveIfNotExists(Swipe.create(userA, userB, SwipeDirection.LIKE));

            // Looking for B -> A should not find A -> B
            Optional<Swipe> result = repo.findByPair(userB, userA);

            assertTrue(result.isEmpty(), "Reverse pair should not be found");
        }

        @Test
        void findSwipedUserIds_shouldReturnEmpty_whenNoSwipes() {
            Set<UserId> result = repo.findSwipedUserIds(UserId.generate());

            assertTrue(result.isEmpty());
        }

        @Test
        void findSwipedUserIds_shouldReturnAllTargets() {
            UserId swiper = UserId.generate();
            UserId target1 = UserId.generate();
            UserId target2 = UserId.generate();
            UserId target3 = UserId.generate();

            repo.saveIfNotExists(Swipe.create(swiper, target1, SwipeDirection.LIKE));
            repo.saveIfNotExists(Swipe.create(swiper, target2, SwipeDirection.DISLIKE));
            repo.saveIfNotExists(Swipe.create(swiper, target3, SwipeDirection.SUPER_LIKE));

            Set<UserId> result = repo.findSwipedUserIds(swiper);

            assertEquals(3, result.size());
            assertTrue(result.contains(target1));
            assertTrue(result.contains(target2));
            assertTrue(result.contains(target3));
        }

        @Test
        void findSwipedUserIds_shouldNotIncludeOtherSwipersTargets() {
            UserId swiperA = UserId.generate();
            UserId swiperB = UserId.generate();
            UserId target1 = UserId.generate();
            UserId target2 = UserId.generate();

            repo.saveIfNotExists(Swipe.create(swiperA, target1, SwipeDirection.LIKE));
            repo.saveIfNotExists(Swipe.create(swiperB, target2, SwipeDirection.LIKE));

            Set<UserId> resultA = repo.findSwipedUserIds(swiperA);

            assertEquals(1, resultA.size());
            assertTrue(resultA.contains(target1));
            assertFalse(resultA.contains(target2));
        }

        @Test
        void findPendingLikersFor_shouldReturnEmpty_whenNoLikers() {
            Set<UserId> result = repo.findPendingLikersFor(UserId.generate());

            assertTrue(result.isEmpty());
        }

        @Test
        void findPendingLikersFor_shouldReturnLikers() {
            UserId target = UserId.generate();
            UserId liker1 = UserId.generate();
            UserId liker2 = UserId.generate();
            UserId disliker = UserId.generate();

            repo.saveIfNotExists(Swipe.create(liker1, target, SwipeDirection.LIKE));
            repo.saveIfNotExists(Swipe.create(liker2, target, SwipeDirection.SUPER_LIKE));
            repo.saveIfNotExists(Swipe.create(disliker, target, SwipeDirection.DISLIKE));

            Set<UserId> result = repo.findPendingLikersFor(target);

            assertEquals(2, result.size());
            assertTrue(result.contains(liker1));
            assertTrue(result.contains(liker2));
            assertFalse(result.contains(disliker), "Disliker should not be included");
        }

        @Test
        void findPendingLikersFor_shouldNotIncludeLikesFromTarget() {
            UserId userA = UserId.generate();
            UserId userB = UserId.generate();

            // A likes B
            repo.saveIfNotExists(Swipe.create(userA, userB, SwipeDirection.LIKE));

            // Find who liked A (should be empty, B didn't like A)
            Set<UserId> likersOfA = repo.findPendingLikersFor(userA);
            assertTrue(likersOfA.isEmpty());

            // Find who liked B (should be A)
            Set<UserId> likersOfB = repo.findPendingLikersFor(userB);
            assertEquals(1, likersOfB.size());
            assertTrue(likersOfB.contains(userA));
        }
    }

    @Nested
    class MatchRepositoryTest {

        private InMemoryMatchRepository repo;

        @BeforeEach
        void setUp() {
            repo = new InMemoryMatchRepository();
        }

        @Test
        void saveIfNotExists_shouldSaveNewMatch() {
            Match match = Match.create(UserId.generate(), UserId.generate());

            Match saved = repo.saveIfNotExists(match);

            assertSame(match, saved);
        }

        @Test
        void saveIfNotExists_shouldReturnExisting_whenDuplicate() {
            UserId userA = UserId.generate();
            UserId userB = UserId.generate();

            Match first = Match.create(userA, userB);
            Match second = Match.create(userA, userB);

            Match saved1 = repo.saveIfNotExists(first);
            Match saved2 = repo.saveIfNotExists(second);

            assertSame(first, saved1);
            assertSame(first, saved2, "Second save should return first match");
        }

        @Test
        void findById_shouldReturnEmpty_whenNotFound() {
            MatchId id = MatchId.canonical(UserId.generate(), UserId.generate());

            Optional<Match> result = repo.findById(id);

            assertTrue(result.isEmpty());
        }

        @Test
        void findById_shouldFindSavedMatch() {
            Match match = Match.create(UserId.generate(), UserId.generate());
            repo.saveIfNotExists(match);

            Optional<Match> result = repo.findById(match.getId());

            assertTrue(result.isPresent());
            assertEquals(match.getId(), result.get().getId());
        }

        @Test
        void findByUser_shouldReturnEmpty_whenNoMatches() {
            List<Match> result = repo.findByUser(UserId.generate());

            assertTrue(result.isEmpty());
        }

        @Test
        void findByUser_shouldFindMatchesWhereUserIsParticipant() {
            UserId user = UserId.generate();
            UserId other1 = UserId.generate();
            UserId other2 = UserId.generate();
            UserId unrelated1 = UserId.generate();
            UserId unrelated2 = UserId.generate();

            repo.saveIfNotExists(Match.create(user, other1));
            repo.saveIfNotExists(Match.create(other2, user)); // user as second arg
            repo.saveIfNotExists(Match.create(unrelated1, unrelated2));

            List<Match> result = repo.findByUser(user);

            assertEquals(2, result.size());
            assertTrue(result.stream().allMatch(m -> m.involves(user)));
        }

        @Test
        void findByUser_shouldReturnMatchRegardlessOfUserPosition() {
            UserId userA = UserId.generate();
            UserId userB = UserId.generate();

            Match match = Match.create(userA, userB);
            repo.saveIfNotExists(match);

            // Both users should find this match
            List<Match> resultA = repo.findByUser(userA);
            List<Match> resultB = repo.findByUser(userB);

            assertEquals(1, resultA.size());
            assertEquals(1, resultB.size());
            assertEquals(match.getId(), resultA.get(0).getId());
            assertEquals(match.getId(), resultB.get(0).getId());
        }
    }
}
