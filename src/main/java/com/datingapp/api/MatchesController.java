package com.datingapp.api;

import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.datingapp.api.dto.MatchDto;
import com.datingapp.domain.Match;
import com.datingapp.domain.User;
import com.datingapp.domain.UserId;
import com.datingapp.infrastructure.security.JwtTokenProvider;
import com.datingapp.domain.repository.MatchRepository;
import com.datingapp.domain.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/matches")
public class MatchesController {
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public MatchesController(MatchRepository matchRepository, UserRepository userRepository, JwtTokenProvider jwtTokenProvider) {
        this.matchRepository = matchRepository;
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @GetMapping
    public ResponseEntity<?> getMatches(HttpServletRequest request) {
        String token = extractToken(request);
        String userIdString = jwtTokenProvider.extractUserId(token);
        UserId userId = new UserId(UUID.fromString(userIdString));

        List<Match> matches = matchRepository.findByUser(userId);

        List<MatchDto> matchDtos = matches.stream()
                .map(match -> {
                    UserId otherUserId = match.otherUser(userId);
                    User otherUser = userRepository.findById(otherUserId).orElseThrow();
                    return new MatchDto(
                        match.getId().value(),
                        otherUserId,
                        otherUser.getProfile().displayName()
                    );
                })
                .toList();

        return ResponseEntity.ok(new java.util.HashMap<String, Object>() {{
            put("matches", matchDtos);
        }});
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        throw new IllegalArgumentException("Missing or invalid Authorization header");
    }
}
