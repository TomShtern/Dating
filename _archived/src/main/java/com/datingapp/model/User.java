package com.datingapp.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * User entity representing a dating app user profile.
 * This class maps to the "users" table in the PostgreSQL database.
 * It includes fields for user identification, authentication, personal details,
 * and creation timestamp.
 * Implements {@link java.io.Serializable} for potential use in distributed
 * systems
 * or caching.
 */
@Entity // Marks this class as a JPA entity
@Table(name = "users") // Specifies the table name in the database
public class User implements java.io.Serializable {

    /**
     * Serial Version UID for serialization.
     * Ensures compatibility during deserialization if the class structure changes.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Default constructor required by JPA.
     * JPA needs a no-argument constructor to instantiate entities.
     */
    public User() {
    }

    /**
     * Unique identifier for the user.
     * Generated automatically as a UUID upon persistence.
     */
    @Id // Marks this field as the primary key
    @GeneratedValue(strategy = GenerationType.UUID) // Specifies UUID generation strategy
    private UUID id;

    /**
     * The user's unique username.
     * Cannot be null and must be unique across all users. Max length 50 characters.
     */
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /**
     * The hashed password of the user.
     * Stored as a hash for security reasons. Cannot be null.
     */
    @Column(nullable = false)
    private String passwordHash;

    /**
     * The user's email address.
     * Optional field. Max length 100 characters.
     */
    @Column(length = 100)
    private String email;

    /**
     * The display name of the user, visible to others.
     * Cannot be null. Max length 100 characters.
     */
    @Column(nullable = false, length = 100)
    private String displayName;

    /**
     * The age of the user.
     * Cannot be null.
     */
    @Column(nullable = false)
    private Integer age;

    /**
     * The gender of the user.
     * Cannot be null. Max length 20 characters.
     */
    @Column(nullable = false, length = 20)
    private String gender;

    /**
     * A short biography or description of the user.
     * Optional field. Max length 500 characters.
     */
    @Column(length = 500)
    private String bio;

    /**
     * The timestamp when the user profile was created.
     * Automatically set to the current time before the entity is persisted.
     * Cannot be null.
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * Callback method executed before the entity is persisted (inserted) into the
     * database.
     * Sets the {@code createdAt} timestamp to the current time.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now(); // Set creation timestamp
    }

    // Explicit Getters and Setters for IDE compatibility and Lombok-free
    // development

    /**
     * Retrieves the unique identifier of the user.
     * 
     * @return The UUID of the user.
     */
    public UUID getId() {
        return id;
    }

    /**
     * Sets the unique identifier of the user.
     * 
     * @param id The UUID to set for the user.
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * Retrieves the username of the user.
     * 
     * @return The username string.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username of the user.
     * 
     * @param username The username string to set.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Retrieves the hashed password of the user.
     * 
     * @return The password hash string.
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * Sets the hashed password of the user.
     * 
     * @param passwordHash The password hash string to set.
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * Retrieves the email address of the user.
     * 
     * @return The email address string.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the email address of the user.
     * 
     * @param email The email address string to set.
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Retrieves the display name of the user.
     * 
     * @return The display name string.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Sets the display name of the user.
     * 
     * @param displayName The display name string to set.
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Retrieves the age of the user.
     * 
     * @return The age as an Integer.
     */
    public Integer getAge() {
        return age;
    }

    /**
     * Sets the age of the user.
     * 
     * @param age The age (Integer) to set.
     */
    public void setAge(Integer age) {
        this.age = age;
    }

    /**
     * Retrieves the gender of the user.
     * 
     * @return The gender string.
     */
    public String getGender() {
        return gender;
    }

    /**
     * Sets the gender of the user.
     * 
     * @param gender The gender string to set.
     */
    public void setGender(String gender) {
        this.gender = gender;
    }

    /**
     * Retrieves the biography of the user.
     * 
     * @return The biography string.
     */
    public String getBio() {
        return bio;
    }

    /**
     * Sets the biography of the user.
     * 
     * @param bio The biography string to set.
     */
    public void setBio(String bio) {
        this.bio = bio;
    }

    /**
     * Retrieves the creation timestamp of the user profile.
     * 
     * @return The {@link LocalDateTime} when the profile was created.
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the creation timestamp of the user profile.
     * 
     * @param createdAt The {@link LocalDateTime} to set as the creation timestamp.
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Explicit Builder for IDE compatibility and fluent object creation

    /**
     * Provides a static factory method to create a new {@link UserBuilder}
     * instance.
     * This allows for fluent construction of {@link User} objects.
     * 
     * @return A new instance of {@link UserBuilder}.
     */
    public static UserBuilder builder() {
        return new UserBuilder();
    }

    /**
     * A static nested builder class for constructing {@link User} objects.
     * This pattern allows for creating immutable or complex objects step-by-step
     * with a clear, readable API.
     */
    public static class UserBuilder {
        private UUID id;
        private String username;
        private String passwordHash;
        private String email;
        private String displayName;
        private Integer age;
        private String gender;
        private String bio;
        private LocalDateTime createdAt;

        /**
         * Private constructor to enforce usage of the static {@code builder()} method.
         */
        UserBuilder() {
        }

        /**
         * Sets the ID for the user.
         * 
         * @param id The UUID for the user.
         * @return The current builder instance for chaining.
         */
        public UserBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the username for the user.
         * 
         * @param username The username string.
         * @return The current builder instance for chaining.
         */
        public UserBuilder username(String username) {
            this.username = username;
            return this;
        }

        /**
         * Sets the password hash for the user.
         * 
         * @param passwordHash The password hash string.
         * @return The current builder instance for chaining.
         */
        public UserBuilder passwordHash(String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }

        /**
         * Sets the email for the user.
         * 
         * @param email The email address string.
         * @return The current builder instance for chaining.
         */
        public UserBuilder email(String email) {
            this.email = email;
            return this;
        }

        /**
         * Sets the display name for the user.
         * 
         * @param displayName The display name string.
         * @return The current builder instance for chaining.
         */
        public UserBuilder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        /**
         * Sets the age for the user.
         * 
         * @param age The age as an Integer.
         * @return The current builder instance for chaining.
         */
        public UserBuilder age(Integer age) {
            this.age = age;
            return this;
        }

        /**
         * Sets the gender for the user.
         * 
         * @param gender The gender string.
         * @return The current builder instance for chaining.
         */
        public UserBuilder gender(String gender) {
            this.gender = gender;
            return this;
        }

        /**
         * Sets the biography for the user.
         * 
         * @param bio The biography string.
         * @return The current builder instance for chaining.
         */
        public UserBuilder bio(String bio) {
            this.bio = bio;
            return this;
        }

        /**
         * Sets the creation timestamp for the user.
         * 
         * @param createdAt The {@link LocalDateTime} creation timestamp.
         * @return The current builder instance for chaining.
         */
        public UserBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        /**
         * Builds and returns a new {@link User} object with the properties
         * set in this builder.
         * 
         * @return A new {@link User} instance.
         */
        public User build() {
            User user = new User(); // Create a new User instance
            user.id = this.id;
            user.username = this.username;
            user.passwordHash = this.passwordHash;
            user.email = this.email;
            user.displayName = this.displayName;
            user.age = this.age;
            user.gender = this.gender;
            user.bio = this.bio;
            user.createdAt = this.createdAt;
            return user; // Return the constructed User object
        }
    }
}
