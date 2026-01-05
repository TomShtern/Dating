package com.datingapp.infrastructure.persistence.inmemory;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.datingapp.domain.Swipe;
import com.datingapp.domain.UserId;
import com.datingapp.domain.repository.SwipeRepository;

/**
 * In-memory implementation of SwipeRepository.
 */
public class InMemorySwipeRepository implements SwipeRepository {
    private final Map<String, Swipe> storage = new ConcurrentHashMap<>();

    @Override
    public Swipe saveIfNotExists(Swipe swipe) {
        String key = key(swipe.getSwiperId(), swipe.getTargetId());
        return storage.putIfAbsent(key, swipe) == null ? swipe : storage.get(key);
    }

    @Override
    public Optional<Swipe> findByPair(UserId swiper, UserId target) {
        return Optional.ofNullable(storage.get(key(swiper, target)));
    }

    @Override
    public Set<UserId> findSwipedUserIds(UserId swiper) {
        return storage.values().stream()
                .filter(s -> s.getSwiperId().equals(swiper))
                .map(Swipe::getTargetId)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<UserId> findPendingLikersFor(UserId userId) {
        // This is inefficient in memory, but fine for small counts
        return storage.values().stream()
                .filter(s -> s.getTargetId().equals(userId) && s.isLike())
                .map(Swipe::getSwiperId)
                .collect(Collectors.toSet());
    }

    private String key(UserId swiper, UserId target) {
        return swiper.value().toString() + "_" + target.value().toString();
    }
}
