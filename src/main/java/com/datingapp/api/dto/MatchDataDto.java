package com.datingapp.api.dto;

import com.datingapp.domain.UserId;

public record MatchDataDto(
    String matchId,
    UserId matchedWithUserId,
    String matchedWithDisplayName
) {}
