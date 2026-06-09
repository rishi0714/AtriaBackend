package com.campus.platform.college.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CollegeDomainDto {

    @NotBlank(message = "Domain is required")
    @Pattern(
            regexp = "^[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
            message = "Must be a valid domain e.g. gitam.edu.in"
    )
    private String domain;

    private boolean primary = false;
}