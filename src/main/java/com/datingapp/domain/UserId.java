package com.datingapp.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object wrapping a UUID for user identification.
 */
public record UserId(UUID value) {
    public UserId {
        Objects.requireNonNull(value, "UserId cannot be null");
    }

    public static UserId generate() {
        return new UserId(UUID.randomUUID());
    }

    public static UserId of(UUID value) {
        return new UserId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
