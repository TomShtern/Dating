package com.datingapp.infrastructure.persistence.jpa;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA entity for persisting Match aggregates.
 * Maps domain Match to relational table.
 *
 * ★ Insight ─────────────────────────────────────
 * Stores canonical user IDs (userA, userB) to support idempotency:
 * Match(user1, user2) and Match(user2, user1) have the same canonical MatchId.
 * This is enforced in the domain Match aggregate's canonicalOrdering logic.
 * ─────────────────────────────────────────────────
 */
@Entity
@Table(name = "matches")
public class MatchEntity {

    @Id
    @Column(length = 73) // "uuid_uuid" format (36 + 36 + 1 underscore)
    private String id;

    @Column(nullable = false, columnDefinition = "UUID")
    private UUID userAId;

    @Column(nullable = false, columnDefinition = "UUID")
    private UUID userBId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    // Default constructor for JPA
    public MatchEntity() {}

    public MatchEntity(String id, UUID userAId, UUID userBId, Instant createdAt) {
        this.id = id;
        this.userAId = userAId;
        this.userBId = userBId;
        this.createdAt = createdAt;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public UUID getUserAId() { return userAId; }
    public void setUserAId(UUID userAId) { this.userAId = userAId; }

    public UUID getUserBId() { return userBId; }
    public void setUserBId(UUID userBId) { this.userBId = userBId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
