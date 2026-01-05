package com.datingapp.domain.matching;

import java.util.List;

import com.datingapp.domain.User;

/**
 * Composes multiple MatchStrategy instances to compute an aggregate score.
 */
public class MatchScorer {
    private final List<MatchStrategy> strategies;

    public MatchScorer(List<MatchStrategy> strategies) {
        this.strategies = List.copyOf(strategies);
    }

    public double score(User candidate, User requester) {
        if (strategies.isEmpty()) {
            return 0.5; // Neutral score
        }

        double sum = 0;
        for (MatchStrategy strategy : strategies) {
            sum += strategy.score(candidate, requester);
        }
        return sum / strategies.size();
    }
}
