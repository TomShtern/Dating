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

    public void setUp() {
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

        String aliceToken = jwtTokenProvider.generateToken(alice, "alice");
        String bobToken = jwtTokenProvider.generateToken(bob, "bob");

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
                java.util.List.of("photo1.jpg")
        );
        return new User(id, username, profile);
    }
}
