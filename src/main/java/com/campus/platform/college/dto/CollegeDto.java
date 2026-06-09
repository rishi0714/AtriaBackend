package com.campus.platform.college.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CollegeDto {

    @NotBlank(message = "College name is required")
    private String name;

    // Optional at creation — can add domains later via /domains endpoint
    @Pattern(
            regexp = "^$|^[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
            message = "Domain must be a valid domain (e.g. sreenidhi.edu.in)"
    )
    private String domain;

    private String logoUrl;
}