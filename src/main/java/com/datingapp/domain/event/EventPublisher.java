package com.datingapp.domain.event;

/**
 * Domain port for publishing domain events.
 */
public interface EventPublisher {
    void publish(DomainEvent event);
}
