package com.datingapp.domain;

import java.util.List;
import java.util.Set;

/**
 * Value object record representing a potential match candidate shown to a user.
 * Computed view, read-only.
 */
public record Prospect(
        UserId userId,
        String displayName,
        int age,
        String bio,
        List<String> photoUrls,
        Distance distance,
        Set<Interest> sharedInterests,
        double score) {
    public Prospect {
        photoUrls = List.copyOf(photoUrls);
        sharedInterests = Set.copyOf(sharedInterests);
    }
}
