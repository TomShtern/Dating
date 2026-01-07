package com.datingapp.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.datingapp.domain.Distance;
import com.datingapp.domain.Location;
import com.datingapp.domain.User;
import com.datingapp.domain.UserId;
import com.datingapp.domain.repository.UserRepository;

@Repository
public class JpaUserRepository implements UserRepository {

    private final SpringDataUserRepository springDataRepo;
    private final Map<UUID, String> passwordHashCache = new java.util.concurrent.ConcurrentHashMap<>();

    public JpaUserRepository(SpringDataUserRepository springDataRepo) {
        this.springDataRepo = springDataRepo;
    }

    @Override
    public Optional<User> findById(UserId id) {
        return springDataRepo.findById(id.value()).map(UserMapper::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return springDataRepo.findByUsername(username).map(UserMapper::toDomain);
    }

    @Override
    public void save(User user) {
        save(user, null);
    }

    public void save(User user, String passwordHash) {
        UserEntity entity = UserMapper.toEntity(user, passwordHash);

        // If password is provided, use it; otherwise check cache
        if (passwordHash != null) {
            passwordHashCache.put(user.getId().value(), passwordHash);
        } else if (passwordHashCache.containsKey(user.getId().value())) {
            entity.setPasswordHash(passwordHashCache.get(user.getId().value()));
        }

        springDataRepo.save(entity);
    }

    public String getPasswordHash(UserId userId) {
        return springDataRepo.findById(userId.value())
                .map(UserEntity::getPasswordHash)
                .orElse(null);
    }

    @Override
    public List<User> findDiscoverableInRadius(Location center, Distance radius, int limit) {
        // Using Haversine formula approximation for PostgreSQL
        List<UserEntity> entities = springDataRepo.findDiscoverableInRadius(
                center.lat(),
                center.lon(),
                radius.kilometers(),
                limit);
        return entities.stream().map(UserMapper::toDomain).toList();
    }

    @Override
    public boolean existsById(UserId id) {
        return springDataRepo.existsById(id.value());
    }

    @Override
    public boolean existsByUsername(String username) {
        return springDataRepo.existsByUsername(username);
    }
}
