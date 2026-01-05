package com.datingapp.domain;

/**
 * Enum representing the direction of a swipe.
 */
public enum SwipeDirection {
    LIKE,
    DISLIKE,
    SUPER_LIKE;

    public boolean isLike() {
        return this == LIKE || this == SUPER_LIKE;
    }
}
