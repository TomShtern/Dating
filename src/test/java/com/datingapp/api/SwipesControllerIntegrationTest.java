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
import com.datingapp.domain.SwipeDirection;
import com.datingapp.domain.User;
import com.datingapp.domain.UserId;
import com.datingapp.infrastructure.security.JwtTokenProvider;
import com.datingapp.domain.repository.UserRepository;
import com.datingapp.api.dto.SwipeRequestDto;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
class SwipesControllerIntegrationTest extends IntegrationTestBase {
    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void postSwipe_shouldRecordLikeWithoutMatch() throws Exception {
        // Given
        UserId alice = UserId.generate();
        User aliceUser = createUser(alice, "alice", 40.7128, -74.0060, Set.of(Interest.HIKING));
        userRepository.save(aliceUser);

        UserId bob = UserId.generate();
        User bobUser = createUser(bob, "bob", 40.7306, -73.9352, Set.of(Interest.HIKING));
        userRepository.save(bobUser);

        String token = jwtTokenProvider.generateToken(alice, "alice");
        String requestBody = objectMapper.writeValueAsString(
            new SwipeRequestDto(bob, SwipeDirection.LIKE)
        );

        // When
        mockMvc.perform(post("/api/swipes")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content(requestBody))
                // Then
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.match").doesNotExist()); // No match yet
    }

    @Test
    void postSwipe_shouldCreateMatchOnMutualLike() throws Exception {
        // Given
        UserId alice = UserId.generate();
        User aliceUser = createUser(alice, "alice", 40.7128, -74.0060, Set.of(Interest.HIKING));
        userRepository.save(aliceUser);

        UserId bob = UserId.generate();
        User bobUser = createUser(bob, "bob", 40.7306, -73.9352, Set.of(Interest.HIKING));
        userRepository.save(bobUser);

        // Alice likes Bob
        String aliceToken = jwtTokenProvider.generateToken(alice, "alice");
        String requestBody = objectMapper.writeValueAsString(
            new SwipeRequestDto(bob, SwipeDirection.LIKE)
        );
        mockMvc.perform(post("/api/swipes")
                .header("Authorization", "Bearer " + aliceToken)
                .contentType("application/json")
                .content(requestBody))
                .andExpect(status().isCreated());

        // When: Bob likes Alice back
        String bobToken = jwtTokenProvider.generateToken(bob, "bob");
        String bobRequestBody = objectMapper.writeValueAsString(
            new SwipeRequestDto(alice, SwipeDirection.LIKE)
        );

        // Then: Match should be created
        mockMvc.perform(post("/api/swipes")
                .header("Authorization", "Bearer " + bobToken)
                .contentType("application/json")
                .content(bobRequestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.match").exists())
                .andExpect(jsonPath("$.match.matchedWithUserId").exists());
    }

    @Test
    void postSwipe_shouldReturn400WhenSwipingSelf() throws Exception {
        // Given
        UserId alice = UserId.generate();
        User aliceUser = createUser(alice, "alice", 40.7128, -74.0060, Set.of(Interest.HIKING));
        userRepository.save(aliceUser);

        String token = jwtTokenProvider.generateToken(alice, "alice");
        String requestBody = objectMapper.writeValueAsString(
            new SwipeRequestDto(alice, SwipeDirection.LIKE)
        );

        // When
        mockMvc.perform(post("/api/swipes")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content(requestBody))
                // Then
                .andExpect(status().isBadRequest());
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
