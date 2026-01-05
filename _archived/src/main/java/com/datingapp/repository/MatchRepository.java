package com.datingapp.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.datingapp.model.Match;
import com.datingapp.model.User;

/**
 * Repository for Match entity.
 * Handles finding matches for a user.
 */
@Repository
public interface MatchRepository extends JpaRepository<Match, UUID> {

    /**
     * Find all matches where the given user is involved (as user1 or user2).
     * Most recent matches first.
     */
    @Query("SELECT m FROM Match m WHERE m.user1 = :user OR m.user2 = :user ORDER BY m.matchedAt DESC")
    List<Match> findMatchesForUser(@Param("user") User user);

    /**
     * Check if two specific users are matched.
     */
    @Query("SELECT m FROM Match m WHERE " +
            "(m.user1 = :userA AND m.user2 = :userB) OR " +
            "(m.user1 = :userB AND m.user2 = :userA)")
    Optional<Match> findMatchBetween(@Param("userA") User userA, @Param("userB") User userB);

    /**
     * Quick boolean check if two users are matched.
     */
    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM Match m " +
            "WHERE (m.user1 = :a AND m.user2 = :b) OR (m.user1 = :b AND m.user2 = :a)")
    boolean areMatched(@Param("a") User a, @Param("b") User b);

    /**
     * Get all user IDs that the given user is matched with.
     * Used to exclude from potential matches.
     */
    @Query("SELECT CASE WHEN m.user1 = :user THEN m.user2.id ELSE m.user1.id END " +
            "FROM Match m WHERE m.user1 = :user OR m.user2 = :user")
    List<UUID> findMatchedUserIds(@Param("user") User user);
}
