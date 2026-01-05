package com.datingapp.api.dto;

import com.datingapp.domain.UserId;

public record ProspectDto(
    UserId userId,
    String displayName,
    int age,
    double distanceKm,
    int sharedInterestCount
) {}
