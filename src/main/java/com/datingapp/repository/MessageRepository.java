package com.datingapp.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.datingapp.model.Match;
import com.datingapp.model.Message;
import com.datingapp.model.User;

/**
 * Repository for Message entity.
 * Handles chat history retrieval.
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    /**
     * Get all messages in a conversation, ordered oldest first.
     * Main query for chat history display.
     */
    List<Message> findByMatchOrderBySentAtAsc(Match match);

    /**
     * Get latest messages with pagination (for infinite scroll).
     */
    List<Message> findByMatchOrderBySentAtDesc(Match match, Pageable pageable);

    /**
     * Get the most recent message for preview in matches list.
     */
    Optional<Message> findTopByMatchOrderBySentAtDesc(Match match);

    /**
     * Count unread messages for notification badge.
     */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.match = :match AND m.sender != :user AND m.isRead = false")
    long countUnreadMessages(@Param("match") Match match, @Param("user") User user);

    /**
     * Mark all messages in a chat as read when user opens it.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Message m SET m.isRead = true WHERE m.match = :match AND m.sender != :user AND m.isRead = false")
    void markAllAsRead(@Param("match") Match match, @Param("user") User user);
}
