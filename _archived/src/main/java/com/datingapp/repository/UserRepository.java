package com.datingapp.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.datingapp.model.User;

/**
 * Repository for User entity CRUD operations.
 * Spring Data JPA auto-implements all methods at runtime.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find a user by their unique username.
     * Used for login authentication.
     */
    Optional<User> findByUsername(String username);

    /**
     * Check if a username is already taken.
     * Used during registration validation.
     */
    boolean existsByUsername(String username);

    /**
     * Find a user by email (optional, for password reset).
     */
    Optional<User> findByEmail(String email);
}
