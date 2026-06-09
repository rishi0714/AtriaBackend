package com.campus.platform.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AssignCollegeAdminDto {

    @NotBlank(message = "College name is required")
    private String collegeName;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email")
    private String email;
}