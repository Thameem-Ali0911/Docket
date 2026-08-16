package com.docket.dto.document;

import java.util.List;

import jakarta.validation.Valid;

public record AnomalyCheckResponseDto(
    @Valid
    List<AnomalyFlagDto> flags
) {
}
