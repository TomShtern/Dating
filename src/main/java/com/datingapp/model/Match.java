package com.datingapp.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;

/**
 * Entity representing a successful match between two users.
 * Created when both users have "Liked" each other.
 * Enables messaging between the matched users.
 */
@Entity
@Table(name = "matches")
public class Match implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /** Required by JPA. */
    public Match() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user1_id", nullable = false)
    private User user1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user2_id", nullable = false)
    private User user2;

    @Column(nullable = false)
    private LocalDateTime matchedAt;

    @PrePersist
    protected void onCreate() {
        matchedAt = LocalDateTime.now();
    }

    /**
     * Get the other user in the match (not the given user).
     */
    public User getOtherUser(User currentUser) {
        if (user1.getId().equals(currentUser.getId())) {
            return user2;
        }
        return user1;
    }

    // Explicit Getters and Setters for IDE compatibility
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getUser1() {
        return user1;
    }

    public void setUser1(User user1) {
        this.user1 = user1;
    }

    public User getUser2() {
        return user2;
    }

    public void setUser2(User user2) {
        this.user2 = user2;
    }

    public LocalDateTime getMatchedAt() {
        return matchedAt;
    }

    public void setMatchedAt(LocalDateTime matchedAt) {
        this.matchedAt = matchedAt;
    }

    // Explicit Builder for IDE compatibility
    public static MatchBuilder builder() {
        return new MatchBuilder();
    }

    public static class MatchBuilder {
        private UUID id;
        private User user1;
        private User user2;
        private LocalDateTime matchedAt;

        MatchBuilder() {
        }

        public MatchBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public MatchBuilder user1(User user1) {
            this.user1 = user1;
            return this;
        }

        public MatchBuilder user2(User user2) {
            this.user2 = user2;
            return this;
        }

        public MatchBuilder matchedAt(LocalDateTime matchedAt) {
            this.matchedAt = matchedAt;
            return this;
        }

        public Match build() {
            Match match = new Match();
            match.id = this.id;
            match.user1 = this.user1;
            match.user2 = this.user2;
            match.matchedAt = this.matchedAt;
            return match;
        }
    }
}
