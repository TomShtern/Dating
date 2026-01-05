package com.datingapp.application;

import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import com.datingapp.domain.Distance;
import com.datingapp.domain.Interest;
import com.datingapp.domain.Location;
import com.datingapp.domain.Preferences;
import com.datingapp.domain.Profile;
import com.datingapp.domain.Swipe;
import com.datingapp.domain.SwipeDirection;
import com.datingapp.domain.User;
import com.datingapp.domain.UserId;
import com.datingapp.domain.repository.SwipeRepository;
import com.datingapp.domain.repository.UserRepository;
import com.datingapp.infrastructure.persistence.inmemory.InMemorySwipeRepository;
import com.datingapp.infrastructure.persistence.inmemory.InMemoryUserRepository;
import com.datingapp.api.dto.ProspectDto;

class ProspectsServiceTest {

    @Test
    void findProspects_shouldReturnUsersWithinDistance() {
        // Given
        UserRepository userRepository = new InMemoryUserRepository();
        SwipeRepository swipeRepository = new InMemorySwipeRepository();
        ProspectsService service = new ProspectsService(userRepository, swipeRepository);

        UserId alice = UserId.generate();
        User aliceUser = createUser(alice, "alice", 40.7128, -74.0060, Set.of(Interest.HIKING));
        userRepository.save(aliceUser);

        UserId bob = UserId.generate();
        User bobUser = createUser(bob, "bob", 40.7306, -73.9352, Set.of(Interest.HIKING));
        userRepository.save(bobUser);

        // When
        List<ProspectDto> prospects = service.findProspects(
                alice,
                Distance.ofKilometers(100),
                10,
                Set.of()
        );

        // Then
        assertEquals(1, prospects.size());
        assertEquals(bob, prospects.get(0).userId());
        assertEquals("alice", aliceUser.getUsername());
        assertEquals("bob", bobUser.getUsername());
        assertEquals(1, prospects.get(0).sharedInterestCount());
    }

    @Test
    void findProspects_shouldExcludeAlreadySwipedUsers() {
        // Given
        UserRepository userRepository = new InMemoryUserRepository();
        SwipeRepository swipeRepository = new InMemorySwipeRepository();
        ProspectsService service = new ProspectsService(userRepository, swipeRepository);

        UserId alice = UserId.generate();
        User aliceUser = createUser(alice, "alice", 40.7128, -74.0060, Set.of(Interest.HIKING));
        userRepository.save(aliceUser);

        UserId bob = UserId.generate();
        User bobUser = createUser(bob, "bob", 40.7306, -73.9352, Set.of(Interest.HIKING));
        userRepository.save(bobUser);

        // Alice already swiped on Bob
        swipeRepository.saveIfNotExists(Swipe.create(alice, bob, SwipeDirection.LIKE));

        // When
        List<ProspectDto> prospects = service.findProspects(
                alice,
                Distance.ofKilometers(100),
                10,
                Set.of()
        );

        // Then
        assertEquals(0, prospects.size()); // Bob excluded because already swiped
    }

    private User createUser(UserId id, String username, double lat, double lon, Set<Interest> interests) {
        Location location = new Location(lat, lon);
        Preferences preferences = new Preferences(Set.of("ALL"), null, Distance.ofKilometers(100));
        Profile profile = new Profile(
                id,
                username,
                "Bio for " + username,
                LocalDate.of(1990, 1, 1),
                interests,
                preferences,
                location,
                List.of("photo1.jpg")  // Must have at least one photo to be discoverable
        );
        return new User(id, username, profile);
    }
}
