package com.datingapp.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
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
import com.datingapp.domain.User;
import com.datingapp.domain.UserId;
import com.datingapp.infrastructure.security.JwtTokenProvider;
import com.datingapp.domain.repository.UserRepository;

@SpringBootTest
class ProspectsControllerIntegrationTest extends IntegrationTestBase {
    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private MockMvc mockMvc;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void getProspects_shouldReturnNearbyUsersExcludingAlreadySwiped() throws Exception {
        // Given: Alice and Bob in NYC
        UserId alice = UserId.generate();
        User aliceUser = createUser(alice, "alice", 40.7128, -74.0060, Set.of(Interest.HIKING));
        userRepository.save(aliceUser);

        UserId bob = UserId.generate();
        User bobUser = createUser(bob, "bob", 40.7306, -73.9352, Set.of(Interest.HIKING));
        userRepository.save(bobUser);

        String token = jwtTokenProvider.generateToken(alice, "alice");

        // When
        mockMvc.perform(get("/api/prospects")
                .header("Authorization", "Bearer " + token)
                .param("limit", "10")
                .param("maxDistanceKm", "100"))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prospects[0].userId").exists())
                .andExpect(jsonPath("$.prospects[0].displayName").value("bob"))
                .andExpect(jsonPath("$.prospects[0].sharedInterestCount").value(1));
    }

    @Test
    void getProspects_shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/prospects")
                .param("limit", "10")
                .param("maxDistanceKm", "100"))
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
                java.util.List.of("photo1.jpg")  // Must have at least one photo to be discoverable
        );
        return new User(id, username, profile);
    }
}
