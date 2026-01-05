package com.datingapp.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.datingapp.model.InteractionType;
import com.datingapp.model.User;
import com.datingapp.model.UserInteraction;

/**
 * Repository for managing user interactions (likes/passes).
 */
@Repository
public interface UserInteractionRepository extends JpaRepository<UserInteraction, UUID> {

    Optional<UserInteraction> findByFromUserAndToUser(User fromUser, User toUser);

    Optional<UserInteraction> findByFromUserAndToUserAndInteractionType(User fromUser, User toUser,
            InteractionType interactionType);

    @Query("SELECT i.toUser.id FROM UserInteraction i WHERE i.fromUser = :user")
    List<UUID> findAllInteractedUserIds(@Param("user") User user);

    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM UserInteraction i WHERE i.fromUser = :from AND i.toUser = :to AND i.interactionType = 'LIKE'")
    boolean hasLiked(@Param("from") User from, @Param("to") User to);
}