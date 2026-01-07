package com.datingapp.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Aggregate root representing a single swipe interaction.
 * References UserIds but is its own aggregate.
 */
public class Swipe {
    private final SwipeId id;
    private final UserId swiperId;
    private final UserId targetId;
    private final SwipeDirection direction;
    private final Instant createdAt;

    private Swipe(SwipeId id, UserId swiperId, UserId targetId,
            SwipeDirection direction, Instant createdAt) {
        this.id = id;
        this.swiperId = swiperId;
        this.targetId = targetId;
        this.direction = direction;
        this.createdAt = createdAt;
    }

    /**
     * Factory method to create a new swipe.
     * Enforces domain invariant that users cannot swipe on themselves.
     */
    public static Swipe create(UserId swiper, UserId target, SwipeDirection direction) {
        Objects.requireNonNull(swiper);
        Objects.requireNonNull(target);
        Objects.requireNonNull(direction);

        if (swiper.equals(target)) {
            throw new IllegalArgumentException("Cannot swipe on yourself");
        }

        return new Swipe(
                SwipeId.generate(),
                swiper,
                target,
                direction,
                Instant.now());
    }

    /**
     * Reconstitute a swipe from persistence.
     * Used when loading swipes from the database.
     * This method bypasses validation since the data is already persisted.
     */
    public static Swipe reconstitute(SwipeId id, UserId swiperId, UserId targetId,
            SwipeDirection direction, Instant createdAt) {
        return new Swipe(id, swiperId, targetId, direction, createdAt);
    }

    public SwipeId getId() {
        return id;
    }

    public UserId getSwiperId() {
        return swiperId;
    }

    public UserId getTargetId() {
        return targetId;
    }

    public SwipeDirection getDirection() {
        return direction;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isLike() {
        return direction.isLike();
    }
}
