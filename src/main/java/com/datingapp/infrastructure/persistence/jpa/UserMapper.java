package com.datingapp.infrastructure.persistence.jpa;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.datingapp.domain.AgeRange;
import com.datingapp.domain.Distance;
import com.datingapp.domain.Interest;
import com.datingapp.domain.Location;
import com.datingapp.domain.Preferences;
import com.datingapp.domain.Profile;
import com.datingapp.domain.User;
import com.datingapp.domain.UserId;

/**
 * Mapper between UserEntity (JPA) and User (Domain).
 */
public final class UserMapper {

    private UserMapper() {
    }

    public static User toDomain(UserEntity entity) {
        Profile profile = buildProfile(entity);
        return new User(
                UserId.of(entity.getId()),
                entity.getUsername(),
                profile);
    }

    public static UserEntity toEntity(User user, String passwordHash) {
        UserEntity entity = new UserEntity();
        entity.setId(user.getId().value());
        entity.setUsername(user.getUsername());
        entity.setState(user.getState());
        entity.setCreatedAt(user.getCreatedAt());
        entity.setUpdatedAt(user.getUpdatedAt());

        if (passwordHash != null) {
            entity.setPasswordHash(passwordHash);
        }

        Profile profile = user.getProfile();
        if (profile != null) {
            entity.setDisplayName(profile.displayName());
            entity.setBio(profile.bio());
            entity.setBirthDate(profile.birthDate());

            if (profile.location() != null) {
                entity.setLatitude(profile.location().lat());
                entity.setLongitude(profile.location().lon());
            }

            if (profile.photoUrls() != null) {
                entity.setPhotoUrls(String.join(",", profile.photoUrls()));
            }

            if (profile.interests() != null) {
                String interestsStr = profile.interests().stream()
                        .map(Interest::name)
                        .collect(Collectors.joining(","));
                entity.setInterests(interestsStr);
            }

            Preferences prefs = profile.preferences();
            if (prefs != null) {
                if (prefs.interestedIn() != null) {
                    entity.setInterestedIn(String.join(",", prefs.interestedIn()));
                }
                if (prefs.ageRange() != null) {
                    entity.setAgeRangeMin(prefs.ageRange().min());
                    entity.setAgeRangeMax(prefs.ageRange().max());
                }
                if (prefs.maxDistance() != null) {
                    entity.setMaxDistanceKm(prefs.maxDistance().kilometers());
                }
            }
        }

        return entity;
    }

    private static Profile buildProfile(UserEntity entity) {
        Location location = null;
        if (entity.getLatitude() != null && entity.getLongitude() != null) {
            location = new Location(entity.getLatitude(), entity.getLongitude());
        }

        List<String> photoUrls = entity.getPhotoUrls() != null && !entity.getPhotoUrls().isBlank()
                ? Arrays.asList(entity.getPhotoUrls().split(","))
                : Collections.emptyList();

        Set<Interest> interests = Collections.emptySet();
        if (entity.getInterests() != null && !entity.getInterests().isBlank()) {
            interests = Arrays.stream(entity.getInterests().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Interest::valueOf)
                    .collect(Collectors.toSet());
        }

        Set<String> interestedIn = Collections.emptySet();
        if (entity.getInterestedIn() != null && !entity.getInterestedIn().isBlank()) {
            interestedIn = new HashSet<>(Arrays.asList(entity.getInterestedIn().split(",")));
        }

        AgeRange ageRange = null;
        if (entity.getAgeRangeMin() != null && entity.getAgeRangeMax() != null) {
            ageRange = new AgeRange(entity.getAgeRangeMin(), entity.getAgeRangeMax());
        }

        Distance maxDistance = entity.getMaxDistanceKm() != null
                ? Distance.ofKilometers(entity.getMaxDistanceKm())
                : null;

        Preferences preferences = new Preferences(interestedIn, ageRange, maxDistance);

        return new Profile(
                UserId.of(entity.getId()),
                entity.getDisplayName(),
                entity.getBio(),
                entity.getBirthDate(),
                interests,
                preferences,
                location,
                photoUrls);
    }
}
