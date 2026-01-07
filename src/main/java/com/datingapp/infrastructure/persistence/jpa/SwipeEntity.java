package com.datingapp.infrastructure.persistence.jpa;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.datingapp.domain.SwipeDirection;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA entity for persisting Swipe aggregates.
 * Maps domain Swipe to relational table.
 */
@Entity
@Table(name = "swipes")
public class SwipeEntity {

    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(nullable = false, columnDefinition = "UUID")
    private UUID swiperId;

    @Column(nullable = false, columnDefinition = "UUID")
    private UUID targetId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SwipeDirection direction;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    // Default constructor for JPA
    public SwipeEntity() {}

    public SwipeEntity(UUID id, UUID swiperId, UUID targetId, SwipeDirection direction, Instant createdAt) {
        this.id = id;
        this.swiperId = swiperId;
        this.targetId = targetId;
        this.direction = direction;
        this.createdAt = createdAt;
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getSwiperId() { return swiperId; }
    public void setSwiperId(UUID swiperId) { this.swiperId = swiperId; }

    public UUID getTargetId() { return targetId; }
    public void setTargetId(UUID targetId) { this.targetId = targetId; }

    public SwipeDirection getDirection() { return direction; }
    public void setDirection(SwipeDirection direction) { this.direction = direction; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
