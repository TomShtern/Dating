package com.datingapp.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.datingapp.model.Match;
import com.datingapp.model.Message;
import com.datingapp.model.User;
import com.datingapp.repository.MatchRepository;
import com.datingapp.repository.MessageRepository;

@Service
@Transactional
public class ChatService {

    private final MessageRepository messageRepository;
    private final MatchRepository matchRepository;

    public ChatService(MessageRepository messageRepository, MatchRepository matchRepository) {
        this.messageRepository = messageRepository;
        this.matchRepository = matchRepository;
    }

    public Message sendMessage(User sender, UUID matchId, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalStateException("Message cannot be empty");
        }
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found"));
        if (!isUserInMatch(sender, match)) {
            throw new IllegalArgumentException("User not in match");
        }
        Message message = Message.builder()
                .match(match)
                .sender(sender)
                .content(content.trim())
                .isRead(false)
                .build();
        return messageRepository.save(message);
    }

    public List<Message> getChatHistory(UUID matchId, User currentUser) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found"));
        if (!isUserInMatch(currentUser, match)) {
            throw new IllegalArgumentException("User not in match");
        }
        messageRepository.markAllAsRead(match, currentUser);
        return messageRepository.findByMatchOrderBySentAtAsc(match);
    }

    @Transactional(readOnly = true)
    public Message getLatestMessage(Match match) {
        return messageRepository.findTopByMatchOrderBySentAtDesc(match).orElse(null);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Match match, User user) {
        return messageRepository.countUnreadMessages(match, user);
    }

    private boolean isUserInMatch(User user, Match match) {
        return match.getUser1().getId().equals(user.getId()) || match.getUser2().getId().equals(user.getId());
    }
}