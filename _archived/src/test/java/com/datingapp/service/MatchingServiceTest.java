package com.datingapp.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.datingapp.model.InteractionResult;
import com.datingapp.model.Match;
import com.datingapp.model.User;
import com.datingapp.model.UserInteraction;
import com.datingapp.repository.MatchRepository;
import com.datingapp.repository.UserInteractionRepository;
import com.datingapp.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class MatchingServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserInteractionRepository interactionRepository;

    @Mock
    private MatchRepository matchRepository;

    private MatchingService matchingService;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        matchingService = new MatchingService(userRepository, interactionRepository, matchRepository);

        user1 = User.builder().id(UUID.randomUUID()).username("user1").displayName("User One").build();
        user2 = User.builder().id(UUID.randomUUID()).username("user2").displayName("User Two").build();
    }

    @Test
    void likeUser_shouldReturnLiked_whenNoMutualLike() {
        when(interactionRepository.findByFromUserAndToUser(user1, user2)).thenReturn(Optional.empty());
        when(interactionRepository.hasLiked(user2, user1)).thenReturn(false);
        when(interactionRepository.save(any(UserInteraction.class))).thenAnswer(inv -> inv.getArgument(0));

        InteractionResult result = matchingService.likeUser(user1, user2);

        assertEquals(InteractionResult.LIKED, result);
        verify(interactionRepository).save(any(UserInteraction.class));
        verify(matchRepository, never()).save(any());
    }

    @Test
    void likeUser_shouldReturnMatched_whenMutualLike() {
        when(interactionRepository.findByFromUserAndToUser(user1, user2)).thenReturn(Optional.empty());
        when(interactionRepository.hasLiked(user2, user1)).thenReturn(true);
        when(interactionRepository.save(any(UserInteraction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(matchRepository.save(any(Match.class))).thenAnswer(inv -> inv.getArgument(0));

        InteractionResult result = matchingService.likeUser(user1, user2);

        assertEquals(InteractionResult.MATCHED, result);
        verify(matchRepository).save(any(Match.class));
    }

    @Test
    void likeUser_shouldReturnAlreadyInteracted_whenPreviouslyInteracted() {
        UserInteraction existingInteraction = UserInteraction.builder()
                .fromUser(user1).toUser(user2).build();
        when(interactionRepository.findByFromUserAndToUser(user1, user2))
                .thenReturn(Optional.of(existingInteraction));

        InteractionResult result = matchingService.likeUser(user1, user2);

        assertEquals(InteractionResult.ALREADY_INTERACTED, result);
        verify(interactionRepository, never()).save(any());
    }

    @Test
    void passUser_shouldCreatePassInteraction() {
        when(interactionRepository.findByFromUserAndToUser(user1, user2)).thenReturn(Optional.empty());
        when(interactionRepository.save(any(UserInteraction.class))).thenAnswer(inv -> inv.getArgument(0));

        matchingService.passUser(user1, user2);

        verify(interactionRepository).save(any(UserInteraction.class));
    }

    @Test
    void passUser_shouldDoNothing_whenAlreadyInteracted() {
        UserInteraction existingInteraction = UserInteraction.builder()
                .fromUser(user1).toUser(user2).build();
        when(interactionRepository.findByFromUserAndToUser(user1, user2))
                .thenReturn(Optional.of(existingInteraction));

        matchingService.passUser(user1, user2);

        verify(interactionRepository, never()).save(any());
    }

    @Test
    void getMatchesForUser_shouldReturnMatches() {
        Match match = Match.builder().user1(user1).user2(user2).build();
        when(matchRepository.findMatchesForUser(user1)).thenReturn(List.of(match));

        List<Match> matches = matchingService.getMatchesForUser(user1);

        assertEquals(1, matches.size());
        assertEquals(user2, matches.get(0).getUser2());
    }

    @Test
    void areMatched_shouldReturnTrue_whenMatched() {
        when(matchRepository.areMatched(user1, user2)).thenReturn(true);

        assertTrue(matchingService.areMatched(user1, user2));
    }

    @Test
    void areMatched_shouldReturnFalse_whenNotMatched() {
        when(matchRepository.areMatched(user1, user2)).thenReturn(false);

        assertFalse(matchingService.areMatched(user1, user2));
    }

    @Test
    void getPotentialMatches_shouldExcludeInteractedAndMatchedUsers() {
        User user3 = User.builder().id(UUID.randomUUID()).username("user3").displayName("User Three").build();

        when(interactionRepository.findAllInteractedUserIds(user1)).thenReturn(List.of(user2.getId()));
        when(matchRepository.findMatchedUserIds(user1)).thenReturn(Collections.emptyList());
        when(userRepository.findAll()).thenReturn(List.of(user1, user2, user3));

        List<User> potentialMatches = matchingService.getPotentialMatches(user1, 10);

        assertEquals(1, potentialMatches.size());
        assertEquals("user3", potentialMatches.get(0).getUsername());
    }
}
