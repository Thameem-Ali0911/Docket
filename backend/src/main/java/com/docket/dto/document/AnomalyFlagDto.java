package com.docket.dto.document;

import jakarta.validation.constraints.NotBlank;

public record AnomalyFlagDto(
    @NotBlank(message = "Field name cannot be blank")
    String fieldName,

    @NotBlank(message = "Description cannot be blank")
    String description,

    @NotBlank(message = "Severity cannot be blank")
    String severity
) {
}
