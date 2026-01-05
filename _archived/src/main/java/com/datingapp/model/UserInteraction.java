package com.datingapp.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;

@Entity
@Table(name = "user_interactions", uniqueConstraints = @UniqueConstraint(columnNames = { "from_user_id",
        "to_user_id" }))
public class UserInteraction implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /** Required by JPA. */
    public UserInteraction() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_user_id", nullable = false)
    private User fromUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_user_id", nullable = false)
    private User toUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private InteractionType interactionType;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Explicit Getters and Setters for IDE compatibility
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getFromUser() {
        return fromUser;
    }

    public void setFromUser(User fromUser) {
        this.fromUser = fromUser;
    }

    public User getToUser() {
        return toUser;
    }

    public void setToUser(User toUser) {
        this.toUser = toUser;
    }

    public InteractionType getInteractionType() {
        return interactionType;
    }

    public void setInteractionType(InteractionType interactionType) {
        this.interactionType = interactionType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Explicit Builder for IDE compatibility
    public static UserInteractionBuilder builder() {
        return new UserInteractionBuilder();
    }

    public static class UserInteractionBuilder {
        private UUID id;
        private User fromUser;
        private User toUser;
        private InteractionType interactionType;
        private LocalDateTime createdAt;

        UserInteractionBuilder() {
        }

        public UserInteractionBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public UserInteractionBuilder fromUser(User fromUser) {
            this.fromUser = fromUser;
            return this;
        }

        public UserInteractionBuilder toUser(User toUser) {
            this.toUser = toUser;
            return this;
        }

        public UserInteractionBuilder interactionType(InteractionType interactionType) {
            this.interactionType = interactionType;
            return this;
        }

        public UserInteractionBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public UserInteraction build() {
            UserInteraction interaction = new UserInteraction();
            interaction.id = this.id;
            interaction.fromUser = this.fromUser;
            interaction.toUser = this.toUser;
            interaction.interactionType = this.interactionType;
            interaction.createdAt = this.createdAt;
            return interaction;
        }
    }
}