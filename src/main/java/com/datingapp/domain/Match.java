package com.datingapp.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Aggregate root representing a confirmed mutual match between two users.
 * Uses a canonical MatchId for idempotency.
 */
public class Match {
    private final MatchId id;
    private final UserId userA; // Smaller ID string
    private final UserId userB; // Larger ID string
    private final Instant createdAt;
    private final boolean newlyCreated;

    private Match(MatchId id, UserId userA, UserId userB,
            Instant createdAt, boolean newlyCreated) {
        this.id = id;
        this.userA = userA;
        this.userB = userB;
        this.createdAt = createdAt;
        this.newlyCreated = newlyCreated;
    }

    /**
     * Factory method for creating a new match.
     * Enforces canonical ordering of user IDs.
     */
    public static Match create(UserId a, UserId b) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(b);

        if (a.equals(b)) {
            throw new IllegalArgumentException("Cannot match user with themselves");
        }

        // Canonical ordering for idempotency
        UserId first;
        UserId second;
        if (a.value().toString().compareTo(b.value().toString()) < 0) {
            first = a;
            second = b;
        } else {
            first = b;
            second = a;
        }

        return new Match(
                MatchId.canonical(first, second),
                first,
                second,
                Instant.now(),
                true);
    }

    /**
     * Reconstitutes a match from persistence.
     */
    public static Match reconstitute(MatchId id, UserId userA, UserId userB, Instant createdAt) {
        return new Match(id, userA, userB, createdAt, false);
    }

    public MatchId getId() {
        return id;
    }

    public UserId getUserA() {
        return userA;
    }

    public UserId getUserB() {
        return userB;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isNewlyCreated() {
        return newlyCreated;
    }

    public boolean involves(UserId userId) {
        return userA.equals(userId) || userB.equals(userId);
    }

    public UserId otherUser(UserId userId) {
        if (userA.equals(userId))
            return userB;
        if (userB.equals(userId))
            return userA;
        throw new IllegalArgumentException("User " + userId + " is not part of this match");
    }
}
