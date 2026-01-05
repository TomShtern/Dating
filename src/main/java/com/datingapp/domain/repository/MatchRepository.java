package com.datingapp.domain.repository;

import java.util.List;
import java.util.Optional;

import com.datingapp.domain.Match;
import com.datingapp.domain.MatchId;
import com.datingapp.domain.UserId;

/**
 * Domain port for Match persistence.
 */
public interface MatchRepository {
    Match saveIfNotExists(Match match);

    Optional<Match> findById(MatchId id);

    List<Match> findByUser(UserId userId);
}
