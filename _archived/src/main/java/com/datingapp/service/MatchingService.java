package com.datingapp.service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.datingapp.model.InteractionResult;
import com.datingapp.model.InteractionType;
import com.datingapp.model.Match;
import com.datingapp.model.User;
import com.datingapp.model.UserInteraction;
import com.datingapp.repository.MatchRepository;
import com.datingapp.repository.UserInteractionRepository;
import com.datingapp.repository.UserRepository;

@Service
@Transactional
public class MatchingService {

    private final UserRepository userRepository;
    private final UserInteractionRepository interactionRepository;
    private final MatchRepository matchRepository;

    public MatchingService(UserRepository userRepository,
            UserInteractionRepository interactionRepository,
            MatchRepository matchRepository) {
        this.userRepository = userRepository;
        this.interactionRepository = interactionRepository;
        this.matchRepository = matchRepository;
    }

    @Transactional(readOnly = true)
    public List<User> getPotentialMatches(User currentUser, int limit) {
        List<UUID> interactedIds = interactionRepository.findAllInteractedUserIds(currentUser);
        List<UUID> matchedIds = matchRepository.findMatchedUserIds(currentUser);

        Set<UUID> excludeIds = new HashSet<>();
        excludeIds.add(currentUser.getId());
        excludeIds.addAll(interactedIds);
        excludeIds.addAll(matchedIds);

        List<User> allUsers = userRepository.findAll();
        return allUsers.stream()
                .filter(u -> !excludeIds.contains(u.getId()))
                .limit(limit)
                .toList();
    }

    public InteractionResult likeUser(User fromUser, User toUser) {
        if (interactionRepository.findByFromUserAndToUser(fromUser, toUser).isPresent()) {
            return InteractionResult.ALREADY_INTERACTED;
        }

        UserInteraction interaction = UserInteraction.builder()
                .fromUser(fromUser)
                .toUser(toUser)
                .interactionType(InteractionType.LIKE)
                .build();
        interactionRepository.save(interaction);

        boolean mutualLike = interactionRepository.hasLiked(toUser, fromUser);
        if (mutualLike) {
            Match match = Match.builder()
                    .user1(fromUser)
                    .user2(toUser)
                    .build();
            matchRepository.save(match);
            return InteractionResult.MATCHED;
        }

        return InteractionResult.LIKED;
    }

    public void passUser(User fromUser, User toUser) {
        if (interactionRepository.findByFromUserAndToUser(fromUser, toUser).isPresent()) {
            return;
        }

        UserInteraction interaction = UserInteraction.builder()
                .fromUser(fromUser)
                .toUser(toUser)
                .interactionType(InteractionType.PASS)
                .build();
        interactionRepository.save(interaction);
    }

    @Transactional(readOnly = true)
    public List<Match> getMatchesForUser(User user) {
        return matchRepository.findMatchesForUser(user);
    }

    @Transactional(readOnly = true)
    public boolean areMatched(User user1, User user2) {
        return matchRepository.areMatched(user1, user2);
    }

    @Transactional(readOnly = true)
    public Optional<Match> getMatchById(UUID matchId) {
        return matchRepository.findById(matchId);
    }
}