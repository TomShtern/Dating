package com.datingapp.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Aggregate root representing a User.
 * Pure Java, framework-agnostic.
 */
public class User {
    private final UserId id;
    private Profile profile;
    private UserState state;
    private final Instant createdAt;
    private Instant updatedAt;

    public User(UserId id, Profile profile) {
        this.id = Objects.requireNonNull(id);
        this.profile = profile;
        this.state = profile != null && profile.isComplete()
                ? UserState.ACTIVE
                : UserState.PROFILE_INCOMPLETE;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    /**
     * Factory method for creating a new user with a generated ID.
     */
    public static User create(Profile profile) {
        return new User(UserId.generate(), profile);
    }

    public UserId getId() {
        return id;
    }

    public Profile getProfile() {
        return profile;
    }

    public UserState getState() {
        return state;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void updateProfile(Profile newProfile) {
        if (!state.canUpdateProfile()) {
            throw new IllegalStateException("Cannot update profile in state: " + state);
        }
        this.profile = newProfile;
        this.updatedAt = Instant.now();

        // Auto-activate if complete and in incomplete state
        if (this.state == UserState.PROFILE_INCOMPLETE && newProfile.isComplete()) {
            this.state = UserState.ACTIVE;
        }
    }

    public void activate() {
        if (state != UserState.PROFILE_INCOMPLETE && state != UserState.PAUSED) {
            throw new IllegalStateException("Cannot activate from " + state);
        }
        this.state = UserState.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void pause() {
        if (state != UserState.ACTIVE) {
            throw new IllegalStateException("Cannot pause from " + state);
        }
        this.state = UserState.PAUSED;
        this.updatedAt = Instant.now();
    }

    public void ban(String reason) {
        this.state = UserState.BANNED;
        this.updatedAt = Instant.now();
    }

    public boolean canSwipe() {
        return state.canSwipe();
    }

    public boolean canMessage() {
        return state.canMessage();
    }

    public boolean canBeDiscovered() {
        return state.canBeDiscovered();
    }
}
