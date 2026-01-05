package com.datingapp.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UserStateTransitionsTest {

    @Nested
    class ValidTransitions {

        @Test
        void shouldTransition_fromIncompleteToActive_whenProfileCompleted() {
            User user = createUserWithIncompleteProfile();
            assertEquals(UserState.PROFILE_INCOMPLETE, user.getState());

            user.updateProfile(createCompleteProfile(user.getId()));

            assertEquals(UserState.ACTIVE, user.getState());
        }

        @Test
        void shouldTransition_fromActiveTopaused() {
            User user = createActiveUser();

            user.pause();

            assertEquals(UserState.PAUSED, user.getState());
        }

        @Test
        void shouldTransition_fromPausedToActive() {
            User user = createActiveUser();
            user.pause();

            user.activate();

            assertEquals(UserState.ACTIVE, user.getState());
        }

        @Test
        void shouldTransition_fromIncompleteToActive_viaActivate() {
            User user = createUserWithIncompleteProfile();
            // First complete the profile
            user.updateProfile(createCompleteProfile(user.getId()));
            user.pause();

            user.activate();

            assertEquals(UserState.ACTIVE, user.getState());
        }

        @Test
        void shouldTransition_fromAnyStateToBanned() {
            User activeUser = createActiveUser();
            activeUser.ban("Violation");
            assertEquals(UserState.BANNED, activeUser.getState());

            User pausedUser = createActiveUser();
            pausedUser.pause();
            pausedUser.ban("Violation");
            assertEquals(UserState.BANNED, pausedUser.getState());

            User incompleteUser = createUserWithIncompleteProfile();
            incompleteUser.ban("Violation");
            assertEquals(UserState.BANNED, incompleteUser.getState());
        }
    }

    @Nested
    class InvalidTransitions {

        @Test
        void shouldThrow_whenPausingFromIncomplete() {
            User user = createUserWithIncompleteProfile();

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> user.pause());

            assertTrue(ex.getMessage().contains("Cannot pause"),
                    "Exception message should mention pause: " + ex.getMessage());
        }

        @Test
        void shouldThrow_whenPausingFromPaused() {
            User user = createActiveUser();
            user.pause();

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> user.pause());

            assertTrue(ex.getMessage().contains("Cannot pause"));
        }

        @Test
        void shouldThrow_whenPausingFromBanned() {
            User user = createActiveUser();
            user.ban("Test");

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> user.pause());

            assertTrue(ex.getMessage().contains("Cannot pause"));
        }

        @Test
        void shouldThrow_whenActivatingFromActive() {
            User user = createActiveUser();

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> user.activate());

            assertTrue(ex.getMessage().contains("Cannot activate"));
        }

        @Test
        void shouldThrow_whenActivatingFromBanned() {
            User user = createActiveUser();
            user.ban("Test");

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> user.activate());

            assertTrue(ex.getMessage().contains("Cannot activate"));
        }

        @Test
        void shouldThrow_whenUpdatingProfileWhileBanned() {
            User user = createActiveUser();
            user.ban("Violation");

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> user.updateProfile(createCompleteProfile(user.getId())));

            assertTrue(ex.getMessage().contains("Cannot update profile"));
        }
    }

    @Nested
    class StatePermissions {

        @Test
        void registeredState_shouldHaveCorrectPermissions() {
            assertFalse(UserState.REGISTERED.canSwipe());
            assertFalse(UserState.REGISTERED.canMessage());
            assertTrue(UserState.REGISTERED.canUpdateProfile());
            assertFalse(UserState.REGISTERED.canBeDiscovered());
        }

        @Test
        void incompleteState_shouldHaveCorrectPermissions() {
            assertFalse(UserState.PROFILE_INCOMPLETE.canSwipe());
            assertFalse(UserState.PROFILE_INCOMPLETE.canMessage());
            assertTrue(UserState.PROFILE_INCOMPLETE.canUpdateProfile());
            assertFalse(UserState.PROFILE_INCOMPLETE.canBeDiscovered());
        }

        @Test
        void activeState_shouldHaveAllPermissions() {
            assertTrue(UserState.ACTIVE.canSwipe());
            assertTrue(UserState.ACTIVE.canMessage());
            assertTrue(UserState.ACTIVE.canUpdateProfile());
            assertTrue(UserState.ACTIVE.canBeDiscovered());
        }

        @Test
        void pausedState_shouldOnlyAllowProfileUpdate() {
            assertFalse(UserState.PAUSED.canSwipe());
            assertFalse(UserState.PAUSED.canMessage());
            assertTrue(UserState.PAUSED.canUpdateProfile());
            assertFalse(UserState.PAUSED.canBeDiscovered());
        }

        @Test
        void bannedState_shouldHaveNoPermissions() {
            assertFalse(UserState.BANNED.canSwipe());
            assertFalse(UserState.BANNED.canMessage());
            assertFalse(UserState.BANNED.canUpdateProfile());
            assertFalse(UserState.BANNED.canBeDiscovered());
        }

        @Test
        void userDelegatesPermissionsToState() {
            User activeUser = createActiveUser();
            assertTrue(activeUser.canSwipe());
            assertTrue(activeUser.canMessage());
            assertTrue(activeUser.canBeDiscovered());

            activeUser.pause();
            assertFalse(activeUser.canSwipe());
            assertFalse(activeUser.canMessage());
            assertFalse(activeUser.canBeDiscovered());
        }
    }

    @Nested
    class UpdatedAtTracking {

        @Test
        void shouldUpdateTimestamp_onStateTransition() throws InterruptedException {
            User user = createActiveUser();
            var initialTime = user.getUpdatedAt();

            Thread.sleep(10); // Ensure time passes
            user.pause();

            assertTrue(user.getUpdatedAt().isAfter(initialTime),
                    "UpdatedAt should be after initial time after pause");
        }

        @Test
        void shouldUpdateTimestamp_onProfileUpdate() throws InterruptedException {
            User user = createActiveUser();
            var initialTime = user.getUpdatedAt();

            Thread.sleep(10);
            user.updateProfile(createCompleteProfile(user.getId()));

            assertTrue(user.getUpdatedAt().isAfter(initialTime),
                    "UpdatedAt should be after initial time after profile update");
        }

        @Test
        void shouldUpdateTimestamp_onBan() throws InterruptedException {
            User user = createActiveUser();
            var initialTime = user.getUpdatedAt();

            Thread.sleep(10);
            user.ban("Test");

            assertTrue(user.getUpdatedAt().isAfter(initialTime),
                    "UpdatedAt should be after initial time after ban");
        }
    }

    private User createUserWithIncompleteProfile() {
        UserId id = UserId.generate();
        Profile incomplete = new Profile(id, null, null, null, null, null, null, null);
        return new User(id, "testuser_" + id.value().toString().substring(0, 8), incomplete);
    }

    private User createActiveUser() {
        UserId id = UserId.generate();
        Profile complete = createCompleteProfile(id);
        return new User(id, "testuser_" + id.value().toString().substring(0, 8), complete);
    }

    private Profile createCompleteProfile(UserId id) {
        return new Profile(
                id, "Alice", "Bio",
                LocalDate.now().minusYears(25),
                Collections.emptySet(), null,
                new Location(40.7, -74.0),
                List.of("photo.jpg"));
    }
}
