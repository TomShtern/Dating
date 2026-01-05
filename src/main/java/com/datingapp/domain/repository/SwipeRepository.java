package com.datingapp.domain.repository;

import java.util.Optional;
import java.util.Set;

import com.datingapp.domain.Swipe;
import com.datingapp.domain.UserId;

/**
 * Domain port for Swipe persistence.
 */
public interface SwipeRepository {
    Swipe saveIfNotExists(Swipe swipe);

    Optional<Swipe> findByPair(UserId swiper, UserId target);

    Set<UserId> findSwipedUserIds(UserId swiper);

    Set<UserId> findPendingLikersFor(UserId userId);
}
