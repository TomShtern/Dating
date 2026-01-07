package com.datingapp.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data JPA repository for SwipeEntity.
 * Provides database access through Spring Data's method naming conventions and
 * custom queries.
 */
public interface SpringDataSwipeRepository extends JpaRepository<SwipeEntity, UUID> {

    Optional<SwipeEntity> findBySwiperIdAndTargetId(UUID swiperId, UUID targetId);

    List<SwipeEntity> findBySwiperId(UUID swiperId);

    @Query("SELECT s FROM SwipeEntity s WHERE s.targetId = :targetId AND (s.direction = 'LIKE' OR s.direction = 'SUPER_LIKE')")
    List<SwipeEntity> findLikersFor(@Param("targetId") UUID targetId);
}
