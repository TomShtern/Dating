package com.datingapp.domain.matching;

import com.datingapp.domain.User;

/**
 * Strategy interface for matching two users.
 */
public interface MatchStrategy {
    /**
     * Compute a score for how well two users match.
     * 
     * @return score between 0.0 (poor match) and 1.0 (excellent match)
     */
    double score(User candidate, User requester);

    /**
     * Human-readable name for logging and debugging.
     */
    String name();
}
