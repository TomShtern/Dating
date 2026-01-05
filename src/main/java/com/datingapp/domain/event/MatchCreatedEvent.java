package com.datingapp.domain.event;

import java.time.Instant;

import com.datingapp.domain.MatchId;
import com.datingapp.domain.UserId;

/**
 * Domain event published when a mutual match is created.
 */
public record MatchCreatedEvent(
        MatchId matchId,
        UserId userA,
        UserId userB,
        Instant occurredAt) implements DomainEvent {
    public MatchCreatedEvent(MatchId matchId, UserId userA, UserId userB) {
        this(matchId, userA, userB, Instant.now());
    }
}
