package com.datingapp.api;

import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.datingapp.api.dto.MatchDataDto;
import com.datingapp.api.dto.SwipeRequestDto;
import com.datingapp.api.dto.SwipeResponseDto;
import com.datingapp.domain.Match;
import com.datingapp.domain.User;
import com.datingapp.domain.UserId;
import com.datingapp.domain.matching.MatchingService;
import com.datingapp.infrastructure.security.JwtTokenProvider;
import com.datingapp.domain.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/swipes")
public class SwipesController {
    private final MatchingService matchingService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    public SwipesController(MatchingService matchingService, JwtTokenProvider jwtTokenProvider, UserRepository userRepository) {
        this.matchingService = matchingService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<SwipeResponseDto> postSwipe(
            @RequestBody SwipeRequestDto request,
            HttpServletRequest httpRequest) {

        String token = extractToken(httpRequest);
        String userIdString = jwtTokenProvider.extractUserId(token);
        UserId swiperId = new UserId(UUID.fromString(userIdString));

        // Validate: can't swipe on yourself
        if (swiperId.equals(request.targetUserId())) {
            return ResponseEntity.badRequest().build();
        }

        // Process swipe (uses MatchingService which calls repositories)
        Optional<Match> match = matchingService.processSwipe(swiperId, request.targetUserId(), request.direction());

        // Build response
        MatchDataDto matchData = null;
        if (match.isPresent()) {
            User otherUser = userRepository.findById(request.targetUserId()).orElseThrow();
            matchData = new MatchDataDto(
                match.get().getId().value(),
                request.targetUserId(),
                otherUser.getProfile().displayName()
            );
        }

        SwipeResponseDto response = new SwipeResponseDto(
            null, // swipeId not critical for now
            matchData
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        throw new IllegalArgumentException("Missing or invalid Authorization header");
    }
}
