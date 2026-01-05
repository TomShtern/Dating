package com.datingapp.infrastructure.persistence.inmemory;

import com.datingapp.domain.Match;
import com.datingapp.domain.MatchId;
import com.datingapp.domain.UserId;
import com.datingapp.domain.repository.MatchRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of MatchRepository.
 */
public class InMemoryMatchRepository implements MatchRepository {
    private final Map<MatchId, Match> storage = new ConcurrentHashMap<>();

    @Override
    public Match saveIfNotExists(Match match) {
        return storage.putIfAbsent(match.getId(), match) == null ? match : storage.get(match.getId());
    }

    @Override
    public Optional<Match> findById(MatchId id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Match> findByUser(UserId userId) {
        return storage.values().stream()
                .filter(m -> m.involves(userId))
                .collect(Collectors.toList());
    }
}
