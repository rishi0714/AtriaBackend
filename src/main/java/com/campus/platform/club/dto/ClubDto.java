package com.campus.platform.club.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClubDto {

    @NotBlank(message = "Club name is required")
    private String name;

    private String managedByEmail;

    private String description;
    private String clubCategory;
    private String logoUrl;
}
