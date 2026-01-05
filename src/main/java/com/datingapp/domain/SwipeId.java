package com.datingapp.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object wrapping a UUID for swipe identification.
 */
public record SwipeId(UUID value) {
    public SwipeId {
        Objects.requireNonNull(value, "SwipeId cannot be null");
    }

    public static SwipeId generate() {
        return new SwipeId(UUID.randomUUID());
    }
}
