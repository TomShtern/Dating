package com.datingapp.domain.service;

import com.datingapp.domain.User;
import com.datingapp.domain.UserId;

/**
 * Domain service for password operations.
 * Decouples password hashing from repository pattern.
 * Maintains hexagonal architecture by keeping password logic
 * behind a domain service interface.
 */
public interface PasswordService {
    /**
     * Hash a raw password.
     */
    String hashPassword(String rawPassword);

    /**
     * Verify a raw password against a stored hash.
     */
    boolean verifyPassword(String rawPassword, String storedHash);

    /**
     * Save user with password hash.
     * This method exists because password is not part of the domain User aggregate.
     * It bridges the domain and persistence layers in a controlled way.
     */
    void saveUserWithPassword(User user, String rawPassword);

    /**
     * Get password hash for a user (for login verification).
     */
    String getPasswordHash(UserId userId);
}
