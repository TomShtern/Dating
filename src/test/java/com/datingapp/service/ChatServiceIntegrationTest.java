package com.datingapp.service;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.datingapp.model.Match;
import com.datingapp.model.Message;
import com.datingapp.model.User;
import com.datingapp.repository.MatchRepository;
import com.datingapp.repository.MessageRepository;
import com.datingapp.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ChatServiceIntegrationTest {

    @Autowired
    private ChatService chatService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private MessageRepository messageRepository;

    private User user1;
    private User user2;
    private Match match;

    @BeforeEach
    void setUp() {
        // Create two users
        user1 = User.builder()
                .username("user1")
                .passwordHash("hash")
                .displayName("User One")
                .age(25)
                .gender("Male")
                .email("one@test.com")
                .build();
        userRepository.save(user1);

        user2 = User.builder()
                .username("user2")
                .passwordHash("hash")
                .displayName("User Two")
                .age(25)
                .gender("Female")
                .email("two@test.com")
                .build();
        userRepository.save(user2);

        // Create a match between them
        match = Match.builder()
                .user1(user1)
                .user2(user2)
                .build();
        matchRepository.save(match);
    }

    @Test
    void sendMessage_shouldPersistMessage() {
        String content = "Hello World";
        Message savedMessage = chatService.sendMessage(user1, match.getId(), content);

        assertNotNull(savedMessage.getId());
        assertEquals(content, savedMessage.getContent());
        assertEquals(user1.getId(), savedMessage.getSender().getId());
        assertEquals(match.getId(), savedMessage.getMatch().getId());
        assertNotNull(savedMessage.getSentAt());
        assertFalse(savedMessage.getIsRead());
    }

    @Test
    void sendMessage_shouldThrow_whenUserNotInMatch() {
        User outsider = User.builder()
                .username("outsider")
                .passwordHash("hash")
                .displayName("Outsider")
                .age(30)
                .gender("Other")
                .email("out@test.com")
                .build();
        userRepository.save(outsider);

        assertThrows(IllegalArgumentException.class, () -> {
            chatService.sendMessage(outsider, match.getId(), "Hello?");
        });
    }

    @Test
    void getChatHistory_shouldReturnMessagesInOrder() throws InterruptedException {
        // Send a few messages
        chatService.sendMessage(user1, match.getId(), "First");
        Thread.sleep(100); // Ensure timestamp difference
        chatService.sendMessage(user2, match.getId(), "Second");
        Thread.sleep(100);
        chatService.sendMessage(user1, match.getId(), "Third");

        List<Message> history = chatService.getChatHistory(match.getId(), user1);

        assertEquals(3, history.size());
        assertEquals("First", history.get(0).getContent());
        assertEquals("Second", history.get(1).getContent());
        assertEquals("Third", history.get(2).getContent());
    }

    @Test
    void getChatHistory_shouldMarkMessagesAsRead() {
        // User 2 sends a message
        Message msg = chatService.sendMessage(user2, match.getId(), "Hi there");
        assertFalse(msg.getIsRead());

        // User 1 reads history
        chatService.getChatHistory(match.getId(), user1);

        // Verify it is now read in DB
        Message updatedMsg = messageRepository.findById(msg.getId()).orElseThrow();
        assertTrue(updatedMsg.getIsRead());
    }

    @Test
    void getUnreadCount_shouldReturnCorrectCount() {
        chatService.sendMessage(user2, match.getId(), "Msg 1");
        chatService.sendMessage(user2, match.getId(), "Msg 2");
        chatService.sendMessage(user1, match.getId(), "My reply"); // Should not count as unread for user1

        long count = chatService.getUnreadCount(match, user1);
        assertEquals(2, count);

        long countForSender = chatService.getUnreadCount(match, user2);
        assertEquals(1, countForSender);
    }
}
