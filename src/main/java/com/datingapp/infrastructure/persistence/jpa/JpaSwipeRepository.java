package com.datingapp.infrastructure.persistence.jpa;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.datingapp.domain.Swipe;
import com.datingapp.domain.SwipeId;
import com.datingapp.domain.UserId;
import com.datingapp.domain.repository.SwipeRepository;

/**
 * JPA implementation of SwipeRepository (domain port).
 * Adapts Spring Data JpaRepository to domain SwipeRepository interface.
 *
 * ★ Insight ─────────────────────────────────────
 * This follows the Adapter pattern: JpaSwipeRepository (adapter) implements
 * SwipeRepository (domain port) using SpringDataSwipeRepository (Spring Data).
 * This maintains hexagonal architecture by keeping domain logic independent
 * from Spring Data.
 * ─────────────────────────────────────────────────
 */
@Repository
public class JpaSwipeRepository implements SwipeRepository {

    private final SpringDataSwipeRepository springDataRepo;

    public JpaSwipeRepository(SpringDataSwipeRepository springDataRepo) {
        this.springDataRepo = springDataRepo;
    }

    @Override
    public Swipe saveIfNotExists(Swipe swipe) {
        Optional<SwipeEntity> existing = springDataRepo.findBySwiperIdAndTargetId(
                swipe.getSwiperId().value(),
                swipe.getTargetId().value());

        if (existing.isPresent()) {
            return toDomain(existing.get());
        }

        SwipeEntity entity = toEntity(swipe);
        springDataRepo.save(entity);
        return swipe;
    }

    @Override
    public Optional<Swipe> findByPair(UserId swiperId, UserId targetId) {
        return springDataRepo.findBySwiperIdAndTargetId(swiperId.value(), targetId.value())
                .map(this::toDomain);
    }

    @Override
    public Set<UserId> findSwipedUserIds(UserId swiperId) {
        return springDataRepo.findBySwiperId(swiperId.value()).stream()
                .map(entity -> new UserId(entity.getTargetId()))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<UserId> findPendingLikersFor(UserId userId) {
        return springDataRepo.findLikersFor(userId.value()).stream()
                .map(entity -> new UserId(entity.getSwiperId()))
                .collect(Collectors.toSet());
    }

    private SwipeEntity toEntity(Swipe swipe) {
        return new SwipeEntity(
                swipe.getId().value(),
                swipe.getSwiperId().value(),
                swipe.getTargetId().value(),
                swipe.getDirection(),
                swipe.getCreatedAt());
    }

    private Swipe toDomain(SwipeEntity entity) {
        return Swipe.reconstitute(
                new SwipeId(entity.getId()),
                new UserId(entity.getSwiperId()),
                new UserId(entity.getTargetId()),
                entity.getDirection(),
                entity.getCreatedAt());
    }
}
