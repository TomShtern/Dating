package com.datingapp.domain.event;

import java.time.Instant;

import com.datingapp.domain.SwipeDirection;
import com.datingapp.domain.UserId;

/**
 * Domain event published when a user swipes on another.
 */
public record UserSwipedEvent(
        UserId swiperId,
        UserId targetId,
        SwipeDirection direction,
        Instant occurredAt) implements DomainEvent {
    public UserSwipedEvent(UserId swiperId, UserId targetId, SwipeDirection direction) {
        this(swiperId, targetId, direction, Instant.now());
    }
}
