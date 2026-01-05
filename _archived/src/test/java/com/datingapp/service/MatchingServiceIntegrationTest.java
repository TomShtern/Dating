package com.datingapp.service;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.datingapp.model.InteractionResult;
import com.datingapp.model.Match;
import com.datingapp.model.User;
import com.datingapp.repository.MatchRepository;
import com.datingapp.repository.UserInteractionRepository;
import com.datingapp.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MatchingServiceIntegrationTest {

    @Autowired
    private MatchingService matchingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserInteractionRepository interactionRepository;

    @Autowired
    private MatchRepository matchRepository;

    private User user1;
    private User user2;
    private User user3;
    private User user4;

    @BeforeEach
    void setUp() {
        user1 = createUser("user1");
        user2 = createUser("user2");
        user3 = createUser("user3");
        user4 = createUser("user4");
    }

    private User createUser(String username) {
        User user = User.builder()
                .username(username)
                .passwordHash("hash")
                .displayName(username.toUpperCase())
                .age(25)
                .gender("Male")
                .email(username + "@test.com")
                .build();
        return userRepository.save(user);
    }

    @Test
    void likeUser_shouldSaveInteraction_whenFirstLike() {
        InteractionResult result = matchingService.likeUser(user1, user2);

        assertEquals(InteractionResult.LIKED, result);
        assertTrue(interactionRepository.hasLiked(user1, user2));
        assertFalse(interactionRepository.hasLiked(user2, user1));
        assertFalse(matchRepository.areMatched(user1, user2));
    }

    @Test
    void likeUser_shouldCreateMatch_whenMutualLike() {
        // User 2 likes User 1 first
        matchingService.likeUser(user2, user1);

        // User 1 likes User 2
        InteractionResult result = matchingService.likeUser(user1, user2);

        assertEquals(InteractionResult.MATCHED, result);
        assertTrue(matchRepository.areMatched(user1, user2));

        List<Match> matches = matchRepository.findMatchesForUser(user1);
        assertEquals(1, matches.size());
        assertEquals(user2.getId(), matches.get(0).getOtherUser(user1).getId());
    }

    @Test
    void likeUser_shouldReturnAlreadyInteracted_ifDuplicate() {
        matchingService.likeUser(user1, user2);
        InteractionResult result = matchingService.likeUser(user1, user2);

        assertEquals(InteractionResult.ALREADY_INTERACTED, result);
    }

    @Test
    void passUser_shouldSavePassInteraction() {
        matchingService.passUser(user1, user2);

        // Ensure user2 does NOT appear in potential matches for user1
        List<User> potentials = matchingService.getPotentialMatches(user1, 10);
        assertTrue(potentials.stream().noneMatch(u -> u.getUsername().equals("user2")));

        // And is NOT liked
        assertFalse(interactionRepository.hasLiked(user1, user2));
    }

    @Test
    void getPotentialMatches_shouldExcludeInteractedAndMatched() {
        // user1 likes user2
        matchingService.likeUser(user1, user2);
        // user1 passes user3
        matchingService.passUser(user1, user3);

        // user4 is fresh

        List<User> potentials = matchingService.getPotentialMatches(user1, 10);

        assertEquals(1, potentials.size());
        assertEquals("user4", potentials.get(0).getUsername());
    }

    @Test
    void getPotentialMatches_shouldExcludeMatchedUsers() {
        // Setup a match between user1 and user2 manually or via service
        matchingService.likeUser(user2, user1);
        matchingService.likeUser(user1, user2); // Matched

        List<User> potentials = matchingService.getPotentialMatches(user1, 10);

        // Should find user3 and user4
        assertEquals(2, potentials.size());
        assertTrue(potentials.stream().anyMatch(u -> u.getUsername().equals("user3")));
        assertTrue(potentials.stream().anyMatch(u -> u.getUsername().equals("user4")));
        assertFalse(potentials.stream().anyMatch(u -> u.getUsername().equals("user2")));
    }
}
