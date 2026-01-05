package com.datingapp.application;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.datingapp.api.dto.ProspectDto;
import com.datingapp.domain.Distance;
import com.datingapp.domain.Interest;
import com.datingapp.domain.Location;
import com.datingapp.domain.User;
import com.datingapp.domain.UserId;
import com.datingapp.domain.repository.SwipeRepository;
import com.datingapp.domain.repository.UserRepository;

@Service
public class ProspectsService {
    private final UserRepository userRepository;
    private final SwipeRepository swipeRepository;

    public ProspectsService(UserRepository userRepository, SwipeRepository swipeRepository) {
        this.userRepository = userRepository;
        this.swipeRepository = swipeRepository;
    }

    public List<ProspectDto> findProspects(UserId userId, Distance maxDistance, int limit, Set<Interest> excludedInterests) {
        User currentUser = userRepository.findById(userId).orElseThrow();
        Location myLocation = currentUser.getProfile().location();
        Set<Interest> myInterests = currentUser.getProfile().interests();

        Set<UserId> alreadySwipedIds = swipeRepository.findSwipedUserIds(userId);

        return userRepository.findDiscoverableInRadius(myLocation, maxDistance, limit).stream()
                .filter(u -> !u.getId().equals(userId)) // Exclude self
                .filter(u -> !alreadySwipedIds.contains(u.getId())) // Exclude already swiped
                .map(u -> new ProspectDto(
                    u.getId(),
                    u.getProfile().displayName(),
                    calculateAge(u.getProfile().birthDate()),
                    u.getProfile().location().distanceTo(myLocation).kilometers(),
                    countSharedInterests(myInterests, u.getProfile().interests())
                ))
                .collect(Collectors.toList());
    }

    private int calculateAge(LocalDate birthDate) {
        return LocalDate.now().getYear() - birthDate.getYear();
    }

    private int countSharedInterests(Set<Interest> myInterests, Set<Interest> theirInterests) {
        return (int) myInterests.stream()
                .filter(theirInterests::contains)
                .count();
    }
}
