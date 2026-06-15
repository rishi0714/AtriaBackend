package com.campus.platform.college.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CollegeSetupDto {

    @NotBlank(message = "College name is required")
    private String name;

    @NotBlank(message = "Domain is required")
    @Pattern(
            regexp = "^[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
            message = "Must be a valid domain e.g. gitam.edu.in"
    )
    private String domain;

    @NotNull(message = "At least one admin is required")
    @Size(min = 1, message = "At least one admin is required")
    private List<@Valid Admin> admins;

    private String logoUrl;

    // ── inline — no separate file needed ────────────────────────────────────
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Admin {

        @NotBlank(message = "Admin email is required")
        @Email(message = "Must be a valid email")
        private String email;

        @NotBlank(message = "Admin full name is required")
        private String fullName;
    }
}