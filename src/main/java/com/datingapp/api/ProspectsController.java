package com.datingapp.api;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.datingapp.api.dto.ProspectDto;
import com.datingapp.api.dto.ProspectsResponseDto;
import com.datingapp.application.ProspectsService;
import com.datingapp.domain.Distance;
import com.datingapp.domain.Interest;
import com.datingapp.domain.UserId;
import com.datingapp.infrastructure.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/prospects")
public class ProspectsController {
    private final ProspectsService prospectsService;
    private final JwtTokenProvider jwtTokenProvider;

    public ProspectsController(ProspectsService prospectsService, JwtTokenProvider jwtTokenProvider) {
        this.prospectsService = prospectsService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @GetMapping
    public ResponseEntity<ProspectsResponseDto> getProspects(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "100") double maxDistanceKm,
            HttpServletRequest request) {

        String token = extractToken(request);
        String userIdString = jwtTokenProvider.extractUserId(token);
        UserId userId = new UserId(UUID.fromString(userIdString));

        List<ProspectDto> prospects = prospectsService.findProspects(
                userId,
                Distance.ofKilometers(maxDistanceKm),
                limit,
                Set.of()
        );

        return ResponseEntity.ok(new ProspectsResponseDto(prospects, prospects.size()));
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        throw new IllegalArgumentException("Missing or invalid Authorization header");
    }
}
