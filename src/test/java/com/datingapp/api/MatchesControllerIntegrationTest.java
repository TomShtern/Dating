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

    public void setUp() {
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

        String token = jwtTokenProvider.generateToken(alice, "alice");

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
                java.util.List.of("photo1.jpg")
        );
        return new User(id, username, profile);
    }
}
