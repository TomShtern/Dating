package com.datingapp;

import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
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
import com.datingapp.infrastructure.persistence.inmemory.InMemoryMatchRepository;
import com.datingapp.infrastructure.persistence.inmemory.InMemorySwipeRepository;
import com.datingapp.infrastructure.persistence.inmemory.InMemoryUserRepository;

/**
 * A demonstration test that walks through the entire matching flow
 * using in-memory repositories to prove the logic is sound.
 */
class FlowDemoTest {

    @Test
    void demonstrateFullMatchingFlow() {
        System.out.println("\n🚀 STARTING FULL MATCHING FLOW DEMO\n");

        // 1. SETUP RECURRING REPOS
        UserRepository userRepo = new InMemoryUserRepository();
        SwipeRepository swipeRepo = new InMemorySwipeRepository();
        MatchRepository matchRepo = new InMemoryMatchRepository();

        // 2. SETUP SERVICE
        MatchScorer scorer = new MatchScorer(List.of(new DistanceStrategy(Distance.ofKilometers(100))));
        MatchingService matchingService = new MatchingService(scorer, userRepo, swipeRepo, matchRepo, event -> {
        });

        // 3. CREATE ALICE (In Manhattan)
        UserId aliceId = UserId.generate();
        User alice = createSampleUser(aliceId, "Alice", 40.7128, -74.0060, Set.of(Interest.HIKING, Interest.TRAVEL));
        userRepo.save(alice);
        System.out.println("✅ Registered Alice (Manhattan)");

        // 4. CREATE BOB (In Brooklyn - ~10km away)
        UserId bobId = UserId.generate();
        User bob = createSampleUser(bobId, "Bob", 40.7306, -73.9352, Set.of(Interest.HIKING, Interest.MUSIC));
        userRepo.save(bob);
        System.out.println("✅ Registered Bob (Brooklyn)");

        // 5. DISCOVERY: Alice finds prospects
        System.out.println("\n🔍 Alice is looking for prospects within 50km...");
        List<Prospect> prospects = matchingService.findProspects(alice, Distance.ofKilometers(50), 10, Set.of());

        assertEquals(1, prospects.size());
        Prospect bobProspect = prospects.get(0);
        System.out.println("✨ Alice found: " + bobProspect.displayName() + " ("
                + Math.round(bobProspect.distance().kilometers()) + "km away)");

        // 6. SWIPE: Alice likes Bob
        System.out.println("\n❤️ Alice LIKES Bob...");
        Optional<Match> match1 = matchingService.processSwipe(aliceId, bobId, SwipeDirection.LIKE);
        assertTrue(match1.isEmpty(), "Match should not exist yet");
        System.out.println("   (Recorded. Waiting for mutual interest...)");

        // 7. SWIPE: Bob likes Alice
        System.out.println("\n❤️ Bob LIKES Alice back...");
        Optional<Match> match2 = matchingService.processSwipe(bobId, aliceId, SwipeDirection.LIKE);

        assertTrue(match2.isPresent(), "Match should be created now!");
        System.out.println("🎉 BOOM! IT'S A MATCH!");
        System.out.println("   Match ID: " + match2.get().getId().value());

        // 8. VERIFY: View matches
        List<Match> aliceMatches = matchRepo.findByUser(aliceId);
        assertEquals(1, aliceMatches.size());
        System.out.println("\n✅ Alice now has " + aliceMatches.size() + " match in her list.");

        System.out.println("\n🏁 DEMO COMPLETED SUCCESSFULLY\n");
    }

    private User createSampleUser(UserId id, String name, double lat, double lon, Set<Interest> interests) {
        Location loc = new Location(lat, lon);
        Preferences prefs = new Preferences(Set.of("ALL"), null, Distance.ofKilometers(100));
        Profile profile = new Profile(id, name, "I love coding", LocalDate.of(1995, 1, 1),
                interests, prefs, loc, List.of("photo.jpg"));
        return new User(id, name, profile);
    }
}
