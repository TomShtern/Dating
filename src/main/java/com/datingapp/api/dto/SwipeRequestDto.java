package com.datingapp.api.dto;

import com.datingapp.domain.SwipeDirection;
import com.datingapp.domain.UserId;

public record SwipeRequestDto(
    UserId targetUserId,
    SwipeDirection direction
) {}
