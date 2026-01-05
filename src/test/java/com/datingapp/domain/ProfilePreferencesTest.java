package com.datingapp.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ProfilePreferencesTest {

    @Nested
    class ProfileIsComplete {

        @Test
        void shouldBeComplete_whenAllRequiredFieldsPresent() {
            Profile profile = new Profile(
                    UserId.generate(),
                    "Alice",
                    "Bio here",
                    LocalDate.now().minusYears(25),
                    Set.of(Interest.HIKING),
                    null,
                    new Location(40.7, -74.0),
                    List.of("photo1.jpg"));

            assertTrue(profile.isComplete());
        }

        @Test
        void shouldBeIncomplete_whenDisplayNameNull() {
            Profile profile = new Profile(
                    UserId.generate(),
                    null, // missing
                    "Bio",
                    LocalDate.now().minusYears(25),
                    Collections.emptySet(),
                    null,
                    new Location(0, 0),
                    List.of("url"));

            assertFalse(profile.isComplete(), "Profile with null displayName should be incomplete");
        }

        @Test
        void shouldBeIncomplete_whenDisplayNameBlank() {
            Profile profile = new Profile(
                    UserId.generate(),
                    "   ", // blank
                    "Bio",
                    LocalDate.now().minusYears(25),
                    Collections.emptySet(),
                    null,
                    new Location(0, 0),
                    List.of("url"));

            assertFalse(profile.isComplete(), "Profile with blank displayName should be incomplete");
        }

        @Test
        void shouldBeIncomplete_whenBirthDateNull() {
            Profile profile = new Profile(
                    UserId.generate(),
                    "Alice",
                    "Bio",
                    null, // missing
                    Collections.emptySet(),
                    null,
                    new Location(0, 0),
                    List.of("url"));

            assertFalse(profile.isComplete(), "Profile with null birthDate should be incomplete");
        }

        @Test
        void shouldBeIncomplete_whenLocationNull() {
            Profile profile = new Profile(
                    UserId.generate(),
                    "Alice",
                    "Bio",
                    LocalDate.now().minusYears(25),
                    Collections.emptySet(),
                    null,
                    null, // missing
                    List.of("url"));

            assertFalse(profile.isComplete(), "Profile with null location should be incomplete");
        }

        @Test
        void shouldBeIncomplete_whenNoPhotos() {
            Profile profile = new Profile(
                    UserId.generate(),
                    "Alice",
                    "Bio",
                    LocalDate.now().minusYears(25),
                    Collections.emptySet(),
                    null,
                    new Location(0, 0),
                    Collections.emptyList()); // no photos

            assertFalse(profile.isComplete(), "Profile with no photos should be incomplete");
        }

        @Test
        void shouldBeComplete_withNullBioAndPreferences() {
            // Bio and preferences are optional for completeness
            Profile profile = new Profile(
                    UserId.generate(),
                    "Alice",
                    null, // bio null is OK
                    LocalDate.now().minusYears(25),
                    Collections.emptySet(),
                    null, // preferences null is OK
                    new Location(0, 0),
                    List.of("url"));

            assertTrue(profile.isComplete(), "Profile should be complete even with null bio/preferences");
        }
    }

    @Nested
    class ProfileAge {

        @Test
        void shouldCalculateCorrectAge() {
            LocalDate birthDate = LocalDate.now().minusYears(30).minusDays(1);
            Profile profile = createProfileWithBirthDate(birthDate);

            assertEquals(30, profile.age());
        }

        @Test
        void shouldReturnZero_whenBirthDateNull() {
            Profile profile = createProfileWithBirthDate(null);

            assertEquals(0, profile.age(), "Null birthDate should return age 0");
        }

        @Test
        void shouldNotCountBirthdayBeforeToday() {
            // If birthday is tomorrow, should still be previous age
            LocalDate birthDate = LocalDate.now().minusYears(25).plusDays(1);
            Profile profile = createProfileWithBirthDate(birthDate);

            assertEquals(24, profile.age(), "Birthday not yet reached should show previous age");
        }

        @Test
        void shouldCountBirthdayOnToday() {
            LocalDate birthDate = LocalDate.now().minusYears(25);
            Profile profile = createProfileWithBirthDate(birthDate);

            assertEquals(25, profile.age(), "Birthday today should show current age");
        }

        private Profile createProfileWithBirthDate(LocalDate birthDate) {
            return new Profile(
                    UserId.generate(), "Name", "Bio", birthDate,
                    Collections.emptySet(), null, new Location(0, 0), List.of("url"));
        }
    }

    @Nested
    class ProfilePhotoLimit {

        @Test
        void shouldAcceptZeroPhotos() {
            assertDoesNotThrow(() -> new Profile(
                    UserId.generate(), "Name", "Bio",
                    LocalDate.now().minusYears(20), Collections.emptySet(),
                    null, new Location(0, 0), Collections.emptyList()));
        }

        @Test
        void shouldAcceptOnePhoto() {
            assertDoesNotThrow(() -> new Profile(
                    UserId.generate(), "Name", "Bio",
                    LocalDate.now().minusYears(20), Collections.emptySet(),
                    null, new Location(0, 0), List.of("one.jpg")));
        }

        @Test
        void shouldAcceptExactlyTwoPhotos() {
            assertDoesNotThrow(() -> new Profile(
                    UserId.generate(), "Name", "Bio",
                    LocalDate.now().minusYears(20), Collections.emptySet(),
                    null, new Location(0, 0), List.of("one.jpg", "two.jpg")));
        }

        @Test
        void shouldRejectThreePhotos() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new Profile(
                            UserId.generate(), "Name", "Bio",
                            LocalDate.now().minusYears(20), Collections.emptySet(),
                            null, new Location(0, 0),
                            List.of("1.jpg", "2.jpg", "3.jpg")));

            assertEquals("Maximum 2 photos allowed", ex.getMessage());
        }

        @Test
        void shouldHandleNullPhotoList() {
            Profile profile = new Profile(
                    UserId.generate(), "Name", "Bio",
                    LocalDate.now().minusYears(20), Collections.emptySet(),
                    null, new Location(0, 0), null);

            assertNotNull(profile.photoUrls());
            assertTrue(profile.photoUrls().isEmpty());
        }
    }

    @Nested
    class PreferencesMatching {

        @Test
        void shouldMatch_whenAgeInRange() {
            Preferences prefs = new Preferences(
                    Set.of("female"),
                    AgeRange.of(20, 30),
                    Distance.ofKilometers(50));

            Profile profile = createProfileWithAge(25);

            assertTrue(prefs.matches(profile));
        }

        @Test
        void shouldMatch_atLowerBoundary() {
            Preferences prefs = new Preferences(
                    Set.of("female"),
                    AgeRange.of(20, 30),
                    null);

            Profile profile = createProfileWithAge(20);

            assertTrue(prefs.matches(profile), "Age exactly at lower bound should match");
        }

        @Test
        void shouldMatch_atUpperBoundary() {
            Preferences prefs = new Preferences(
                    Set.of("female"),
                    AgeRange.of(20, 30),
                    null);

            Profile profile = createProfileWithAge(30);

            assertTrue(prefs.matches(profile), "Age exactly at upper bound should match");
        }

        @Test
        void shouldNotMatch_whenAgeBelowRange() {
            Preferences prefs = new Preferences(
                    Set.of("female"),
                    AgeRange.of(20, 30),
                    null);

            Profile profile = createProfileWithAge(19);

            assertFalse(prefs.matches(profile), "Age below range should not match");
        }

        @Test
        void shouldNotMatch_whenAgeAboveRange() {
            Preferences prefs = new Preferences(
                    Set.of("female"),
                    AgeRange.of(20, 30),
                    null);

            Profile profile = createProfileWithAge(31);

            assertFalse(prefs.matches(profile), "Age above range should not match");
        }

        @Test
        void shouldMatch_whenAgeRangeNull() {
            Preferences prefs = new Preferences(
                    Set.of("any"),
                    null, // no age restriction
                    null);

            Profile profile = createProfileWithAge(99);

            assertTrue(prefs.matches(profile), "Null ageRange should match any age");
        }

        @Test
        void shouldHandleNullInterestedIn() {
            Preferences prefs = new Preferences(null, null, null);

            assertNotNull(prefs.interestedIn());
            assertTrue(prefs.interestedIn().isEmpty());
        }

        private Profile createProfileWithAge(int age) {
            LocalDate birthDate = LocalDate.now().minusYears(age);
            return new Profile(
                    UserId.generate(), "Name", "Bio", birthDate,
                    Collections.emptySet(), null, new Location(0, 0), List.of("url"));
        }
    }
}
