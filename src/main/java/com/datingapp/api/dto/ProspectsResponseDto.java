package com.datingapp.api.dto;

import java.util.List;

public record ProspectsResponseDto(
    List<ProspectDto> prospects,
    int total
) {}
