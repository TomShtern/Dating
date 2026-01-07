package com.datingapp.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.datingapp.domain.Match;
import com.datingapp.domain.MatchId;
import com.datingapp.domain.UserId;
import com.datingapp.domain.repository.MatchRepository;

/**
 * JPA implementation of MatchRepository (domain port).
 * Adapts Spring Data JpaRepository to domain MatchRepository interface.
 *
 * ★ Insight ─────────────────────────────────────
 * This repository leverages the canonical ID pattern from the domain Match aggregate:
 * When saving, it checks if a match with the same canonical ID already exists,
 * preventing duplicate matches regardless of user order (A→B or B→A).
 * ─────────────────────────────────────────────────
 */
@Repository
public class JpaMatchRepository implements MatchRepository {

    private final SpringDataMatchRepository springDataRepo;

    public JpaMatchRepository(SpringDataMatchRepository springDataRepo) {
        this.springDataRepo = springDataRepo;
    }

    @Override
    public Match saveIfNotExists(Match match) {
        Optional<MatchEntity> existing = springDataRepo.findById(match.getId().value());

        if (existing.isPresent()) {
            return toDomain(existing.get());
        }

        MatchEntity entity = toEntity(match);
        springDataRepo.save(entity);
        return match;
    }

    @Override
    public Optional<Match> findById(MatchId id) {
        return springDataRepo.findById(id.value())
                .map(this::toDomain);
    }

    @Override
    public List<Match> findByUser(UserId userId) {
        return springDataRepo.findByUser(userId.value()).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private MatchEntity toEntity(Match match) {
        return new MatchEntity(
                match.getId().value(),
                match.getUserA().value(),
                match.getUserB().value(),
                match.getCreatedAt()
        );
    }

    private Match toDomain(MatchEntity entity) {
        return Match.reconstitute(
                new MatchId(entity.getId()),
                new UserId(entity.getUserAId()),
                new UserId(entity.getUserBId()),
                entity.getCreatedAt()
        );
    }
}
