package com.datingapp.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SwipeResponseDto(
    String swipeId,
    MatchDataDto match
) {}
