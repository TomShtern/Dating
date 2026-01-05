package com.datingapp.domain.repository;

import java.util.List;
import java.util.Optional;

import com.datingapp.domain.Distance;
import com.datingapp.domain.Location;
import com.datingapp.domain.User;
import com.datingapp.domain.UserId;

/**
 * Domain port for User persistence.
 */
public interface UserRepository {
    Optional<User> findById(UserId id);

    Optional<User> findByUsername(String username);

    void save(User user);

    List<User> findDiscoverableInRadius(Location center, Distance radius, int limit);

    boolean existsById(UserId id);

    boolean existsByUsername(String username);
}
