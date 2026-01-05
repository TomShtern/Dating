package com.datingapp.api.dto;

import com.datingapp.domain.UserId;

public record MatchDto(
    String matchId,
    UserId matchedWithUserId,
    String displayName
) {}
