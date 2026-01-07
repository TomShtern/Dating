package com.datingapp.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.datingapp.domain.Distance;
import com.datingapp.domain.event.EventPublisher;
import com.datingapp.domain.matching.DistanceStrategy;
import com.datingapp.domain.matching.MatchScorer;
import com.datingapp.domain.matching.MatchingService;
import com.datingapp.domain.repository.MatchRepository;
import com.datingapp.domain.repository.SwipeRepository;
import com.datingapp.domain.repository.UserRepository;

@Configuration
public class DomainConfig {

    @Bean
    public MatchScorer matchScorer() {
        // In a real app, max distance might be configurable
        return new MatchScorer(List.of(
                new DistanceStrategy(Distance.ofKilometers(100))));
    }

    @Bean
    public MatchingService matchingService(
            MatchScorer matchScorer,
            UserRepository userRepository,
            SwipeRepository swipeRepository,
            MatchRepository matchRepository,
            EventPublisher eventPublisher) {
        return new MatchingService(
                matchScorer,
                userRepository,
                swipeRepository,
                matchRepository,
                eventPublisher);
    }
}
