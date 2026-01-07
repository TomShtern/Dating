package com.datingapp.infrastructure.persistence.jpa;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data JPA repository for MatchEntity.
 * Provides database access through Spring Data's method naming conventions and custom queries.
 *
 * Note: MatchEntity uses String IDs (canonical composite key from two UUIDs).
 */
public interface SpringDataMatchRepository extends JpaRepository<MatchEntity, String> {

    @Query("SELECT m FROM MatchEntity m WHERE m.userAId = :userId OR m.userBId = :userId")
    List<MatchEntity> findByUser(@Param("userId") UUID userId);
}
