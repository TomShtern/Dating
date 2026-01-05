package com.datingapp.domain;

import java.util.Objects;

/**
 * Canonical match ID derived from two UserIds.
 * Same pair always produces same MatchId regardless of order.
 */
public record MatchId(String value) {
    public MatchId {
        Objects.requireNonNull(value, "MatchId cannot be null");
    }

    /**
     * Creates canonical MatchId by sorting UUIDs lexicographically.
     */
    public static MatchId canonical(UserId a, UserId b) {
        String aStr = a.value().toString();
        String bStr = b.value().toString();
        String sorted = aStr.compareTo(bStr) < 0
                ? aStr + "_" + bStr
                : bStr + "_" + aStr;
        return new MatchId(sorted);
    }
}
