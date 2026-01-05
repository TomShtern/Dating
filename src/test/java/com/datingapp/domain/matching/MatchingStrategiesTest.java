package com.datingapp.domain.matching;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.datingapp.domain.Distance;
import com.datingapp.domain.Location;
import com.datingapp.domain.Profile;
import com.datingapp.domain.User;
import com.datingapp.domain.UserId;

class MatchingStrategiesTest {

    @Nested
    class DistanceStrategyScoring {

        @Test
        void shouldReturn1_whenSameLocation() {
            DistanceStrategy strategy = new DistanceStrategy(Distance.ofKilometers(100));

            User user1 = createUserAt(40.7128, -74.0060);
            User user2 = createUserAt(40.7128, -74.0060);

            double score = strategy.score(user1, user2);

            assertEquals(1.0, score, 0.001, "Same location should score 1.0");
        }

        @Test
        void shouldReturn0_whenBeyondMaxDistance() {
            DistanceStrategy strategy = new DistanceStrategy(Distance.ofKilometers(50));

            User nyc = createUserAt(40.7128, -74.0060);
            User la = createUserAt(34.0522, -118.2437); // ~3940 km away

            double score = strategy.score(la, nyc);

            assertEquals(0.0, score, "Beyond max distance should score 0.0");
        }

        @Test
        void shouldReturn0_whenExactlyAtMaxDistance() {
            // Create a strategy with max 10km
            DistanceStrategy strategy = new DistanceStrategy(Distance.ofKilometers(10));

            // These locations are approximately 10km apart
            User user1 = createUserAt(40.7128, -74.0060);
            User user2 = createUserAt(40.8028, -74.0060); // ~10km north

            double score = strategy.score(user2, user1);

            // At exactly max distance, score should be 0 (or very close)
            assertTrue(score <= 0.1, "At max distance, score should be near 0, was: " + score);
        }

        @Test
        void shouldScoreLinearlyWithDistance() {
            DistanceStrategy strategy = new DistanceStrategy(Distance.ofKilometers(100));

            User center = createUserAt(40.0, -74.0);
            User near = createUserAt(40.09, -74.0);   // ~10km
            User medium = createUserAt(40.45, -74.0); // ~50km
            User far = createUserAt(40.9, -74.0);     // ~100km

            double nearScore = strategy.score(near, center);
            double mediumScore = strategy.score(medium, center);
            double farScore = strategy.score(far, center);

            assertTrue(nearScore > mediumScore, "Nearer user should score higher");
            assertTrue(mediumScore > farScore, "Medium distance should score higher than far");
            assertTrue(nearScore > 0.8, "10km of 100km should score > 0.8, was: " + nearScore);
            assertTrue(mediumScore > 0.4 && mediumScore < 0.6,
                    "50km of 100km should score ~0.5, was: " + mediumScore);
        }

        @Test
        void shouldReturn0_whenCandidateLocationNull() {
            DistanceStrategy strategy = new DistanceStrategy(Distance.ofKilometers(100));

            User withLocation = createUserAt(40.0, -74.0);
            User withoutLocation = createUserWithNullLocation();

            double score = strategy.score(withoutLocation, withLocation);

            assertEquals(0.0, score, "Null candidate location should score 0");
        }

        @Test
        void shouldReturn0_whenRequesterLocationNull() {
            DistanceStrategy strategy = new DistanceStrategy(Distance.ofKilometers(100));

            User withLocation = createUserAt(40.0, -74.0);
            User withoutLocation = createUserWithNullLocation();

            double score = strategy.score(withLocation, withoutLocation);

            assertEquals(0.0, score, "Null requester location should score 0");
        }

        @Test
        void shouldReturnCorrectName() {
            DistanceStrategy strategy = new DistanceStrategy(Distance.ofKilometers(100));
            assertEquals("distance", strategy.name());
        }
    }

    @Nested
    class MatchScorerComposition {

        @Test
        void shouldReturn0Point5_whenNoStrategies() {
            MatchScorer scorer = new MatchScorer(Collections.emptyList());

            User user1 = createUserAt(0, 0);
            User user2 = createUserAt(0, 0);

            assertEquals(0.5, scorer.score(user1, user2), "Empty strategies should return neutral 0.5");
        }

        @Test
        void shouldReturnStrategyScore_withSingleStrategy() {
            MatchScorer scorer = new MatchScorer(List.of(
                    new DistanceStrategy(Distance.ofKilometers(100))));

            User user1 = createUserAt(40.0, -74.0);
            User user2 = createUserAt(40.0, -74.0); // Same location

            assertEquals(1.0, scorer.score(user1, user2), 0.001);
        }

        @Test
        void shouldAverageMultipleStrategies() {
            // Create a mock strategy that always returns 0.8
            MatchStrategy fixedStrategy = new MatchStrategy() {
                @Override
                public double score(User candidate, User requester) {
                    return 0.8;
                }

                @Override
                public String name() {
                    return "fixed";
                }
            };

            // Create a mock strategy that always returns 0.4
            MatchStrategy anotherFixed = new MatchStrategy() {
                @Override
                public double score(User candidate, User requester) {
                    return 0.4;
                }

                @Override
                public String name() {
                    return "another";
                }
            };

            MatchScorer scorer = new MatchScorer(List.of(fixedStrategy, anotherFixed));

            User user1 = createUserAt(0, 0);
            User user2 = createUserAt(0, 0);

            // Average of 0.8 and 0.4 = 0.6
            assertEquals(0.6, scorer.score(user1, user2), 0.001);
        }

        @Test
        void shouldHandleStrategiesReturningZero() {
            MatchStrategy zeroStrategy = new MatchStrategy() {
                @Override
                public double score(User candidate, User requester) {
                    return 0.0;
                }

                @Override
                public String name() {
                    return "zero";
                }
            };

            MatchStrategy oneStrategy = new MatchStrategy() {
                @Override
                public double score(User candidate, User requester) {
                    return 1.0;
                }

                @Override
                public String name() {
                    return "one";
                }
            };

            MatchScorer scorer = new MatchScorer(List.of(zeroStrategy, oneStrategy));

            User user1 = createUserAt(0, 0);
            User user2 = createUserAt(0, 0);

            assertEquals(0.5, scorer.score(user1, user2), 0.001, "Average of 0 and 1 should be 0.5");
        }

        @Test
        void shouldBeImmutable() {
            List<MatchStrategy> strategies = new java.util.ArrayList<>();
            strategies.add(new DistanceStrategy(Distance.ofKilometers(100)));

            MatchScorer scorer = new MatchScorer(strategies);

            // Modify original list
            strategies.clear();

            // Scorer should still work
            User user1 = createUserAt(40.0, -74.0);
            User user2 = createUserAt(40.0, -74.0);

            assertEquals(1.0, scorer.score(user1, user2), 0.001,
                    "Scorer should maintain its own copy of strategies");
        }
    }

    private User createUserAt(double lat, double lon) {
        UserId id = UserId.generate();
        Profile profile = new Profile(
                id, "TestUser", "Bio",
                LocalDate.now().minusYears(25),
                Collections.emptySet(), null,
                new Location(lat, lon),
                List.of("photo.jpg"));
        return new User(id, "testuser_" + id.value().toString().substring(0, 8), profile);
    }

    private User createUserWithNullLocation() {
        UserId id = UserId.generate();
        Profile profile = new Profile(
                id, "TestUser", "Bio",
                LocalDate.now().minusYears(25),
                Collections.emptySet(), null,
                null, // null location
                List.of("photo.jpg"));
        return new User(id, "testuser_" + id.value().toString().substring(0, 8), profile);
    }
}
