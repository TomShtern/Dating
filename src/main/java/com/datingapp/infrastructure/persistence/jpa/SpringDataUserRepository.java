package com.datingapp.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataUserRepository extends JpaRepository<UserEntity, UUID> {

        Optional<UserEntity> findByUsername(String username);

        boolean existsByUsername(String username);

        @Query(value = """
                        SELECT * FROM users
                        WHERE state = 'ACTIVE'
                        AND latitude IS NOT NULL
                        AND longitude IS NOT NULL
                        AND (6371 * acos(cos(radians(:centerLat)) * cos(radians(latitude)) *
                             cos(radians(longitude) - radians(:centerLon)) +
                             sin(radians(:centerLat)) * sin(radians(latitude)))) <= :radiusKm
                        LIMIT :limit
                        """, nativeQuery = true)
        List<UserEntity> findDiscoverableInRadius(
                        @Param("centerLat") double centerLat,
                        @Param("centerLon") double centerLon,
                        @Param("radiusKm") double radiusKm,
                        @Param("limit") int limit);
}
