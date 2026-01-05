package com.datingapp.domain.event;

import java.time.Instant;

/**
 * Base sealed interface for all domain events.
 */
public sealed interface DomainEvent permits UserSwipedEvent, MatchCreatedEvent {
    Instant occurredAt();
}
