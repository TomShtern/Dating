package com.datingapp.domain;

import java.util.Collections;
import java.util.Set;

/**
 * Value object record for matching preferences.
 */
public record Preferences(
        Set<String> interestedIn, // genders
        AgeRange ageRange,
        Distance maxDistance) {
    public Preferences {
        interestedIn = interestedIn != null ? Set.copyOf(interestedIn) : Collections.emptySet();
    }

    public boolean matches(Profile profile) {
        if (ageRange != null && !ageRange.contains(profile.age())) {
            return false;
        }
        // Location check is usually done in the repository/service layer via distanceTo
        return true;
    }
}
