package com.datingapp.domain;

import java.time.LocalDate;
import java.time.Period;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Value object record representing a user's dating profile.
 * Managed within the User aggregate.
 */
public record Profile(
        UserId userId,
        String displayName,
        String bio,
        LocalDate birthDate,
        Set<Interest> interests,
        Preferences preferences,
        Location location,
        List<String> photoUrls) {
    public Profile {
        interests = interests != null ? Set.copyOf(interests) : Collections.emptySet();
        photoUrls = photoUrls != null ? List.copyOf(photoUrls) : Collections.emptyList();
        if (photoUrls.size() > 2) {
            throw new IllegalArgumentException("Maximum 2 photos allowed");
        }
    }

    public boolean isComplete() {
        return displayName != null && !displayName.isBlank()
                && birthDate != null
                && location != null
                && !photoUrls.isEmpty();
    }

    public int age() {
        if (birthDate == null)
            return 0;
        return Period.between(birthDate, LocalDate.now()).getYears();
    }
}
