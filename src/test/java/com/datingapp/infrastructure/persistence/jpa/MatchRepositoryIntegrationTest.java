package com.datingapp.infrastructure.persistence.jpa;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.datingapp.IntegrationTestBase;
import com.datingapp.domain.Match;
import com.datingapp.domain.MatchId;
import com.datingapp.domain.UserId;
import com.datingapp.domain.repository.MatchRepository;

/**
 * Integration tests for MatchRepository with real PostgreSQL.
 * Verifies that matches are properly persisted and retrieved with canonical ID deduplication.
 */
class MatchRepositoryIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MatchRepository matchRepository;

    private UserId user1;
    private UserId user2;
    private UserId user3;

    @BeforeEach
    void setUp() {
        user1 = UserId.generate();
        user2 = UserId.generate();
        user3 = UserId.generate();
    }

    @Test
    void saveIfNotExists_shouldSaveNewMatch() {
        Match match = Match.create(user1, user2);

        Match saved = matchRepository.saveIfNotExists(match);

        assertNotNull(saved);
        assertEquals(match.getId(), saved.getId());
        assertTrue(saved.involves(user1));
        assertTrue(saved.involves(user2));
    }

    @Test
    void saveIfNotExists_shouldNotDuplicateMatch() {
        Match match1 = Match.create(user1, user2);
        matchRepository.saveIfNotExists(match1);

        Match match2 = Match.create(user2, user1); // Reverse order
        Match result = matchRepository.saveIfNotExists(match2);

        // Should return existing match with canonical ID
        assertEquals(match1.getId(), result.getId());
        assertFalse(result.isNewlyCreated());
    }

    @Test
    void findById_shouldFindExistingMatch() {
        Match match = Match.create(user1, user2);
        matchRepository.saveIfNotExists(match);

        Optional<Match> found = matchRepository.findById(match.getId());

        assertTrue(found.isPresent());
        assertEquals(match.getId(), found.get().getId());
    }

    @Test
    void findById_shouldReturnEmptyWhenNotFound() {
        MatchId nonExistentId = MatchId.canonical(user1, user2);
        Optional<Match> found = matchRepository.findById(nonExistentId);
        assertTrue(found.isEmpty());
    }

    @Test
    void findByUser_shouldReturnAllMatchesForUser() {
        matchRepository.saveIfNotExists(Match.create(user1, user2));
        matchRepository.saveIfNotExists(Match.create(user1, user3));
        matchRepository.saveIfNotExists(Match.create(user2, user3)); // user1 not involved

        List<Match> matches = matchRepository.findByUser(user1);

        assertEquals(2, matches.size());
        assertTrue(matches.stream().allMatch(m -> m.involves(user1)));
    }

    @Test
    void findByUser_shouldReturnEmptyListWhenNoMatches() {
        List<Match> matches = matchRepository.findByUser(user1);
        assertTrue(matches.isEmpty());
    }
}
