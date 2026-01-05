package com.datingapp.domain;

/**
 * Enum representing the state of a user and their associated permissions.
 */
public enum UserState {
    REGISTERED(false, false, true, false),
    PROFILE_INCOMPLETE(false, false, true, false),
    ACTIVE(true, true, true, true),
    PAUSED(false, false, true, false),
    BANNED(false, false, false, false);

    private final boolean canSwipe;
    private final boolean canMessage;
    private final boolean canUpdateProfile;
    private final boolean canBeDiscovered;

    UserState(boolean canSwipe, boolean canMessage,
            boolean canUpdateProfile, boolean canBeDiscovered) {
        this.canSwipe = canSwipe;
        this.canMessage = canMessage;
        this.canUpdateProfile = canUpdateProfile;
        this.canBeDiscovered = canBeDiscovered;
    }

    public boolean canSwipe() {
        return canSwipe;
    }

    public boolean canMessage() {
        return canMessage;
    }

    public boolean canUpdateProfile() {
        return canUpdateProfile;
    }

    public boolean canBeDiscovered() {
        return canBeDiscovered;
    }
}
