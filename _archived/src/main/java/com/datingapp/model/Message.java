package com.datingapp.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;

/**
 * Entity representing a chat message between matched users.
 * Messages can only be sent within the context of an existing Match.
 */
@Entity
@Table(name = "messages")
public class Message implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /** Required by JPA. */
    public Message() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    @Column(nullable = false)
    private Boolean isRead;

    @PrePersist
    protected void onCreate() {
        sentAt = LocalDateTime.now();
        if (isRead == null) {
            isRead = false;
        }
    }

    // Explicit Getters and Setters for IDE compatibility
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Match getMatch() {
        return match;
    }

    public void setMatch(Match match) {
        this.match = match;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    // Explicit Builder for IDE compatibility
    public static MessageBuilder builder() {
        return new MessageBuilder();
    }

    public static class MessageBuilder {
        private UUID id;
        private Match match;
        private User sender;
        private String content;
        private LocalDateTime sentAt;
        private Boolean isRead;

        MessageBuilder() {
        }

        public MessageBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public MessageBuilder match(Match match) {
            this.match = match;
            return this;
        }

        public MessageBuilder sender(User sender) {
            this.sender = sender;
            return this;
        }

        public MessageBuilder content(String content) {
            this.content = content;
            return this;
        }

        public MessageBuilder sentAt(LocalDateTime sentAt) {
            this.sentAt = sentAt;
            return this;
        }

        public MessageBuilder isRead(Boolean isRead) {
            this.isRead = isRead;
            return this;
        }

        public Message build() {
            Message message = new Message();
            message.id = this.id;
            message.match = this.match;
            message.sender = this.sender;
            message.content = this.content;
            message.sentAt = this.sentAt;
            message.isRead = this.isRead;
            return message;
        }
    }
}
