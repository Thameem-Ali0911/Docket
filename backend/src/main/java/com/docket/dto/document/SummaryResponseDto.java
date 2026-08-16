package com.docket.dto.document;

import jakarta.validation.constraints.NotBlank;

public record SummaryResponseDto(
    @NotBlank(message = "Summary cannot be blank")
    String summary
) {
}
