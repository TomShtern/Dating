package com.datingapp.domain.matching;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.datingapp.domain.Distance;
import com.datingapp.domain.Interest;
import com.datingapp.domain.Match;
import com.datingapp.domain.MatchId;
import com.datingapp.domain.Prospect;
import com.datingapp.domain.Swipe;
import com.datingapp.domain.SwipeDirection;
import com.datingapp.domain.User;
import com.datingapp.domain.UserId;
import com.datingapp.domain.event.EventPublisher;
import com.datingapp.domain.event.MatchCreatedEvent;
import com.datingapp.domain.repository.MatchRepository;
import com.datingapp.domain.repository.SwipeRepository;
import com.datingapp.domain.repository.UserRepository;

/**
 * Domain service for managing matching logic and swipe processing.
 */
public class MatchingService {
    private final MatchScorer scorer;
    private final UserRepository userRepository;
    private final SwipeRepository swipeRepository;
    private final MatchRepository matchRepository;
    private final EventPublisher eventPublisher;

    public MatchingService(MatchScorer scorer,
            UserRepository userRepository,
            SwipeRepository swipeRepository,
            MatchRepository matchRepository,
            EventPublisher eventPublisher) {
        this.scorer = scorer;
        this.userRepository = userRepository;
        this.swipeRepository = swipeRepository;
        this.matchRepository = matchRepository;
        this.eventPublisher = eventPublisher;
    }

    public List<Prospect> findProspects(User requester, Distance radius, int limit, Set<UserId> excludedIds) {
        return userRepository.findDiscoverableInRadius(
                requester.getProfile().location(),
                radius,
                limit * 2 // Fetch more to allow for filtering
        ).stream()
                .filter(u -> !u.getId().equals(requester.getId()))
                .filter(u -> !excludedIds.contains(u.getId()))
                .map(candidate -> toProspect(candidate, requester))
                .sorted((p1, p2) -> Double.compare(p2.score(), p1.score()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public Optional<Match> processSwipe(UserId swiper, UserId target, SwipeDirection direction) {
        // 1. Record swipe
        Swipe swipe = swipeRepository.saveIfNotExists(
                Swipe.create(swiper, target, direction));

        if (!swipe.isLike()) {
            return Optional.empty();
        }

        // 2. Check for mutual interest
        Optional<Swipe> reverseSwipe = swipeRepository.findByPair(target, swiper);

        if (reverseSwipe.isEmpty() || !reverseSwipe.get().isLike()) {
            return Optional.empty();
        }

        // 3. Create match (idempotent check)
        MatchId matchId = MatchId.canonical(swiper, target);
        Optional<Match> existingMatch = matchRepository.findById(matchId);
        if (existingMatch.isPresent()) {
            return existingMatch;
        }

        Match match = Match.create(swiper, target);
        matchRepository.saveIfNotExists(match);

        // 4. Publish event for the new match
        eventPublisher.publish(new MatchCreatedEvent(match.getId(), swiper, target));

        return Optional.of(match);
    }

    private Prospect toProspect(User candidate, User requester) {
        double score = scorer.score(candidate, requester);
        // Distance is computed again here, could be optimized
        Distance dist = requester.getProfile().location().distanceTo(candidate.getProfile().location());

        Set<Interest> shared = candidate.getProfile().interests().stream()
                .filter(requester.getProfile().interests()::contains)
                .collect(Collectors.toSet());

        return new Prospect(
                candidate.getId(),
                candidate.getProfile().displayName(),
                candidate.getProfile().age(),
                candidate.getProfile().bio(),
                candidate.getProfile().photoUrls(),
                dist,
                shared,
                score);
    }
}
