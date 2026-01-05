package com.datingapp.infrastructure.persistence.inmemory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.datingapp.domain.Distance;
import com.datingapp.domain.Location;
import com.datingapp.domain.User;
import com.datingapp.domain.UserId;
import com.datingapp.domain.repository.UserRepository;

/**
 * In-memory implementation of UserRepository for Phase 0.
 */
public class InMemoryUserRepository implements UserRepository {
    private final Map<UserId, User> storage = new ConcurrentHashMap<>();

    @Override
    public Optional<User> findById(UserId id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return storage.values().stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst();
    }

    @Override
    public void save(User user) {
        storage.put(user.getId(), user);
    }

    @Override
    public List<User> findDiscoverableInRadius(Location center, Distance radius, int limit) {
        return storage.values().stream()
                .filter(User::canBeDiscovered)
                .filter(u -> u.getProfile() != null && u.getProfile().location() != null)
                .filter(u -> center.distanceTo(u.getProfile().location()).isLessThanOrEqual(radius))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsById(UserId id) {
        return storage.containsKey(id);
    }

    @Override
    public boolean existsByUsername(String username) {
        return storage.values().stream()
                .anyMatch(u -> u.getUsername().equals(username));
    }
}
